package fun.yaddu.guard.api;

import com.google.gson.*;
import fun.yaddu.guard.config.GuardConfig;
import org.slf4j.Logger;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class AiModerationClient {

    private final GuardConfig config;
    private final Logger logger;
    private final HttpClient http;

    public AiModerationClient(GuardConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
        this.http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    }

    public CompletableFuture<ModerationResult> analyze(String player, String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String provider = config.getProvider().toLowerCase();
                ModerationResult result = switch (provider) {
                    case "groq" -> callOpenAICompat(
                        "https://api.groq.com/openai/v1/chat/completions",
                        config.getGrokApiKey(), config.getGrokModel(), player, message);
                    case "openai" -> callOpenAICompat(
                        "https://api.openai.com/v1/chat/completions",
                        config.getOpenAiApiKey(), config.getOpenAiModel(), player, message);
                    default -> callGemini(player, message);
                };
                // Auto-fallback to Gemini
                if (result.isError() && !provider.equals("gemini")
                        && !config.getGeminiApiKey().equals("YOUR_GEMINI_API_KEY")) {
                    logger.warn("[YadduGuard] Fallback to Gemini");
                    return callGemini(player, message);
                }
                return result;
            } catch (Exception e) {
                logger.error("[YadduGuard] API error: " + e.getMessage());
                return new ModerationResult("error: " + e.getMessage());
            }
        });
    }

    private ModerationResult callGemini(String player, String message) throws Exception {
        String key = config.getGeminiApiKey();
        if (key.isEmpty() || key.equals("YOUR_GEMINI_API_KEY"))
            return new ModerationResult("Gemini key not set");
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
            + config.getGeminiModel() + ":generateContent?key=" + key;

        JsonObject part = new JsonObject(); part.addProperty("text", buildPrompt(player, message));
        JsonArray parts = new JsonArray(); parts.add(part);
        JsonObject content = new JsonObject(); content.add("parts", parts);
        JsonArray contents = new JsonArray(); contents.add(content);
        JsonObject genCfg = new JsonObject();
        genCfg.addProperty("responseMimeType","application/json");
        genCfg.addProperty("temperature", 0.1);
        JsonObject body = new JsonObject();
        body.add("contents", contents); body.add("generationConfig", genCfg);

        String resp = post(url, body.toString(), null);
        if (config.isDebug()) logger.info("[Gemini] " + resp);
        try {
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            if (root.has("error"))
                return new ModerationResult("Gemini: " + root.getAsJsonObject("error").get("message").getAsString());
            String text = root.getAsJsonArray("candidates").get(0).getAsJsonObject()
                .getAsJsonObject("content").getAsJsonArray("parts")
                .get(0).getAsJsonObject().get("text").getAsString();
            return parseJson(text);
        } catch (Exception e) { return new ModerationResult("Gemini parse: " + e.getMessage()); }
    }

    private ModerationResult callOpenAICompat(String url, String key, String model, String player, String message) throws Exception {
        if (key == null || key.isEmpty() || key.startsWith("YOUR_"))
            return new ModerationResult("API key not set");
        JsonObject sys = new JsonObject(); sys.addProperty("role","system");
        sys.addProperty("content","You are a Minecraft chat moderator. Respond ONLY with valid JSON.");
        JsonObject usr = new JsonObject(); usr.addProperty("role","user");
        usr.addProperty("content", buildPrompt(player, message));
        JsonArray msgs = new JsonArray(); msgs.add(sys); msgs.add(usr);
        JsonObject fmt = new JsonObject(); fmt.addProperty("type","json_object");
        JsonObject body = new JsonObject();
        body.addProperty("model", model); body.add("messages", msgs);
        body.addProperty("temperature", 0.1); body.addProperty("max_tokens", 150);
        body.add("response_format", fmt);

        String resp = post(url, body.toString(), "Bearer " + key);
        if (config.isDebug()) logger.info("[OpenAI-compat] " + resp);
        try {
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            if (root.has("error"))
                return new ModerationResult("API: " + root.getAsJsonObject("error").get("message").getAsString());
            String text = root.getAsJsonArray("choices").get(0).getAsJsonObject()
                .getAsJsonObject("message").get("content").getAsString();
            return parseJson(text);
        } catch (Exception e) { return new ModerationResult("Parse: " + e.getMessage()); }
    }

    private String buildPrompt(String player, String message) {
        return config.getAiContext()
            + "\n\nPlayer: " + player + "\nMessage: " + message
            + "\n\nJSON only:\n{\"toxic\":true/false,\"severity\":0-10,\"reason\":\"reason\",\"category\":\"gaali/harassment/hate/spam/clean\"}";
    }

    private ModerationResult parseJson(String json) {
        try {
            json = json.replaceAll("```json","").replaceAll("```","").trim();
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            return new ModerationResult(
                o.has("toxic") && o.get("toxic").getAsBoolean(),
                o.has("severity") ? Math.min(10, Math.max(0, o.get("severity").getAsInt())) : 0,
                o.has("reason") ? o.get("reason").getAsString() : "Unknown",
                o.has("category") ? o.get("category").getAsString() : "unknown"
            );
        } catch (Exception e) { return new ModerationResult("JSON parse failed"); }
    }

    private String post(String url, String body, String auth) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder()
            .uri(URI.create(url)).timeout(Duration.ofSeconds(12))
            .header("Content-Type","application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (auth != null) req.header("Authorization", auth);
        return http.send(req.build(), HttpResponse.BodyHandlers.ofString()).body();
    }
}
