package com.codereviewer.infrastructure.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * Minimal GitHub API DTOs. Only the fields the service needs are modeled;
 * unknown fields are ignored so GitHub can extend payloads without breaking us.
 */
public final class GitHubDtos {

    private GitHubDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PullRequest(
            Long id,
            Integer number,
            String title,
            String state,
            PullRequestHead head,
            PullRequestHead base,
            User user
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PullRequestHead(
            String ref,
            String sha,
            Repository repo
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Repository(
            Long id,
            String name,
            String fullName,
            Owner owner
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Owner(
            String login
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(
            Long id,
            String login
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PullRequestFile(
            String filename,
            String previousFilename,
            String status,
            String patch
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CreateReviewRequest(
            String commitId,
            String body,
            String event,
            List<ReviewComment> comments
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReviewComment(
            String path,
            Integer line,
            String side,
            String body
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SubmittedReview(
            Long id,
            String htmlUrl,
            String state
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RepositoryDetails(
            Long id,
            String name,
            @JsonProperty("full_name") String fullName,
            String description,
            @JsonProperty("html_url") String htmlUrl,
            @JsonProperty("default_branch") String defaultBranch,
            String language,
            String visibility,
            Boolean fork,
            Boolean archived,
            @JsonProperty("stargazers_count") Integer stars,
            @JsonProperty("forks_count") Integer forks,
            @JsonProperty("open_issues_count") Integer openIssues,
            @JsonProperty("created_at") Instant createdAt,
            @JsonProperty("updated_at") Instant updatedAt,
            @JsonProperty("pushed_at") Instant pushedAt,
            Account owner
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Account(
            Long id,
            String login,
            String type,
            @JsonProperty("avatar_url") String avatarUrl,
            @JsonProperty("html_url") String htmlUrl
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contributor(
            Long id,
            String login,
            @JsonProperty("avatar_url") String avatarUrl,
            @JsonProperty("html_url") String htmlUrl,
            Integer contributions
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserProfile(
            Long id,
            String login,
            String name,
            @JsonProperty("avatar_url") String avatarUrl,
            @JsonProperty("html_url") String htmlUrl,
            String bio,
            String company,
            String location,
            String blog,
            @JsonProperty("public_repos") Integer publicRepositories,
            Integer followers,
            Integer following,
            @JsonProperty("created_at") Instant createdAt,
            @JsonProperty("updated_at") Instant updatedAt
    ) {
    }
}
