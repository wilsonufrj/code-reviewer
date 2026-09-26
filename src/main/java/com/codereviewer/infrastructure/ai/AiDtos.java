package com.codereviewer.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Minimal request and response models for an OpenAI-compatible chat API. */
public final class AiDtos {

    private AiDtos() {
    }

    public record ChatCompletionRequest(String model, List<ChatMessage> messages) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatMessage(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatCompletionResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(Integer index, ChatMessage message) {
    }
}
