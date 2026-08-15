package com.example.blogwriter.model;

public enum RunStatus {
    NEVER_RUN,
    SUCCESS,
    // VELOG/TISTORY have no click-through publish automation (Cloudflare blocks write
    // actions from a Playwright-launched browser). Content is queued for the companion
    // Chrome extension to fill in on a real, human-driven browser tab instead.
    // PENDING_FILL: queued, waiting for the extension to find and fill the write page.
    PENDING_FILL,
    // FILLED: extension filled title/tags/content; user still has to click publish
    // themselves (and either the extension detects it or the user marks it published).
    FILLED,
    FAILURE
}
