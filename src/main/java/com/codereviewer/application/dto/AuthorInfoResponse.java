package com.codereviewer.application.dto;

import java.time.Instant;
import java.util.List;

/** Public GitHub author profile and repositories owned by that author. */
public record AuthorInfoResponse(
        Long id,
        String login,
        String name,
        String avatarUrl,
        String htmlUrl,
        String bio,
        String company,
        String location,
        String blog,
        Integer publicRepositories,
        Integer followers,
        Integer following,
        Instant createdAt,
        Instant updatedAt,
        List<Repository> repositories
) {
    public record Repository(
            Long id,
            String name,
            String fullName,
            String description,
            String htmlUrl,
            String defaultBranch,
            String language,
            String visibility,
            Boolean fork,
            Boolean archived,
            Integer stars,
            Integer forks,
            Integer openIssues,
            Instant createdAt,
            Instant updatedAt,
            Instant pushedAt
    ) {
    }
}
