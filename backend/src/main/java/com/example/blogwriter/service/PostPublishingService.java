package com.example.blogwriter.service;

import com.example.blogwriter.model.*;
import com.example.blogwriter.repository.PostHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

// Shared by the manual "지금 포스팅" button and the scheduler, so both immediate
// and scheduled posts land in PostHistory the same way.
@Service
public class PostPublishingService {

    private final MockBlogPlaywrightService mockBlogPlaywrightService;
    private final VelogAutomationService velogAutomationService;
    private final OpenAiService openAiService;
    private final PostHistoryRepository postHistoryRepository;

    public PostPublishingService(MockBlogPlaywrightService mockBlogPlaywrightService,
                                  VelogAutomationService velogAutomationService,
                                  OpenAiService openAiService,
                                  PostHistoryRepository postHistoryRepository) {
        this.mockBlogPlaywrightService = mockBlogPlaywrightService;
        this.velogAutomationService = velogAutomationService;
        this.openAiService = openAiService;
        this.postHistoryRepository = postHistoryRepository;
    }

    public AutomationResult publishPrepared(String topic, String stylePresetId, String title, String tags,
                                             String content, PostTarget target, Long scheduledJobId) {
        Instant startedAt = Instant.now();

        AutomationResult result = target == PostTarget.VELOG
            ? velogAutomationService.postToVelog(title, tags, content)
            : mockBlogPlaywrightService.runBlogPostingAutomation(title, tags, content);

        PostHistory history = new PostHistory();
        history.setScheduledJobId(scheduledJobId);
        history.setTopic(topic);
        history.setStylePresetId(stylePresetId);
        history.setTitle(title);
        history.setTags(tags);
        history.setContent(content);
        history.setTarget(target);
        history.setStatus(result.isSuccess() ? RunStatus.SUCCESS : RunStatus.FAILURE);
        history.setFailureReason(result.getFailureReason());
        history.setScreenshotUrl(result.getScreenshotUrl());
        history.setPublishedUrl(result.getPublishedUrl());
        history.setLogs(String.join("\n", result.getLogs()));
        history.setStartedAt(startedAt);
        history.setFinishedAt(Instant.now());
        postHistoryRepository.save(history);

        return result;
    }

    // Used by the scheduler: generates fresh content from the topic/style, then publishes it.
    public AutomationResult generateAndPublish(String topic, String stylePresetId, PostTarget target,
                                                Long scheduledJobId) throws Exception {
        Map<String, String> generated = openAiService.generateBlogPost(topic, stylePresetId);
        return publishPrepared(topic, stylePresetId, generated.get("title"), generated.get("tags"),
            generated.get("content"), target, scheduledJobId);
    }
}
