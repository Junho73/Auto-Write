package com.example.blogwriter.service;

import com.example.blogwriter.model.*;
import com.example.blogwriter.repository.PostHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

// Shared by the manual "지금 포스팅" button and the scheduler, so both immediate
// and scheduled posts land in PostHistory the same way.
@Service
public class PostPublishingService {

    private final MockBlogPlaywrightService mockBlogPlaywrightService;
    private final ClaudeService claudeService;
    private final PostHistoryRepository postHistoryRepository;

    public PostPublishingService(MockBlogPlaywrightService mockBlogPlaywrightService,
                                  ClaudeService claudeService,
                                  PostHistoryRepository postHistoryRepository) {
        this.mockBlogPlaywrightService = mockBlogPlaywrightService;
        this.claudeService = claudeService;
        this.postHistoryRepository = postHistoryRepository;
    }

    public AutomationResult publishPrepared(String topic, String stylePresetId, String aiModel, String title,
                                             String tags, String content, PostTarget target, Long scheduledJobId) {
        Instant startedAt = Instant.now();

        // VELOG/TISTORY have no browser automation at all — Cloudflare blocks write actions
        // from any Playwright-launched browser (verified live; see project history). Content
        // is queued as PENDING_FILL for the companion Chrome extension to pick up and fill in
        // on the user's own, human-driven browser tab; the final publish click stays manual.
        AutomationResult result = target == PostTarget.MOCK
            ? mockBlogPlaywrightService.runBlogPostingAutomation(title, tags, content)
            : new AutomationResult(true, null, null, FailureReason.NONE,
                List.of("확장 프로그램 대기열에 추가되었습니다. " + target + " 글쓰기 페이지를 열면 자동으로 채워집니다."));

        PostHistory history = new PostHistory();
        history.setScheduledJobId(scheduledJobId);
        history.setTopic(topic);
        history.setStylePresetId(stylePresetId);
        history.setAiModel(aiModel);
        history.setTitle(title);
        history.setTags(tags);
        history.setContent(content);
        history.setTarget(target);
        history.setStatus(resolveStatus(result, target));
        history.setFailureReason(result.getFailureReason());
        history.setScreenshotUrl(result.getScreenshotUrl());
        history.setPublishedUrl(result.getPublishedUrl());
        history.setLogs(String.join("\n", result.getLogs()));
        history.setStartedAt(startedAt);
        history.setFinishedAt(Instant.now());
        postHistoryRepository.save(history);

        return result;
    }

    // Shared with ScheduledJobRunner so both call sites derive status the same way.
    // A VELOG/TISTORY "success" here just means the content was generated and queued —
    // not that it was published.
    public RunStatus resolveStatus(AutomationResult result, PostTarget target) {
        if (!result.isSuccess()) {
            return RunStatus.FAILURE;
        }
        return target == PostTarget.MOCK ? RunStatus.SUCCESS : RunStatus.PENDING_FILL;
    }

    // Used by the scheduler: generates fresh content from the topic/style, then publishes it.
    public AutomationResult generateAndPublish(String topic, String stylePresetId, String aiModel, PostTarget target,
                                                Long scheduledJobId) throws Exception {
        Map<String, String> generated = claudeService.generateBlogPost(topic, stylePresetId, aiModel);
        return publishPrepared(topic, stylePresetId, aiModel, generated.get("title"), generated.get("tags"),
            generated.get("content"), target, scheduledJobId);
    }
}
