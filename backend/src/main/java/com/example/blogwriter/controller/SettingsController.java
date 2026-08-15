package com.example.blogwriter.controller;

import com.example.blogwriter.service.ApiKeySettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final ApiKeySettingsService apiKeySettingsService;

    public SettingsController(ApiKeySettingsService apiKeySettingsService) {
        this.apiKeySettingsService = apiKeySettingsService;
    }

    @GetMapping("/api-key")
    public ApiKeySettingsService.Status status() {
        return apiKeySettingsService.getStatus();
    }

    @PostMapping("/api-key")
    public ResponseEntity<?> save(@RequestBody Map<String, String> body) {
        String apiKey = body.get("apiKey");
        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "API 키를 입력해주세요."));
        }
        try {
            apiKeySettingsService.saveKey(apiKey);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "저장 중 오류가 발생했습니다: " + e.getMessage()));
        }
        return ResponseEntity.ok(apiKeySettingsService.getStatus());
    }
}
