package com.example.blogwriter.repository;

import com.example.blogwriter.model.PostHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostHistoryRepository extends JpaRepository<PostHistory, Long> {
    List<PostHistory> findTop50ByOrderByStartedAtDesc();
}
