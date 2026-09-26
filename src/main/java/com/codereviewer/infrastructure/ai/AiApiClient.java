package com.codereviewer.infrastructure.ai;

import com.codereviewer.infrastructure.ai.AiDtos.ChatCompletionRequest;
import com.codereviewer.infrastructure.ai.AiDtos.ChatCompletionResponse;
import com.codereviewer.infrastructure.ai.AiDtos.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

/** Client for the configured OpenAI-compatible chat-completions endpoint. */
@Component
public class AiApiClient {

    private final AiProperties properties;
    private final RestClient restClient;

    @Autowired
    public AiApiClient(AiProperties properties) {
        this(properties, RestClient.builder());
    }

    AiApiClient(AiProperties properties, RestClient.Builder builder) {

        this.properties = properties;
        RestClient.Builder configuredBuilder = builder.defaultHeader(
                HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if (properties.bearerToken() != null && !properties.bearerToken().isBlank()) {
            configuredBuilder.defaultHeader(
                    HttpHeaders.AUTHORIZATION, "Bearer " + properties.bearerToken());
        }
        
        this.restClient = configuredBuilder.build();
    }

    /** Sends one user prompt and returns only the assistant's visible content. */
    public String complete(String content) {
        ChatCompletionRequest request = new ChatCompletionRequest(
                properties.model(), List.of(new ChatMessage("user", content)));
        try {
            ChatCompletionResponse response = restClient.post()
                    .uri(properties.chatCompletionsUrl())
                    .body(request)
                    .retrieve()
                    .body(ChatCompletionResponse.class);
            return extractContent(response);
        } catch (AiApiException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw new AiApiException(
                    "AI API call failed with status " + exception.getStatusCode().value(),
                    exception);
        } catch (RuntimeException exception) {
            throw new AiApiException("AI API call failed", exception);
        }
    }

    private String extractContent(ChatCompletionResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()
                || response.choices().getFirst() == null
                || response.choices().getFirst().message() == null
                || response.choices().getFirst().message().content() == null
                || response.choices().getFirst().message().content().isBlank()) {
            throw new AiApiException("AI API returned no assistant content");
        }
        return response.choices().getFirst().message().content();
    }
}
