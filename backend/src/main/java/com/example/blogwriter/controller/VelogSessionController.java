package com.example.blogwriter.controller;

import com.example.blogwriter.service.VelogAutomationService;
import com.example.blogwriter.service.VelogSessionStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/velog/session")
public class VelogSessionController {

    private final VelogAutomationService velogAutomationService;

    public VelogSessionController(VelogAutomationService velogAutomationService) {
        this.velogAutomationService = velogAutomationService;
    }

    @GetMapping("/status")
    public VelogSessionStatus status() {
        return velogAutomationService.getStatus();
    }

    @PostMapping("/connect")
    public ResponseEntity<?> connect() {
        try {
            velogAutomationService.startConnect();
            return ResponseEntity.ok(Map.of(
                "message", "브라우저 창이 열렸습니다. 그 창에서 Velog 로그인을 완료한 뒤, '로그인 완료' 버튼을 눌러주세요."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirm() {
        try {
            velogAutomationService.confirmConnect();
            return ResponseEntity.ok(Map.of("message", "Velog 세션이 저장되었습니다."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancel() {
        velogAutomationService.cancelConnect();
        return ResponseEntity.ok(Map.of("message", "연결이 취소되었습니다."));
    }
}
