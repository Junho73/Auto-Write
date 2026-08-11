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

        // VELOG has no browser automation at all — Cloudflare blocks write actions from any
        // Playwright-launched browser (verified live; see project history). The content is
        // just generated and handed back for the user to copy into velog.io/write themselves.
        AutomationResult result = target == PostTarget.VELOG
            ? new AutomationResult(true, null, null, FailureReason.NONE,
                List.of("콘텐츠 생성 완료. 아래에서 복사해서 Velog에 직접 붙여넣어주세요."))
            : mockBlogPlaywrightService.runBlogPostingAutomation(title, tags, content);

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

    // A VELOG success means content was generated and is ready to copy — not that it was published.
    private RunStatus resolveStatus(AutomationResult result, PostTarget target) {
        if (!result.isSuccess()) {
            return RunStatus.FAILURE;
        }
        return target == PostTarget.VELOG ? RunStatus.DRAFT_SAVED : RunStatus.SUCCESS;
    }

    // Used by the scheduler: generates fresh content from the topic/style, then publishes it.
    public AutomationResult generateAndPublish(String topic, String stylePresetId, String aiModel, PostTarget target,
                                                Long scheduledJobId) throws Exception {
        Map<String, String> generated = claudeService.generateBlogPost(topic, stylePresetId, aiModel);
        return publishPrepared(topic, stylePresetId, aiModel, generated.get("title"), generated.get("tags"),
            generated.get("content"), target, scheduledJobId);
    }
}
