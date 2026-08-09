package com.example.blogwriter.service;

import com.example.blogwriter.exception.VelogSessionExpiredException;
import com.example.blogwriter.model.AutomationResult;
import com.example.blogwriter.model.FailureReason;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Velog (velog.io) requires email-link/code login — there is no username/password
 * form to automate. So instead of logging in on every run, the user logs in once
 * manually inside a headed browser this service opens, and we persist that
 * authenticated session (cookies + localStorage) via Playwright's storageState.
 * Every later post reuses the saved session; if it has expired, we fail clearly
 * (SESSION_EXPIRED) instead of hanging on a login page that never appears.
 *
 * NOTE: the actual Velog write-page DOM has not been seen by anyone but the user
 * (it requires their personal login). Selectors below are written defensively
 * (role/placeholder/text based) but are best-effort guesses — see "TODO calibrate"
 * comments. The user should verify/adjust these against the real page, e.g. via
 * `playwright codegen velog.io` after logging in.
 */
@Service
public class VelogAutomationService {

    private static final String VELOG_WRITE_URL = "https://velog.io/write";

    @Value("${velog.session.storage-path}")
    private String storagePath;

    @Value("${screenshot.upload.dir}")
    private String uploadDir;

    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final ScheduledExecutorService connectTimeoutExecutor = Executors.newSingleThreadScheduledExecutor();

    // Only populated while a manual "connect" flow is in progress (between startConnect and confirm/cancel).
    private Playwright connectPlaywright;
    private Browser connectBrowser;
    private BrowserContext connectContext;
    private ScheduledFuture<?> connectTimeoutFuture;

    public synchronized VelogSessionStatus getStatus() {
        File file = new File(storagePath);
        boolean connected = file.exists();
        Instant connectedAt = connected ? Instant.ofEpochMilli(file.lastModified()) : null;
        return new VelogSessionStatus(connected, connectedAt, connectContext != null, busy.get());
    }

    public synchronized void startConnect() {
        if (!busy.compareAndSet(false, true)) {
            throw new IllegalStateException("다른 Velog 브라우저 작업이 진행 중입니다. 잠시 후 다시 시도해주세요.");
        }
        try {
            connectPlaywright = Playwright.create();
            connectBrowser = connectPlaywright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(false)
            );
            connectContext = connectBrowser.newContext(
                new Browser.NewContextOptions().setViewportSize(1280, 800)
            );
            Page page = connectContext.newPage();
            page.navigate("https://velog.io");

            // Auto-cancel if the user abandons the login window (avoid a leaked Chromium process).
            connectTimeoutFuture = connectTimeoutExecutor.schedule(this::cancelConnect, 10, TimeUnit.MINUTES);
        } catch (RuntimeException e) {
            cleanupConnectState();
            busy.set(false);
            throw e;
        }
    }

    public synchronized void confirmConnect() {
        if (connectContext == null) {
            throw new IllegalStateException("진행 중인 로그인 세션이 없습니다. 먼저 연결을 시작해주세요.");
        }
        try {
            File dir = new File(storagePath).getParentFile();
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }
            connectContext.storageState(new BrowserContext.StorageStateOptions().setPath(Paths.get(storagePath)));
        } finally {
            closeConnectResources();
            busy.set(false);
        }
    }

    public synchronized void cancelConnect() {
        closeConnectResources();
        busy.set(false);
    }

    private void closeConnectResources() {
        if (connectTimeoutFuture != null) {
            connectTimeoutFuture.cancel(false);
            connectTimeoutFuture = null;
        }
        try {
            if (connectContext != null) connectContext.close();
        } catch (RuntimeException ignored) {
        }
        try {
            if (connectBrowser != null) connectBrowser.close();
        } catch (RuntimeException ignored) {
        }
        try {
            if (connectPlaywright != null) connectPlaywright.close();
        } catch (RuntimeException ignored) {
        }
        cleanupConnectState();
    }

    private void cleanupConnectState() {
        connectContext = null;
        connectBrowser = null;
        connectPlaywright = null;
    }

    public AutomationResult postToVelog(String title, String tags, String content) {
        if (!busy.compareAndSet(false, true)) {
            return new AutomationResult(false, null, null, FailureReason.AUTOMATION_ERROR,
                List.of("다른 Velog 브라우저 작업(세션 연결 등)이 진행 중이라 포스팅을 시작할 수 없습니다."));
        }
        try {
            return doPostToVelog(title, tags, content);
        } finally {
            busy.set(false);
        }
    }

    private AutomationResult doPostToVelog(String title, String tags, String content) {
        List<String> logs = new ArrayList<>();

        if (!Files.exists(Paths.get(storagePath))) {
            logs.add("저장된 Velog 세션이 없습니다. 먼저 'Velog 연결하기'로 1회 로그인해주세요.");
            return new AutomationResult(false, null, null, FailureReason.SESSION_MISSING, logs);
        }

        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String screenshotFileName = "velog_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".png";
        Path screenshotPath = Paths.get(uploadDir, screenshotFileName);

        logs.add("Playwright 자동화 포스팅 작업 시작 (대상: Velog)...");

        try (Playwright playwright = Playwright.create()) {
            logs.add("저장된 세션으로 브라우저 컨텍스트 생성 중...");
            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(false).setSlowMo(500)
            );
            BrowserContext context = browser.newContext(
                new Browser.NewContextOptions()
                    .setViewportSize(1280, 800)
                    .setStorageStatePath(Paths.get(storagePath))
            );
            Page page = context.newPage();

            try {
                logs.add("Velog 글쓰기 페이지 이동: " + VELOG_WRITE_URL);
                page.navigate(VELOG_WRITE_URL);

                assertSessionStillValid(page, logs);

                // TODO calibrate: confirm the real placeholder text on velog.io/write once logged in.
                logs.add("제목 작성 중: " + title);
                Locator titleField = page.getByPlaceholder("제목을 입력하세요").first();
                titleField.click();
                titleField.fill(title);

                // TODO calibrate: Velog's body editor is a markdown editor, not a plain textarea.
                logs.add("본문 내용 작성 중...");
                Locator contentField = page.locator("[contenteditable='true'], .CodeMirror, textarea").last();
                contentField.click();
                contentField.fill(content);

                // TODO calibrate: the publish button opens a modal with tag/series/description fields.
                logs.add("발행(출간) 버튼 클릭...");
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("출간하기")).first().click();

                if (tags != null && !tags.isBlank()) {
                    logs.add("태그 입력 중: " + tags);
                    Locator tagInput = page.getByPlaceholder("태그를 입력해주세요");
                    for (String tag : tags.split(",")) {
                        String trimmed = tag.trim();
                        if (trimmed.isEmpty()) continue;
                        tagInput.fill(trimmed);
                        tagInput.press("Enter");
                    }
                }

                logs.add("최종 출간 확인...");
                page.getByRole(AriaRole.DIALOG).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("출간하기")).click();

                page.waitForURL(url -> !url.contains("/write"), new Page.WaitForURLOptions().setTimeout(20000));
                String publishedUrl = page.url();

                logs.add("최종 화면 스크린샷 캡쳐 중...");
                page.screenshot(new Page.ScreenshotOptions().setPath(screenshotPath));

                logs.add("발행 완료: " + publishedUrl);
                context.close();
                browser.close();

                return new AutomationResult(true, "/uploads/" + screenshotFileName, publishedUrl, FailureReason.NONE, logs);

            } catch (VelogSessionExpiredException e) {
                logs.add("세션 만료: " + e.getMessage());
                safeScreenshot(page, screenshotPath, logs);
                context.close();
                browser.close();
                return new AutomationResult(false, "/uploads/" + screenshotFileName, null, FailureReason.SESSION_EXPIRED, logs);

            } catch (Exception e) {
                logs.add("에러 발생: " + e.getMessage());
                safeScreenshot(page, screenshotPath, logs);
                context.close();
                browser.close();
                return new AutomationResult(false, "/uploads/" + screenshotFileName, null, FailureReason.AUTOMATION_ERROR, logs);
            }

        } catch (Exception e) {
            logs.add("브라우저 실행 중 에러 발생: " + e.getMessage());
            return new AutomationResult(false, null, null, FailureReason.AUTOMATION_ERROR, logs);
        }
    }

    // TODO calibrate: this heuristic (looking for a visible "로그인" button) is a best guess
    // for detecting "storageState no longer authenticates" — verify against the real page.
    private void assertSessionStillValid(Page page, List<String> logs) {
        boolean loginPromptVisible = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("로그인"))
            .isVisible();
        if (loginPromptVisible) {
            logs.add("세션이 만료되어 로그인 화면으로 이동된 것으로 보입니다.");
            throw new VelogSessionExpiredException("저장된 Velog 세션이 더 이상 유효하지 않습니다. 다시 연결해주세요.");
        }
    }

    private void safeScreenshot(Page page, Path path, List<String> logs) {
        try {
            page.screenshot(new Page.ScreenshotOptions().setPath(path));
            logs.add("실패 시점 스크린샷 저장: " + path);
        } catch (RuntimeException e) {
            logs.add("스크린샷 저장 실패: " + e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        closeConnectResources();
        connectTimeoutExecutor.shutdownNow();
    }
}
