package com.codereviewer.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO to trigger a review of a pull request.
 */
public record ReviewRequest(
        @NotBlank(message = "owner is required")
        String owner,

        @NotBlank(message = "repo is required")
        String repo,

        @NotNull(message = "pullRequestNumber is required")
        @Positive(message = "pullRequestNumber must be positive")
        Integer pullRequestNumber
) {
}
