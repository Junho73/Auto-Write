package com.example.blogwriter.model;

import java.util.List;

public class AutomationResult {
    private final boolean success;
    private final String screenshotUrl;
    private final String publishedUrl;
    private final FailureReason failureReason;
    private final List<String> logs;

    public AutomationResult(boolean success, String screenshotUrl, String publishedUrl,
                             FailureReason failureReason, List<String> logs) {
        this.success = success;
        this.screenshotUrl = screenshotUrl;
        this.publishedUrl = publishedUrl;
        this.failureReason = failureReason;
        this.logs = logs;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getScreenshotUrl() {
        return screenshotUrl;
    }

    public String getPublishedUrl() {
        return publishedUrl;
    }

    public FailureReason getFailureReason() {
        return failureReason;
    }

    public List<String> getLogs() {
        return logs;
    }
}
