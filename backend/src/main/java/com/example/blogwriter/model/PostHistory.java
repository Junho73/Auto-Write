package com.example.blogwriter.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "post_history")
public class PostHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Null when triggered manually rather than by a ScheduledJob
    private Long scheduledJobId;

    private String topic;

    private String stylePresetId;

    private String title;

    private String tags;

    @Lob
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostTarget target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RunStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FailureReason failureReason = FailureReason.NONE;

    private String screenshotUrl;

    private String publishedUrl;

    @Lob
    private String logs;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant finishedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getScheduledJobId() {
        return scheduledJobId;
    }

    public void setScheduledJobId(Long scheduledJobId) {
        this.scheduledJobId = scheduledJobId;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public PostTarget getTarget() {
        return target;
    }

    public void setTarget(PostTarget target) {
        this.target = target;
    }

    public RunStatus getStatus() {
        return status;
    }

    public void setStatus(RunStatus status) {
        this.status = status;
    }

    public FailureReason getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(FailureReason failureReason) {
        this.failureReason = failureReason;
    }

    public String getScreenshotUrl() {
        return screenshotUrl;
    }

    public void setScreenshotUrl(String screenshotUrl) {
        this.screenshotUrl = screenshotUrl;
    }

    public String getPublishedUrl() {
        return publishedUrl;
    }

    public void setPublishedUrl(String publishedUrl) {
        this.publishedUrl = publishedUrl;
    }

    public String getLogs() {
        return logs;
    }

    public void setLogs(String logs) {
        this.logs = logs;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
