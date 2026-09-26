package com.codereviewer.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Webhook payload for {@code pull_request} events. Only the fields the service
 * needs are modeled; unknown fields are ignored. Never log raw payloads —
 * they are large and may contain secrets.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookPayload(
        String action,
        PullRequestPayload pull_request,
        RepositoryPayload repository
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PullRequestPayload(
            Integer number,
            String title,
            String state,
            HeadPayload head,
            HeadPayload base
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HeadPayload(
            String ref,
            String sha
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RepositoryPayload(
            String name,
            OwnerPayload owner
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OwnerPayload(
            String login
    ) {
    }
}
