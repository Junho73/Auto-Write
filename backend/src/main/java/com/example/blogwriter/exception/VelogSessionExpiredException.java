package com.example.blogwriter.exception;

// Thrown when a saved storageState no longer authenticates against velog.io
// (write page redirected somewhere unauthenticated instead of showing the editor).
public class VelogSessionExpiredException extends RuntimeException {
    public VelogSessionExpiredException(String message) {
        super(message);
    }
}
