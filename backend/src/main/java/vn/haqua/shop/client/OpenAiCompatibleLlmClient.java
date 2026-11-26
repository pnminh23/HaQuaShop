package vn.haqua.shop.client;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import vn.haqua.shop.config.AppProperties;

/**
 * Gọi LLM qua endpoint /chat/completions theo chuẩn OpenAI. Mặc định trỏ tới Groq;
 * đổi LLM_BASE_URL / LLM_MODEL / LLM_API_KEY là dùng được OpenRouter, Ollama (http://localhost:11434/v1)…
 */
@Slf4j
@Component
public class OpenAiCompatibleLlmClient implements LlmClient {

    private final AppProperties.Llm config;
    private final RestClient http;

    public OpenAiCompatibleLlmClient(AppProperties props) {
        this.config = props.llm();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(config.timeout()).build());
        factory.setReadTimeout(config.timeout());
        this.http = RestClient.builder()
                .baseUrl(config.baseUrl())
                .requestFactory(new BufferingClientHttpRequestFactory(factory)) // gửi kèm Content-Length thay vì chunked
                .build();
    }

    @PostConstruct
    void warnIfNotConfigured() {
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            log.warn("LLM_API_KEY chưa được cấu hình: chatbot sẽ trả câu xin lỗi cho tới khi có key (xem backend/.env.example).");
        } else {
            log.info("LLM sẵn sàng: model {} tại {}", config.model(), config.baseUrl());
        }
    }

    @Override
    public String complete(String systemPrompt, List<Message> history) {
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            throw new IllegalStateException("LLM_API_KEY chưa được cấu hình");
        }
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", systemPrompt));
        // chỉ gửi ~10 tin gần nhất để tiết kiệm token
        messages.addAll(history.subList(Math.max(0, history.size() - 10), history.size()));

        ChatCompletionResponse response;
        try {
            response = http.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + config.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ChatCompletionRequest(config.model(), messages, config.temperature(), config.maxTokens(),
                            config.reasoningEffort() == null || config.reasoningEffort().isBlank() ? null : config.reasoningEffort()))
                    .retrieve()
                    .body(ChatCompletionResponse.class);
        } catch (RestClientResponseException e) {
            // 401 = sai key, 404/400 = sai tên model, 429 = hết hạn mức: ghi rõ để dễ chẩn đoán (không ghi key)
            String body = e.getResponseBodyAsString();
            throw new IllegalStateException("LLM trả HTTP " + e.getStatusCode().value() + " (model=" + config.model()
                    + "): " + (body.length() > 300 ? body.substring(0, 300) : body), e);
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()
                || response.choices().get(0).message() == null) {
            throw new IllegalStateException("LLM trả về phản hồi rỗng");
        }
        String content = response.choices().get(0).message().content();
        if (content == null || content.isBlank()) {
            // model suy luận có thể dùng hết max_tokens cho phần "nghĩ" nên không còn chữ trả lời
            throw new IllegalStateException("LLM không trả nội dung (thử tăng LLM_MAX_TOKENS hoặc hạ LLM_REASONING_EFFORT)");
        }
        return content.trim();
    }

    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private record ChatCompletionRequest(String model, List<Message> messages, double temperature,
                                         @com.fasterxml.jackson.annotation.JsonProperty("max_tokens") int maxTokens,
                                         @com.fasterxml.jackson.annotation.JsonProperty("reasoning_effort") String reasoningEffort) {
    }

    private record ChatCompletionResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }
}
