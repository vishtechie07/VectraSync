package com.vectrasync.core;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;

@Service
public class ModelProvider {

    public enum Provider {
        OPENAI("gpt-4o"),
        GEMINI("gemini-1.5-flash");

        final String defaultModel;
        Provider(String defaultModel) { this.defaultModel = defaultModel; }
    }

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(60);

    private final KeyResolver keyResolver;

    public ModelProvider(KeyResolver keyResolver) {
        this.keyResolver = keyResolver;
    }

    public ChatLanguageModel chatModel() {
        return chatModel(requireKey());
    }

    public ChatLanguageModel chatModel(String apiKey) {
        String key = Objects.requireNonNull(apiKey, "apiKey").trim();
        if (key.isEmpty()) {
            throw new MissingCredentialException("No LLM API key provided.");
        }
        return switch (detect(key)) {
            case OPENAI -> OpenAiChatModel.builder()
                    .apiKey(key)
                    .modelName(Provider.OPENAI.defaultModel)
                    .timeout(HTTP_TIMEOUT)
                    .logRequests(false)
                    .logResponses(false)
                    .build();
            case GEMINI -> GoogleAiGeminiChatModel.builder()
                    .apiKey(key)
                    .modelName(Provider.GEMINI.defaultModel)
                    .timeout(HTTP_TIMEOUT)
                    .build();
        };
    }

    public StreamingChatLanguageModel streamingChatModel() {
        return streamingChatModel(requireKey());
    }

    public StreamingChatLanguageModel streamingChatModel(String apiKey) {
        String key = Objects.requireNonNull(apiKey, "apiKey").trim();
        if (key.isEmpty()) {
            throw new MissingCredentialException("No LLM API key provided.");
        }
        return switch (detect(key)) {
            case OPENAI -> OpenAiStreamingChatModel.builder()
                    .apiKey(key)
                    .modelName(Provider.OPENAI.defaultModel)
                    .timeout(HTTP_TIMEOUT)
                    .build();
            case GEMINI -> GoogleAiGeminiStreamingChatModel.builder()
                    .apiKey(key)
                    .modelName(Provider.GEMINI.defaultModel)
                    .timeout(HTTP_TIMEOUT)
                    .build();
        };
    }

    public Provider detect() {
        return detect(requireKey());
    }

    public static Provider detect(String key) {
        if (key.startsWith("sk-")) return Provider.OPENAI;
        if (key.startsWith("AIza")) return Provider.GEMINI;
        throw new MissingCredentialException(
                "Unrecognized API key prefix. Expected 'sk-' (OpenAI) or 'AIza' (Gemini).");
    }

    private String requireKey() {
        return keyResolver.resolve()
                .orElseThrow(() -> new MissingCredentialException(
                        "No LLM API key in session. Set one under Settings."));
    }
}
