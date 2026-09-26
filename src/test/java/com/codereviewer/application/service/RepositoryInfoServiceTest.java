package com.codereviewer.application.service;

import com.codereviewer.application.dto.AuthorInfoResponse;
import com.codereviewer.application.dto.RepositoryInfoResponse;
import com.codereviewer.infrastructure.github.GitHubApiClient;
import com.codereviewer.infrastructure.github.GitHubDtos.Account;
import com.codereviewer.infrastructure.github.GitHubDtos.Contributor;
import com.codereviewer.infrastructure.github.GitHubDtos.RepositoryDetails;
import com.codereviewer.infrastructure.github.GitHubDtos.UserProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryInfoServiceTest {

    private static final Instant DATE = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private GitHubApiClient gitHubApiClient;

    @InjectMocks
    private RepositoryInfoService repositoryInfoService;

    @Test
    void shouldReturnRepositoryWithAuthors() {
        when(gitHubApiClient.getRepository("owner", "repo")).thenReturn(repository());
        when(gitHubApiClient.getRepositoryContributors("owner", "repo"))
                .thenReturn(List.of(new Contributor(2L, "author", "avatar", "profile", 12)));

        RepositoryInfoResponse response = repositoryInfoService.getRepository("owner", "repo");

        assertThat(response.fullName()).isEqualTo("owner/repo");
        assertThat(response.owner().login()).isEqualTo("owner");
        assertThat(response.authors()).singleElement()
                .satisfies(author -> {
                    assertThat(author.login()).isEqualTo("author");
                    assertThat(author.contributions()).isEqualTo(12);
                });
    }

    @Test
    void shouldReturnAuthorWithOwnedRepositories() {
        when(gitHubApiClient.getUser("author")).thenReturn(new UserProfile(
                2L, "author", "Author Name", "avatar", "profile", "bio", "company",
                "location", "https://example.com", 1, 10, 3, DATE, DATE));
        when(gitHubApiClient.getUserRepositories("author")).thenReturn(List.of(repository()));

        AuthorInfoResponse response = repositoryInfoService.getAuthor("author");

        assertThat(response.login()).isEqualTo("author");
        assertThat(response.repositories()).singleElement()
                .extracting(AuthorInfoResponse.Repository::fullName)
                .isEqualTo("owner/repo");
    }

    private RepositoryDetails repository() {
        return new RepositoryDetails(
                1L, "repo", "owner/repo", "description", "repository-url", "main", "Java",
                "public", false, false, 5, 2, 1, DATE, DATE, DATE,
                new Account(1L, "owner", "User", "avatar", "profile"));
    }
}
