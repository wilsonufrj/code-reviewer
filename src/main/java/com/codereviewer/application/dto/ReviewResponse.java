package com.codereviewer.application.dto;

import com.codereviewer.domain.models.Review;
import com.codereviewer.domain.models.ReviewStatus;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for a review run.
 */
public record ReviewResponse(
        Long id,
        String repository,
        Integer pullRequestNumber,
        String commitSha,
        ReviewStatus status,
        Long githubReviewId,
        String failureReason,
        Instant createdAt,
        List<FindingResponse> findings
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getRepository(),
                review.getPullRequestNumber(),
                review.getCommitSha(),
                review.getStatus(),
                review.getGithubReviewId(),
                review.getFailureReason(),
                review.getCreatedAt(),
                review.getFindings().stream()
                        .map(FindingResponse::from)
                        .toList());
    }
}
