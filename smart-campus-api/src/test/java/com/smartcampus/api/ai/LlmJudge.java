package com.smartcampus.api.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Small utility to ask a "judge" LLM whether a chatbot reply satisfies a rubric.
 * Used by AiChatJudgeTest for CI-grade LLM-as-judge evaluation.
 */
public final class LlmJudge {

    public record Verdict(boolean pass, String reasoning) {}

    private static final String JUDGE_MODEL = System.getenv().getOrDefault("JUDGE_MODEL", "gpt-4o-mini");
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    private final String apiKey;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public LlmJudge(String apiKey) {
        this.apiKey = apiKey;
    }

    public Verdict judge(String userPrompt, String botReply, List<String> toolsUsed, String rubric) {
        String judgeSystem = """
                You evaluate a campus chatbot's reply against a rubric.
                Return ONLY compact JSON: {"pass": boolean, "reasoning": "short sentence"}.
                Read the rubric carefully and apply it CHARITABLY. If the rubric says \
                "EITHER X OR Y", satisfying just one is a pass. Empty-result answers \
                (e.g. "no X available", "none found") are valid whenever the rubric allows \
                stating none. Only fail if the rubric is clearly violated (e.g. hallucinated \
                data, ignored the question, wrong topic). Treat equivalent phrasings as \
                equivalent (e.g. "zero" ≡ "none" ≡ "no items"; "2pm" ≡ "14:00").
                """;

        String judgeUser = String.format("""
                USER PROMPT:
                %s

                BOT REPLY:
                %s

                TOOLS THE BOT USED: %s

                RUBRIC:
                %s

                Return strict JSON now.""",
                userPrompt, botReply,
                toolsUsed == null || toolsUsed.isEmpty() ? "(none)" : String.join(", ", toolsUsed),
                rubric);

        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", JUDGE_MODEL);
            body.put("temperature", 0.0);
            body.set("response_format",
                    mapper.createObjectNode().put("type", "json_object"));
            ArrayNode msgs = body.putArray("messages");
            msgs.addObject().put("role", "system").put("content", judgeSystem);
            msgs.addObject().put("role", "user").put("content", judgeUser);

            HttpRequest req = HttpRequest.newBuilder(URI.create(OPENAI_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new RuntimeException("Judge API " + resp.statusCode() + ": " + resp.body());
            }
            JsonNode root = mapper.readTree(resp.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            JsonNode verdict = mapper.readTree(content);
            return new Verdict(
                    verdict.path("pass").asBoolean(false),
                    verdict.path("reasoning").asText(""));
        } catch (Exception e) {
            throw new RuntimeException("LlmJudge failed: " + e.getMessage(), e);
        }
    }
}
