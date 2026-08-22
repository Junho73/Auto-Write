package com.example.blogwriter.service;

import com.example.blogwriter.model.StylePreset;
import com.example.blogwriter.model.TopicSuggestion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Talks to the Claude Messages API for two things: generating blog post content
// (structured outputs, guaranteed valid JSON) and curating a weekly topic digest
// (Claude's web_search tool, since topic curation needs real current information).
@Service
public class ClaudeService {

    // Sonnet burned through tokens far faster than expected for what this app needs
    // (900K+ input tokens in a single day — see project history) — Haiku only, no
    // matter what a caller (including old ScheduledJob rows saved before this change)
    // asks for.
    public static final String MODEL_HAIKU = "claude-haiku-4-5";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final StylePresetService stylePresetService;
    private final ApiKeySettingsService apiKeySettingsService;

    public ClaudeService(StylePresetService stylePresetService, ApiKeySettingsService apiKeySettingsService) {
        this.stylePresetService = stylePresetService;
        this.apiKeySettingsService = apiKeySettingsService;
    }

    public static String resolveModel(String requested) {
        return MODEL_HAIKU;
    }

    public Map<String, String> generateBlogPost(String topic, String stylePresetId, String model) throws Exception {
        String resolvedModel = resolveModel(model);
        StylePreset preset = stylePresetService.getOrDefault(stylePresetId);

        String systemPrompt = "You are a professional blog writer. Generate a blog post in Korean. " +
                "다음 스타일 가이드를 참고하여 글의 구조와 톤을 맞추세요 (" + preset.label() + "): " + preset.description() +
                " 사용자가 주는 주제는 완성된 문장이 아니라 '언제/어디서/무엇을/어떻게 했는지'를 간단히 적은 " +
                "메모일 수 있습니다. 그런 경우 메모에 없는 사실을 지어내지 말고, 메모에 담긴 내용을 바탕으로 " +
                "살을 붙여 완성된 글로 자연스럽게 확장하세요. 여러 항목을 비교하거나 구조화된 정보를 전달할 " +
                "때는 마크다운 표를 적극 활용하고, 절차나 흐름을 설명할 때는 번호 목록이나 mermaid 코드 " +
                "블록(```mermaid)으로 시각적으로 표현하세요 (단, mermaid는 블로그 스킨에 따라 그림 대신 " +
                "코드로만 보일 수 있으니 과하게 의존하지 마세요). 관련 코드가 있다면 코드 블록으로 보여주세요.";

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
                "블로그 글감으로 좋은 주제 정확히 5개를 선정해서, 다른 설명 없이 아래 형식의 JSON 배열만 " +
                "출력하세요: [{\"title\": \"주제 제목\", \"summary\": \"1~2문장 요약\", \"sourceUrl\": \"참고 링크\"}]";

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("model", MODEL_HAIKU);
        requestBodyMap.put("max_tokens", 8192);
        requestBodyMap.put("system", systemPrompt);
        // Haiku (unlike Sonnet) rejects web_search without an explicit allowed_callers —
        // it doesn't support programmatic tool calling by default.
        requestBodyMap.put("tools", List.of(Map.of(
                "type", "web_search_20260209",
                "name", "web_search",
                "allowed_callers", List.of("direct"))));
        requestBodyMap.put("messages", List.of(Map.of(
                "role", "user",
                "content", "이번 주 한국 개발자들이 관심 가질만한 기술/개발 주요 토픽 5개를 찾아줘.")));

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
        String apiKey = apiKeySettingsService.getEffectiveKey();
        if (apiKey == null) {
            throw new IllegalStateException("Claude API 키가 설정되지 않았습니다. 설정 화면에서 등록해주세요.");
        }

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
