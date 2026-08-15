package com.example.blogwriter.repository;

import com.example.blogwriter.model.PostHistory;
import com.example.blogwriter.model.PostTarget;
import com.example.blogwriter.model.RunStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostHistoryRepository extends JpaRepository<PostHistory, Long> {
    List<PostHistory> findTop50ByOrderByStartedAtDesc();

    // FIFO: the extension picks up the oldest queued post first for its target platform.
    Optional<PostHistory> findFirstByTargetAndStatusOrderByStartedAtAsc(PostTarget target, RunStatus status);
}
