package com.example.blogwriter.controller;

import com.example.blogwriter.model.PostHistory;
import com.example.blogwriter.repository.PostHistoryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
