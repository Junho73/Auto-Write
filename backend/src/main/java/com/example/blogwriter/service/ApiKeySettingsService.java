package com.example.blogwriter.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// The ANTHROPIC_API_KEY env var doesn't survive a shell restart, which made local
// development repeatedly annoying (see project history). This adds a fallback: a
// key saved once from the Settings screen persists to a local, gitignored file
// under backend/data/ and is used whenever the env var isn't set. The env var, if
// present, always takes priority — this is a convenience fallback, not a replacement.
@Service
public class ApiKeySettingsService {

    private static final Path KEY_FILE = Paths.get("data", "anthropic-api-key.txt");

    @Value("${anthropic.api.key:}")
    private String envApiKey;

    public String getEffectiveKey() {
        if (envApiKey != null && !envApiKey.isBlank()) {
            return envApiKey.trim();
        }
        if (Files.exists(KEY_FILE)) {
            try {
                String saved = Files.readString(KEY_FILE, StandardCharsets.UTF_8).trim();
                return saved.isBlank() ? null : saved;
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    public void saveKey(String newKey) throws IOException {
        File dir = KEY_FILE.toAbsolutePath().getParent().toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        Files.writeString(KEY_FILE, newKey.trim(), StandardCharsets.UTF_8);
    }

    public Status getStatus() {
        if (envApiKey != null && !envApiKey.isBlank()) {
            return new Status(true, "env", mask(envApiKey.trim()));
        }
        String saved = Files.exists(KEY_FILE) ? readQuietly() : null;
        if (saved != null && !saved.isBlank()) {
            return new Status(true, "saved", mask(saved));
        }
        return new Status(false, "none", null);
    }

    private String readQuietly() {
        try {
            return Files.readString(KEY_FILE, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return null;
        }
    }

    private String mask(String key) {
        if (key.length() <= 8) {
            return "****";
        }
        return key.substring(0, 7) + "..." + key.substring(key.length() - 4);
    }

    public record Status(boolean configured, String source, String masked) {
    }
}
