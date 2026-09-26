package com.codereviewer.infrastructure.github;

import com.codereviewer.infrastructure.github.GitHubDtos.CreateReviewRequest;
import com.codereviewer.infrastructure.github.GitHubDtos.Contributor;
import com.codereviewer.infrastructure.github.GitHubDtos.PullRequest;
import com.codereviewer.infrastructure.github.GitHubDtos.PullRequestFile;
import com.codereviewer.infrastructure.github.GitHubDtos.RepositoryDetails;
import com.codereviewer.infrastructure.github.GitHubDtos.SubmittedReview;
import com.codereviewer.infrastructure.github.GitHubDtos.UserProfile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

/**
 * Thin REST client for the GitHub API. All GitHub-specific knowledge lives here;
 * nothing outside {@code infrastructure} may import these types.
 */
@Component
public class GitHubApiClient {

    private final RestClient restClient;

    public GitHubApiClient(GitHubProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.apiBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.token())
                .build();
    }

    /** Fetches a single pull request. */
    public PullRequest getPullRequest(String owner, String repo, int pullNumber) {
        return execute(() -> restClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{number}", owner, repo, pullNumber)
                .retrieve()
                .body(PullRequest.class));
    }

    /** Fetches the files (with patches) of a pull request. */
    public List<PullRequestFile> getPullRequestFiles(String owner, String repo, int pullNumber) {
        return execute(() -> restClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{number}/files?per_page=100", owner, repo, pullNumber)
                .retrieve()
                .body(new ParameterizedTypeReference<List<PullRequestFile>>() {
                }));
    }

    /** Submits findings as a pull request review. */
    public SubmittedReview submitReview(String owner, String repo, int pullNumber,
                                        CreateReviewRequest request) {
        return execute(() -> restClient.post()
                .uri("/repos/{owner}/{repo}/pulls/{number}/reviews", owner, repo, pullNumber)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(SubmittedReview.class));
    }

    /** Fetches public metadata for a repository. */
    public RepositoryDetails getRepository(String owner, String repo) {
        return execute(() -> restClient.get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .retrieve()
                .body(RepositoryDetails.class));
    }

    /** Fetches up to 100 contributors, ordered by contribution count. */
    public List<Contributor> getRepositoryContributors(String owner, String repo) {
        return execute(() -> restClient.get()
                .uri("/repos/{owner}/{repo}/contributors?per_page=100", owner, repo)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Contributor>>() {
                }));
    }

    /** Fetches public profile data for a GitHub user. */
    public UserProfile getUser(String username) {
        return execute(() -> restClient.get()
                .uri("/users/{username}", username)
                .retrieve()
                .body(UserProfile.class));
    }

    /** Fetches up to 100 public repositories owned by a GitHub user. */
    public List<RepositoryDetails> getUserRepositories(String username) {
        return execute(() -> restClient.get()
                .uri("/users/{username}/repos?type=owner&sort=updated&per_page=100", username)
                .retrieve()
                .body(new ParameterizedTypeReference<List<RepositoryDetails>>() {
                }));
    }

    private <T> T execute(GitHubCall<T> call) {
        try {
            return call.run();
        } catch (RestClientResponseException e) {
            throw new GitHubApiException(
                    "GitHub API call failed with status " + e.getStatusCode().value(),
                    e.getStatusCode().value(), e);
        } catch (RuntimeException e) {
            throw new GitHubApiException("GitHub API call failed: " + e.getMessage(), 0, e);
        }
    }

    @FunctionalInterface
    private interface GitHubCall<T> {
        T run();
    }
}
