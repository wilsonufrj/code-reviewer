package com.codereviewer.infrastructure.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for the OpenAI-compatible chat-completions endpoint. */
@ConfigurationProperties(prefix = "ai")
public record AiProperties(
        String chatCompletionsUrl,
        String model,
        String bearerToken,
        int maxPromptCharacters
) {
}
