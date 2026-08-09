package com.example.blogwriter.controller;

import com.example.blogwriter.model.AutomationResult;
import com.example.blogwriter.model.PostTarget;
import com.example.blogwriter.service.OpenAiService;
import com.example.blogwriter.service.PostPublishingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/blog")
public class BlogApiController {

    private final OpenAiService openAiService;
    private final PostPublishingService postPublishingService;

    public BlogApiController(OpenAiService openAiService, PostPublishingService postPublishingService) {
        this.openAiService = openAiService;
        this.postPublishingService = postPublishingService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generatePost(@RequestBody Map<String, String> request) {
        String topic = request.get("topic");
        if (topic == null || topic.trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "주제를 입력해주세요.");
            return ResponseEntity.badRequest().body(error);
        }
        String stylePresetId = request.get("stylePresetId");

        try {
            Map<String, String> generatedPost = openAiService.generateBlogPost(topic, stylePresetId);
            return ResponseEntity.ok(generatedPost);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "블로그 생성에 실패했습니다: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/post")
    public ResponseEntity<?> autoPost(@RequestBody Map<String, String> request) {
        String title = request.get("title");
        String tags = request.get("tags");
        String content = request.get("content");
        String topic = request.get("topic");
        String stylePresetId = request.get("stylePresetId");
        PostTarget target = "VELOG".equalsIgnoreCase(request.get("target")) ? PostTarget.VELOG : PostTarget.MOCK;

        if (title == null || title.trim().isEmpty() || content == null || content.trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "제목과 본문 내용은 필수 항목입니다.");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            AutomationResult result = postPublishingService.publishPrepared(
                topic, stylePresetId, title, tags, content, target, null);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "자동화 포스팅 작업 중 오류가 발생했습니다: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
