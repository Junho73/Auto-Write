package com.example.blogwriter.controller;

import com.example.blogwriter.model.TopicSuggestion;
import com.example.blogwriter.service.ClaudeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final ClaudeService claudeService;

    public TopicController(ClaudeService claudeService) {
        this.claudeService = claudeService;
    }

    @GetMapping("/weekly")
    public ResponseEntity<?> weekly() {
        try {
            List<TopicSuggestion> topics = claudeService.fetchWeeklyTopics();
            return ResponseEntity.ok(topics);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "이번 주 토픽을 불러오지 못했습니다: " + e.getMessage()));
        }
    }
}
