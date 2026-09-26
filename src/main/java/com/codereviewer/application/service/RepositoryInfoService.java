package com.codereviewer.application.service;

import com.codereviewer.application.dto.AuthorInfoResponse;
import com.codereviewer.application.dto.RepositoryInfoResponse;
import com.codereviewer.infrastructure.github.GitHubApiClient;
import com.codereviewer.infrastructure.github.GitHubDtos.Account;
import com.codereviewer.infrastructure.github.GitHubDtos.Contributor;
import com.codereviewer.infrastructure.github.GitHubDtos.RepositoryDetails;
import com.codereviewer.infrastructure.github.GitHubDtos.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Coordinates repository and author metadata queries against GitHub. */
@Service
@RequiredArgsConstructor
public class RepositoryInfoService {

    private final GitHubApiClient gitHubApiClient;

    public RepositoryInfoResponse getRepository(String owner, String repo) {
        RepositoryDetails repository = gitHubApiClient.getRepository(owner, repo);
        List<Contributor> contributors = gitHubApiClient.getRepositoryContributors(owner, repo);
        return toRepositoryInfo(repository, contributors);
    }

    public AuthorInfoResponse getAuthor(String username) {
        UserProfile author = gitHubApiClient.getUser(username);
        List<RepositoryDetails> repositories = gitHubApiClient.getUserRepositories(username);
        return new AuthorInfoResponse(
                author.id(), author.login(), author.name(), author.avatarUrl(), author.htmlUrl(),
                author.bio(), author.company(), author.location(), author.blog(),
                author.publicRepositories(), author.followers(), author.following(),
                author.createdAt(), author.updatedAt(),
                repositories.stream().map(this::toAuthorRepository).toList());
    }

    private RepositoryInfoResponse toRepositoryInfo(RepositoryDetails repository,
                                                     List<Contributor> contributors) {
        Account owner = repository.owner();
        RepositoryInfoResponse.Owner ownerResponse = owner == null ? null
                : new RepositoryInfoResponse.Owner(owner.id(), owner.login(), owner.type(),
                owner.avatarUrl(), owner.htmlUrl());
        List<RepositoryInfoResponse.Author> authors = contributors.stream()
                .map(author -> new RepositoryInfoResponse.Author(
                        author.id(), author.login(), author.avatarUrl(), author.htmlUrl(),
                        author.contributions()))
                .toList();
        return new RepositoryInfoResponse(
                repository.id(), repository.name(), repository.fullName(), repository.description(),
                repository.htmlUrl(), repository.defaultBranch(), repository.language(),
                repository.visibility(), repository.fork(), repository.archived(), repository.stars(),
                repository.forks(), repository.openIssues(), repository.createdAt(),
                repository.updatedAt(), repository.pushedAt(), ownerResponse, authors);
    }

    private AuthorInfoResponse.Repository toAuthorRepository(RepositoryDetails repository) {
        return new AuthorInfoResponse.Repository(
                repository.id(), repository.name(), repository.fullName(), repository.description(),
                repository.htmlUrl(), repository.defaultBranch(), repository.language(),
                repository.visibility(), repository.fork(), repository.archived(), repository.stars(),
                repository.forks(), repository.openIssues(), repository.createdAt(),
                repository.updatedAt(), repository.pushedAt());
    }
}
