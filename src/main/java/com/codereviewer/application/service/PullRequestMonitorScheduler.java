package com.codereviewer.application.service;

import com.codereviewer.application.config.PullRequestMonitorProperties;
import com.codereviewer.infrastructure.ai.AiApiException;
import com.codereviewer.infrastructure.github.GitHubApiClient;
import com.codereviewer.infrastructure.github.GitHubApiException;
import com.codereviewer.infrastructure.github.GitHubDtos.PullRequestSummary;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Polls one repository and logs pull requests first observed after startup. */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "github.pull-request-monitor",
        name = "enabled",
        havingValue = "true")
public class PullRequestMonitorScheduler {

    private final GitHubApiClient gitHubApiClient;
    private final PullRequestMonitorProperties properties;
    private final PullRequestAiReviewService aiReviewService;
    private final Set<Long> observedPullRequestIds = new HashSet<>();
    private final Set<Long> reviewedPullRequestIds = new HashSet<>();
    private boolean initialized;

    @PostConstruct
    void validateConfiguration() {
        properties.validate();
    }

    @Scheduled(fixedDelayString = "${github.pull-request-monitor.interval-ms:60000}")
    public synchronized void poll() {

        List<PullRequestSummary> openPullRequests;
        try {
            openPullRequests = gitHubApiClient.listOpenPullRequests(
                    properties.owner(), properties.repository());
        } catch (GitHubApiException exception) {
            log.warn("Unable to poll pull requests for {}/{}: {}",
                    properties.owner(), properties.repository(), exception.getMessage());
            return;
        }

        if (!initialized) {
            openPullRequests.stream()
                    .map(PullRequestSummary::id)
                    .forEach(observedPullRequestIds::add);
            reviewedPullRequestIds.addAll(observedPullRequestIds);
            initialized = true;
            return;
        }

        for (PullRequestSummary pullRequest : openPullRequests) {
            if (observedPullRequestIds.add(pullRequest.id())) {
                logNewPullRequest(pullRequest);
            }
            if (!reviewedPullRequestIds.contains(pullRequest.id())) {
                reviewWithAi(pullRequest);
            }
        }
    }

    private void reviewWithAi(PullRequestSummary pullRequest) {
        try {
            
            aiReviewService.reviewAndComment(
                    properties.owner(), properties.repository(), pullRequest.number());
            reviewedPullRequestIds.add(pullRequest.id());

        } catch (GitHubApiException | AiApiException exception) {
            log.warn("Unable to complete AI review for pull request {}/{}#{}: {}",
                    properties.owner(), properties.repository(), pullRequest.number(),
                    exception.getMessage());
        }
    }

    private void logNewPullRequest(PullRequestSummary pullRequest) {
        String author = pullRequest.user() == null ? "unknown" : pullRequest.user().login();
        log.info("New open pull request: {}/{}#{} | title=\"{}\" | author={} | url={}",
                properties.owner(), properties.repository(), pullRequest.number(),
                pullRequest.title(), author, pullRequest.htmlUrl());
    }
}
