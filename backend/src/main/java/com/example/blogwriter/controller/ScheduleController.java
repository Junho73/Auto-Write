package com.example.blogwriter.controller;

import com.example.blogwriter.dto.ScheduleRequest;
import com.example.blogwriter.model.ScheduleType;
import com.example.blogwriter.model.ScheduledJob;
import com.example.blogwriter.repository.ScheduledJobRepository;
import com.example.blogwriter.service.ScheduledJobRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduledJobRepository scheduledJobRepository;
    private final ScheduledJobRunner scheduledJobRunner;

    public ScheduleController(ScheduledJobRepository scheduledJobRepository, ScheduledJobRunner scheduledJobRunner) {
        this.scheduledJobRepository = scheduledJobRepository;
        this.scheduledJobRunner = scheduledJobRunner;
    }

    @GetMapping
    public List<ScheduledJob> list() {
        return scheduledJobRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ScheduleRequest request) {
        String validationError = validate(request);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(Map.of("error", validationError));
        }

        ScheduledJob job = new ScheduledJob();
        job.setTopic(request.getTopic());
        job.setStylePresetId(request.getStylePresetId());
        job.setTarget(request.getTarget());
        job.setScheduleType(request.getScheduleType());
        job.setRunAt(request.getRunAt());
        job.setCronExpression(request.getCronExpression());
        job.setEnabled(true);

        job = scheduledJobRepository.save(job);
        scheduledJobRunner.register(job);
        return ResponseEntity.ok(job);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> setEnabled(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Optional<ScheduledJob> maybeJob = scheduledJobRepository.findById(id);
        if (maybeJob.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ScheduledJob job = maybeJob.get();
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "enabled 값이 필요합니다."));
        }

        job.setEnabled(enabled);
        job = scheduledJobRepository.save(job);

        if (enabled) {
            scheduledJobRunner.register(job);
        } else {
            scheduledJobRunner.cancel(job.getId());
        }
        return ResponseEntity.ok(job);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        scheduledJobRunner.cancel(id);
        scheduledJobRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private String validate(ScheduleRequest request) {
        if (request.getTopic() == null || request.getTopic().isBlank()) {
            return "주제를 입력해주세요.";
        }
        if (request.getTarget() == null) {
            return "포스팅 대상(target)을 선택해주세요.";
        }
        if (request.getScheduleType() == ScheduleType.ONCE) {
            if (request.getRunAt() == null || !request.getRunAt().isAfter(Instant.now())) {
                return "1회 예약은 현재보다 미래의 날짜/시간이어야 합니다.";
            }
        } else if (request.getScheduleType() == ScheduleType.RECURRING) {
            if (request.getCronExpression() == null || !CronExpression.isValidExpression(request.getCronExpression())) {
                return "반복 예약의 cron 표현식이 유효하지 않습니다.";
            }
        } else {
            return "스케줄 타입(scheduleType)을 선택해주세요.";
        }
        return null;
    }
}
