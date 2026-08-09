package com.example.blogwriter.model;

import java.time.LocalDateTime;

public class BlogPost {
    private String title;
    private String content;
    private String tags;
    private LocalDateTime createdAt;

    public BlogPost() {
        this.createdAt = LocalDateTime.now();
    }

    public BlogPost(String title, String content, String tags) {
        this.title = title;
        this.content = content;
        this.tags = tags;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
