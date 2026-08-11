package com.example.blogwriter.service;

import com.example.blogwriter.model.StylePreset;
import com.example.blogwriter.model.TopicSuggestion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Talks to the Claude Messages API for two things: generating blog post content
// (structured outputs, guaranteed valid JSON) and curating a weekly topic digest
// (Claude's web_search tool, since topic curation needs real current information).
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

        JsonNode root = callMessages(requestBodyMap);
        String textJson = firstTextBlock(root);
        JsonNode postJson = objectMapper.readTree(textJson);

        Map<String, String> result = new HashMap<>();
        result.put("title", postJson.path("title").asText());
        result.put("tags", postJson.path("tags").asText());
        result.put("content", postJson.path("content").asText());
        return result;
    }

    // Uses Claude's web_search tool so the digest reflects real, current articles
    // rather than the model's training-data knowledge. No structured-output schema
    // here (search + JSON-schema output isn't a combination we've verified), so the
    // system prompt just asks for a bare JSON array and we parse defensively.
    public List<TopicSuggestion> fetchWeeklyTopics() throws Exception {
        String systemPrompt = "당신은 개발자를 위한 기술 뉴스 큐레이터입니다. web_search 도구로 최근 7일 이내 " +
                "한국 및 해외의 주요 기술/개발 관련 뉴스, 트렌드, 릴리즈 소식을 조사하세요. 조사가 끝나면 그 중 " +
                "블로그 글감으로 좋은 주제 정확히 10개를 선정해서, 다른 설명 없이 아래 형식의 JSON 배열만 " +
                "출력하세요: [{\"title\": \"주제 제목\", \"summary\": \"1~2문장 요약\", \"sourceUrl\": \"참고 링크\"}]";

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("model", MODEL_SONNET);
        requestBodyMap.put("max_tokens", 8192);
        requestBodyMap.put("system", systemPrompt);
        requestBodyMap.put("tools", List.of(Map.of("type", "web_search_20260209", "name", "web_search")));
        requestBodyMap.put("messages", List.of(Map.of(
                "role", "user",
                "content", "이번 주 한국 개발자들이 관심 가질만한 기술/개발 주요 토픽 10개를 찾아줘.")));

        JsonNode root = callMessages(requestBodyMap);
        String text = lastTextBlock(root);
        String jsonArray = text.substring(text.indexOf('['), text.lastIndexOf(']') + 1);

        JsonNode topicsJson = objectMapper.readTree(jsonArray);
        List<TopicSuggestion> topics = new ArrayList<>();
        for (JsonNode node : topicsJson) {
            topics.add(new TopicSuggestion(
                    node.path("title").asText(),
                    node.path("summary").asText(),
                    node.path("sourceUrl").asText(null)
            ));
        }
        return topics;
    }

    private JsonNode callMessages(Map<String, Object> requestBodyMap) throws Exception {
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

        return objectMapper.readTree(response.body());
    }

    private String firstTextBlock(JsonNode root) {
        for (JsonNode block : root.path("content")) {
            if ("text".equals(block.path("type").asText())) {
                return block.path("text").asText();
            }
        }
        throw new RuntimeException("Claude API response did not contain a text block: " + root);
    }

    // Web search interleaves server_tool_use / web_search_tool_result blocks with text —
    // the final answer is the last text block, not necessarily the first.
    private String lastTextBlock(JsonNode root) {
        String last = null;
        for (JsonNode block : root.path("content")) {
            if ("text".equals(block.path("type").asText())) {
                last = block.path("text").asText();
            }
        }
        if (last == null) {
            throw new RuntimeException("Claude API response did not contain a text block: " + root);
        }
        return last;
    }
}
