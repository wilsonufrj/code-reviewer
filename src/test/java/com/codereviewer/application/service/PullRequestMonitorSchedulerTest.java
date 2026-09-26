package com.codereviewer.application.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.codereviewer.application.config.PullRequestMonitorProperties;
import com.codereviewer.infrastructure.github.GitHubApiClient;
import com.codereviewer.infrastructure.github.GitHubApiException;
import com.codereviewer.infrastructure.github.GitHubDtos.PullRequestSummary;
import com.codereviewer.infrastructure.github.GitHubDtos.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PullRequestMonitorSchedulerTest {

    @Mock
    private GitHubApiClient gitHubApiClient;
    @Mock
    private PullRequestAiReviewService aiReviewService;

    private PullRequestMonitorScheduler scheduler;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        PullRequestMonitorProperties properties = new PullRequestMonitorProperties(
                true, "owner", "repo", 60_000);
        scheduler = new PullRequestMonitorScheduler(gitHubApiClient, properties, aiReviewService);
        logger = (Logger) LoggerFactory.getLogger(PullRequestMonitorScheduler.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void firstPollShouldEstablishBaselineWithoutLoggingPullRequests() {
        when(gitHubApiClient.listOpenPullRequests("owner", "repo"))
                .thenReturn(List.of(pullRequest(1L, 1, "Existing")));

        scheduler.poll();

        assertThat(infoMessages()).isEmpty();
        verify(aiReviewService, never()).reviewAndComment("owner", "repo", 1);
    }

    @Test
    void shouldLogNewPullRequestOnlyOnce() {
        PullRequestSummary existing = pullRequest(1L, 1, "Existing");
        PullRequestSummary added = pullRequest(2L, 2, "Add feature");
        when(gitHubApiClient.listOpenPullRequests("owner", "repo"))
                .thenReturn(List.of(existing), List.of(added, existing), List.of(added, existing));

        scheduler.poll();
        scheduler.poll();
        scheduler.poll();

        assertThat(infoMessages()).containsExactly(
                "New open pull request: owner/repo#2 | title=\"Add feature\" "
                        + "| author=author | url=https://github.com/owner/repo/pull/2");
        verify(aiReviewService).reviewAndComment("owner", "repo", 2);
    }

    @Test
    void shouldLogMultipleNewPullRequests() {
        PullRequestSummary first = pullRequest(2L, 2, "First");
        PullRequestSummary second = pullRequest(3L, 3, "Second");
        when(gitHubApiClient.listOpenPullRequests("owner", "repo"))
                .thenReturn(List.of(), List.of(first, second));

        scheduler.poll();
        scheduler.poll();

        assertThat(infoMessages()).hasSize(2);
        assertThat(infoMessages()).anyMatch(message -> message.contains("repo#2"));
        assertThat(infoMessages()).anyMatch(message -> message.contains("repo#3"));
        verify(aiReviewService).reviewAndComment("owner", "repo", 2);
        verify(aiReviewService).reviewAndComment("owner", "repo", 3);
    }

    @Test
    void apiFailureShouldNotPreventLaterPolling() {
        GitHubApiException failure = new GitHubApiException("GitHub API call failed", 500, null);
        when(gitHubApiClient.listOpenPullRequests("owner", "repo"))
                .thenThrow(failure)
                .thenReturn(List.of())
                .thenReturn(List.of(pullRequest(4L, 4, "Recovered")));

        assertThatCode(scheduler::poll).doesNotThrowAnyException();
        scheduler.poll();
        scheduler.poll();

        assertThat(appender.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage()).contains("Unable to poll pull requests");
        });
        assertThat(infoMessages()).singleElement()
                .satisfies(message -> assertThat(message).contains("repo#4"));
        verify(aiReviewService).reviewAndComment("owner", "repo", 4);
    }

    @Test
    void failedCommentShouldRetryWithoutRepeatingTerminalNotification() {
        PullRequestSummary added = pullRequest(5L, 5, "Retry comment");
        when(gitHubApiClient.listOpenPullRequests("owner", "repo"))
                .thenReturn(List.of(), List.of(added), List.of(added));
        doThrow(new GitHubApiException("Comment failed", 500, null))
                .doNothing()
                .when(aiReviewService).reviewAndComment("owner", "repo", 5);

        scheduler.poll();
        scheduler.poll();
        scheduler.poll();

        assertThat(infoMessages()).singleElement()
                .satisfies(message -> assertThat(message).contains("repo#5"));
        verify(aiReviewService, times(2)).reviewAndComment("owner", "repo", 5);
    }

    private PullRequestSummary pullRequest(long id, int number, String title) {
        return new PullRequestSummary(
                id, number, title, "open", false,
                "https://github.com/owner/repo/pull/" + number,
                Instant.parse("2026-01-01T00:00:00Z"),
                new User(10L, "author"));
    }

    private List<String> infoMessages() {
        return appender.list.stream()
                .filter(event -> event.getLevel() == Level.INFO)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }
}
