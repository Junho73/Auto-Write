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
import java.util.Map;

@Service
public class OpenAiService {

    @Value("${openai.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final StylePresetService stylePresetService;

    public OpenAiService(StylePresetService stylePresetService) {
        this.stylePresetService = stylePresetService;
    }

    public Map<String, String> generateBlogPost(String topic, String stylePresetId) throws Exception {
        StylePreset preset = stylePresetService.getOrDefault(stylePresetId);
        String systemPrompt = "You are a professional blog writer. Generate a blog post in Korean. " +
                "Return your output strictly as a JSON object with exactly three keys: " +
                "'title', 'tags' (comma-separated string, e.g. 'React, IT, Web'), and 'content'. " +
                "Do not wrap the JSON in markdown code blocks like ```json ... ```. Just return raw JSON. " +
                "다음 스타일 가이드를 참고하여 글의 구조와 톤을 맞추세요 (" + preset.label() + "): " + preset.description();

        // Construct request body JSON using Map and ObjectMapper
        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("model", "gpt-4o");
        
        Map<String, String> responseFormat = new HashMap<>();
        responseFormat.put("type", "json_object");
        requestBodyMap.put("response_format", responseFormat);

        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", "Write a blog post about: " + topic);

        requestBodyMap.put("messages", new Object[]{systemMessage, userMessage});

        String requestBody = objectMapper.writeValueAsString(requestBodyMap);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("OpenAI API call failed with status: " + response.statusCode() + " - " + response.body());
        }

        // Parse OpenAI response
        JsonNode root = objectMapper.readTree(response.body());
        String contentJsonString = root.path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();

        // Parse the generated blog post JSON (title, tags, content)
        JsonNode postJson = objectMapper.readTree(contentJsonString);
        Map<String, String> result = new HashMap<>();
        result.put("title", postJson.path("title").asText());
        result.put("tags", postJson.path("tags").asText());
        result.put("content", postJson.path("content").asText());

        return result;
    }
}
