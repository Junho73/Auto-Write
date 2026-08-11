package com.example.blogwriter.model;

public enum RunStatus {
    NEVER_RUN,
    SUCCESS,
    // Velog has no publish automation (Cloudflare blocks write actions from any
    // Playwright-launched browser). DRAFT_SAVED means content was generated and is
    // ready for the user to copy into velog.io/write themselves — not that it's live.
    DRAFT_SAVED,
    FAILURE
}
