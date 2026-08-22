package com.example.blogwriter.service;

import com.example.blogwriter.model.AutomationResult;
import com.example.blogwriter.model.FailureReason;
import com.microsoft.playwright.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Drives the fake Thymeleaf blog under /mock-blog — used for dry-running the
// generation + scheduling pipeline without touching a real account.
@Service
public class MockBlogPlaywrightService {

    @Value("${screenshot.upload.dir}")
    private String uploadDir;

    public AutomationResult runBlogPostingAutomation(String title, String tags, String content) {
        List<String> logs = new ArrayList<>();
        String screenshotFileName = "mock_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".png";

        logs.add("Playwright 자동화 포스팅 작업 시작 (대상: 모의 블로그)...");

        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try (Playwright playwright = Playwright.create()) {
            logs.add("브라우저 인스턴스 생성 중...");
            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                    .setHeadless(false)
                    .setSlowMo(800)
            );

            BrowserContext context = browser.newContext(
                new Browser.NewContextOptions().setViewportSize(1280, 800)
            );
            Page page = context.newPage();

            logs.add("모의 블로그 로그인 페이지 이동: http://localhost:9231/mock-blog/login");
            page.navigate("http://localhost:9231/mock-blog/login");

            logs.add("아이디(admin) 및 비밀번호 입력 중...");
            page.fill("#username", "admin");
            page.fill("#password", "admin");

            logs.add("로그인 버튼 클릭...");
            page.click("#login-btn");

            logs.add("로그인 성공! 글쓰기 페이지 로딩 대기...");
            page.waitForURL("**/mock-blog/write");

            logs.add("제목 작성 중: " + title);
            page.fill("#title", title);

            logs.add("태그 입력 중: " + tags);
            page.fill("#tags", tags);

            logs.add("본문 내용 작성 중...");
            page.fill("#content", content);

            logs.add("글 등록 버튼 클릭...");
            page.click("#submit-btn");

            logs.add("등록 완료! 포스트 목록 페이지 이동 중...");
            page.waitForURL("**/mock-blog/posts");

            logs.add("최종 화면 스크린샷 캡쳐 중...");
            String screenshotPath = Paths.get(uploadDir, screenshotFileName).toString();
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(screenshotPath)));

            logs.add("브라우저 세션 정리 및 작업 완료.");
            context.close();
            browser.close();

            String screenshotUrl = "/uploads/" + screenshotFileName;
            return new AutomationResult(true, screenshotUrl, null, FailureReason.NONE, logs);

        } catch (Exception e) {
            logs.add("에러 발생: " + e.getMessage());
            return new AutomationResult(false, null, null, FailureReason.AUTOMATION_ERROR, logs);
        }
    }
}
