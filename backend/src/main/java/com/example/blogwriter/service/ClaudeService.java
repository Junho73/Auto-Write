package com.example.blogwriter.service;

import com.example.blogwriter.model.StylePreset;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Generates blog post content via the Claude Messages API, using structured
// outputs (output_config.format) so the response is guaranteed valid JSON —
// no markdown-fence stripping or retry-on-parse-failure needed.
@Service
public class ClaudeService {

    public static final String MODEL_HAIKU = "claude-haiku-4-5";
    public static final String MODEL_SONNET = "claude-sonnet-5";
    private static final Set<String> ALLOWED_MODELS = Set.of(MODEL_HAIKU, MODEL_SONNET);
    private static final String DEFAULT_MODEL = MODEL_HAIKU;

    @Value("${anthropic.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final StylePresetService stylePresetService;

    public ClaudeService(StylePresetService stylePresetService) {
        this.stylePresetService = stylePresetService;
    }

    public static String resolveModel(String requested) {
        return ALLOWED_MODELS.contains(requested) ? requested : DEFAULT_MODEL;
    }

    public Map<String, String> generateBlogPost(String topic, String stylePresetId, String model) throws Exception {
        String resolvedModel = resolveModel(model);
        StylePreset preset = stylePresetService.getOrDefault(stylePresetId);

        String systemPrompt = "You are a professional blog writer. Generate a blog post in Korean. " +
                "다음 스타일 가이드를 참고하여 글의 구조와 톤을 맞추세요 (" + preset.label() + "): " + preset.description();

        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "title", Map.of("type", "string"),
                        "tags", Map.of("type", "string", "description", "comma-separated tags, e.g. 'React, IT, Web'"),
                        "content", Map.of("type", "string")
                ),
                "required", List.of("title", "tags", "content"),
                "additionalProperties", false
        );

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("model", resolvedModel);
        requestBodyMap.put("max_tokens", 8192);
        requestBodyMap.put("system", systemPrompt);
        // Simple content generation doesn't need reasoning — keep it fast and cheap.
        requestBodyMap.put("thinking", Map.of("type", "disabled"));
        requestBodyMap.put("output_config", Map.of("format", Map.of("type", "json_schema", "schema", schema)));
        requestBodyMap.put("messages", List.of(Map.of("role", "user", "content", "Write a blog post about: " + topic)));

        String requestBody = objectMapper.writeValueAsString(requestBodyMap);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.anthropic.com/v1/messages"))
                .header("content-type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Claude API call failed with status: " + response.statusCode() + " - " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String textJson = null;
        for (JsonNode block : root.path("content")) {
            if ("text".equals(block.path("type").asText())) {
                textJson = block.path("text").asText();
                break;
            }
        }
        if (textJson == null) {
            throw new RuntimeException("Claude API response did not contain a text block: " + response.body());
        }

        JsonNode postJson = objectMapper.readTree(textJson);
        Map<String, String> result = new HashMap<>();
        result.put("title", postJson.path("title").asText());
        result.put("tags", postJson.path("tags").asText());
        result.put("content", postJson.path("content").asText());

        return result;
    }
}
