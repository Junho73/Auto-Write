package com.example.blogwriter.controller;

import com.example.blogwriter.model.FailureReason;
import com.example.blogwriter.model.PostHistory;
import com.example.blogwriter.model.PostTarget;
import com.example.blogwriter.model.RunStatus;
import com.example.blogwriter.repository.PostHistoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

// Talked to by the companion Chrome extension's content scripts, not the React app.
// A content script running on velog.io/write or *.tistory.com/manage polls /pending,
// fills the page in the user's own real browser tab, then reports back what happened.
@RestController
@RequestMapping("/api/extension")
public class ExtensionController {

    private final PostHistoryRepository postHistoryRepository;

    public ExtensionController(PostHistoryRepository postHistoryRepository) {
        this.postHistoryRepository = postHistoryRepository;
    }

    @GetMapping("/pending")
    public ResponseEntity<?> pending(@RequestParam String target) {
        PostTarget postTarget;
        try {
            postTarget = PostTarget.valueOf(target.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "알 수 없는 target: " + target));
        }

        Optional<PostHistory> next = postHistoryRepository
            .findFirstByTargetAndStatusOrderByStartedAtAsc(postTarget, RunStatus.PENDING_FILL);
        if (next.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        PostHistory history = next.get();
        return ResponseEntity.ok(Map.of(
            "id", history.getId(),
            "title", history.getTitle(),
            "tags", history.getTags() == null ? "" : history.getTags(),
            "content", history.getContent()
        ));
    }

    @PostMapping("/{id}/filled")
    public ResponseEntity<?> filled(@PathVariable Long id) {
        return updateStatus(id, RunStatus.FILLED, null, null);
    }

    @PostMapping("/{id}/published")
    public ResponseEntity<?> published(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String publishedUrl = body == null ? null : body.get("publishedUrl");
        return updateStatus(id, RunStatus.SUCCESS, publishedUrl, null);
    }

    @PostMapping("/{id}/failed")
    public ResponseEntity<?> failed(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return updateStatus(id, RunStatus.FAILURE, null, reason);
    }

    private ResponseEntity<?> updateStatus(Long id, RunStatus status, String publishedUrl, String failureLog) {
        Optional<PostHistory> maybeHistory = postHistoryRepository.findById(id);
        if (maybeHistory.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PostHistory history = maybeHistory.get();
        history.setStatus(status);
        if (publishedUrl != null && !publishedUrl.isBlank()) {
            history.setPublishedUrl(publishedUrl);
        }
        if (status == RunStatus.FAILURE) {
            history.setFailureReason(FailureReason.AUTOMATION_ERROR);
        }
        if (failureLog != null && !failureLog.isBlank()) {
            String existingLogs = history.getLogs() == null ? "" : history.getLogs() + "\n";
            history.setLogs(existingLogs + "[확장 프로그램] " + failureLog);
        }
        history.setFinishedAt(Instant.now());
        postHistoryRepository.save(history);

        return ResponseEntity.ok(Map.of("id", history.getId(), "status", history.getStatus().name()));
    }
}
