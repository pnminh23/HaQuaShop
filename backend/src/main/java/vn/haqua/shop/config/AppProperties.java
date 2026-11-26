package vn.haqua.shop.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "haqua")
public record AppProperties(Jwt jwt, Cors cors, Shop shop, Payment payment, Llm llm, Seed seed) {

    public record Jwt(String secret, Duration accessTtl, Duration refreshTtl) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Shop(long freeShipThreshold, long standardShipFee, long expressShipFee,
                       int lowStockThreshold, int qrExpireMinutes, long greenPointRate) {
    }

    public record Payment(String webhookSecret) {
    }

    /** LLM theo chuẩn OpenAI-compatible (Groq, OpenRouter, Ollama…). */
    public record Llm(String baseUrl, String apiKey, String model, Duration timeout, int maxTokens,
                      double temperature, String reasoningEffort) {
    }

    public record Seed(boolean enabled) {
    }
}
