package com.example.blogwriter.controller;

import com.example.blogwriter.model.PostHistory;
import com.example.blogwriter.model.RunStatus;
import com.example.blogwriter.repository.PostHistoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/history")
public class PostHistoryController {

    private final PostHistoryRepository postHistoryRepository;

    public PostHistoryController(PostHistoryRepository postHistoryRepository) {
        this.postHistoryRepository = postHistoryRepository;
    }

    @GetMapping
    public List<PostHistory> list() {
        return postHistoryRepository.findTop50ByOrderByStartedAtDesc();
    }

    // Manual fallback for VELOG/TISTORY: the extension's best-effort auto-detection of a
    // successful publish (watching for navigation away from the write page) may not fire on
    // every platform, so the user can confirm it themselves from the history list instead.
    @PostMapping("/{id}/mark-published")
    public ResponseEntity<?> markPublished(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        Optional<PostHistory> maybeHistory = postHistoryRepository.findById(id);
        if (maybeHistory.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PostHistory history = maybeHistory.get();
        history.setStatus(RunStatus.SUCCESS);
        String publishedUrl = body == null ? null : body.get("publishedUrl");
        if (publishedUrl != null && !publishedUrl.isBlank()) {
            history.setPublishedUrl(publishedUrl);
        }
        history.setFinishedAt(Instant.now());
        postHistoryRepository.save(history);

        return ResponseEntity.ok(history);
    }
}
