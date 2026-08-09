package com.example.blogwriter.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "scheduled_job")
public class ScheduledJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private String stylePresetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostTarget target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleType scheduleType;

    // Used when scheduleType == ONCE
    private Instant runAt;

    // Used when scheduleType == RECURRING (Spring 6-field cron expression)
    private String cronExpression;

    @Column(nullable = false)
    private boolean enabled = true;

    private Instant lastRunAt;

    @Enumerated(EnumType.STRING)
    private RunStatus lastRunStatus = RunStatus.NEVER_RUN;

    @Column(length = 2000)
    private String lastRunError;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getStylePresetId() {
        return stylePresetId;
    }

    public void setStylePresetId(String stylePresetId) {
        this.stylePresetId = stylePresetId;
    }

    public PostTarget getTarget() {
        return target;
    }

    public void setTarget(PostTarget target) {
        this.target = target;
    }

    public ScheduleType getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }

    public Instant getRunAt() {
        return runAt;
    }

    public void setRunAt(Instant runAt) {
        this.runAt = runAt;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(Instant lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public RunStatus getLastRunStatus() {
        return lastRunStatus;
    }

    public void setLastRunStatus(RunStatus lastRunStatus) {
        this.lastRunStatus = lastRunStatus;
    }

    public String getLastRunError() {
        return lastRunError;
    }

    public void setLastRunError(String lastRunError) {
        this.lastRunError = lastRunError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
