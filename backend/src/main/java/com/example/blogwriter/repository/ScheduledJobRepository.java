package com.example.blogwriter.repository;

import com.example.blogwriter.model.ScheduledJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, Long> {
    List<ScheduledJob> findByEnabledTrue();
}
