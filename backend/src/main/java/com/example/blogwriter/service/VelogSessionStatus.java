package com.example.blogwriter.service;

import java.time.Instant;

public record VelogSessionStatus(boolean connected, Instant connectedAt, boolean connectInProgress, boolean busy) {
}
