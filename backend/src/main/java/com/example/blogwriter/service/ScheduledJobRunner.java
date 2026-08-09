package com.example.blogwriter.service;

import com.example.blogwriter.model.AutomationResult;
import com.example.blogwriter.model.RunStatus;
import com.example.blogwriter.model.ScheduleType;
import com.example.blogwriter.model.ScheduledJob;
import com.example.blogwriter.repository.ScheduledJobRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

// Dynamic registration of ScheduledJob rows onto Spring's TaskScheduler:
// ONCE jobs get a single Instant trigger, RECURRING jobs get a CronTrigger.
// Jobs are (re)registered on app startup and whenever created/paused/resumed/deleted via the API.
@Component
public class ScheduledJobRunner {

    private final TaskScheduler taskScheduler;
    private final ScheduledJobRepository scheduledJobRepository;
    private final PostPublishingService postPublishingService;
    private final Map<Long, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

    public ScheduledJobRunner(TaskScheduler taskScheduler,
                               ScheduledJobRepository scheduledJobRepository,
                               PostPublishingService postPublishingService) {
        this.taskScheduler = taskScheduler;
        this.scheduledJobRepository = scheduledJobRepository;
        this.postPublishingService = postPublishingService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerAllEnabledJobs() {
        scheduledJobRepository.findByEnabledTrue().forEach(this::register);
    }

    public void register(ScheduledJob job) {
        cancel(job.getId());

        ScheduledFuture<?> future = job.getScheduleType() == ScheduleType.ONCE
            ? taskScheduler.schedule(() -> runJob(job.getId()), job.getRunAt())
            : taskScheduler.schedule(() -> runJob(job.getId()),
                new CronTrigger(job.getCronExpression(), ZoneId.systemDefault()));

        if (future != null) {
            futures.put(job.getId(), future);
        }
    }

    public void cancel(Long jobId) {
        ScheduledFuture<?> existing = futures.remove(jobId);
        if (existing != null) {
            existing.cancel(false);
        }
    }

    private void runJob(Long jobId) {
        Optional<ScheduledJob> maybeJob = scheduledJobRepository.findById(jobId);
        if (maybeJob.isEmpty() || !maybeJob.get().isEnabled()) {
            return;
        }
        ScheduledJob job = maybeJob.get();

        try {
            AutomationResult result = postPublishingService.generateAndPublish(
                job.getTopic(), job.getStylePresetId(), job.getTarget(), job.getId());
            job.setLastRunStatus(result.isSuccess() ? RunStatus.SUCCESS : RunStatus.FAILURE);
            job.setLastRunError(result.isSuccess() ? null : result.getFailureReason().name());
        } catch (Exception e) {
            job.setLastRunStatus(RunStatus.FAILURE);
            job.setLastRunError(e.getMessage());
        }

        job.setLastRunAt(Instant.now());
        if (job.getScheduleType() == ScheduleType.ONCE) {
            job.setEnabled(false);
            futures.remove(job.getId());
        }
        scheduledJobRepository.save(job);
    }
}
