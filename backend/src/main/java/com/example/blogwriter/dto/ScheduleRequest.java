package com.example.blogwriter.dto;

import com.example.blogwriter.model.PostTarget;
import com.example.blogwriter.model.ScheduleType;

import java.time.Instant;

public class ScheduleRequest {
    private String topic;
    private String stylePresetId;
    private PostTarget target;
    private ScheduleType scheduleType;
    private Instant runAt;
    private String cronExpression;

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
}
