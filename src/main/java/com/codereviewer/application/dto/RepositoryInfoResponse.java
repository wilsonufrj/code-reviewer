package com.codereviewer.application.dto;

import java.time.Instant;
import java.util.List;

/** Public repository metadata and its contributing authors. */
public record RepositoryInfoResponse(
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
        Instant pushedAt,
        Owner owner,
        List<Author> authors
) {
    public record Owner(Long id, String login, String type, String avatarUrl, String htmlUrl) {
    }

    public record Author(Long id, String login, String avatarUrl, String htmlUrl,
                         Integer contributions) {
    }
}
