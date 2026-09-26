package com.codereviewer.application.service;

import com.codereviewer.application.dto.ReviewRequest;
import com.codereviewer.application.dto.ReviewResponse;
import com.codereviewer.domain.models.DiffFile;
import com.codereviewer.domain.models.DiffHunk;
import com.codereviewer.domain.models.DiffLine;
import com.codereviewer.domain.models.DiffSide;
import com.codereviewer.domain.models.Finding;
import com.codereviewer.domain.models.Review;
import com.codereviewer.domain.models.ReviewStatus;
import com.codereviewer.domain.models.Severity;
import com.codereviewer.domain.service.DiffParser;
import com.codereviewer.domain.service.ReviewEngine;
import com.codereviewer.infrastructure.github.GitHubApiClient;
import com.codereviewer.infrastructure.github.GitHubApiException;
import com.codereviewer.infrastructure.github.GitHubDtos;
import com.codereviewer.infrastructure.github.GitHubDtos.CreateReviewRequest;
import com.codereviewer.infrastructure.github.GitHubDtos.PullRequest;
import com.codereviewer.infrastructure.github.GitHubDtos.PullRequestFile;
import com.codereviewer.infrastructure.github.GitHubDtos.SubmittedReview;
import com.codereviewer.infrastructure.github.GitHubProperties;
import com.codereviewer.infrastructure.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private GitHubApiClient gitHubApiClient;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private DiffParser diffParser;
    @Mock
    private ReviewEngine reviewEngine;

    @InjectMocks
    private ReviewService reviewService;

    private final GitHubProperties dryRun = new GitHubProperties("https://api.github.com", "token", false);
    private final GitHubProperties enabled = new GitHubProperties("https://api.github.com", "token", true);

    @Test
    void reviewShouldFetchParseAnalyzePersistAndSkipSubmissionInDryRun() {
        reviewService = new ReviewService(gitHubApiClient, dryRun, reviewRepository, diffParser, reviewEngine);
        ReviewRequest request = new ReviewRequest("owner", "repo", 42);
        when(gitHubApiClient.getPullRequest(anyString(), anyString(), anyInt())).thenReturn(pr());
        when(gitHubApiClient.getPullRequestFiles(anyString(), anyString(), anyInt()))
                .thenReturn(List.of(new PullRequestFile("a.txt", null, "modified", "@@ -1 +1 @@\n-old\n+new")));
        when(diffParser.parse(anyString(), any(), anyString(), anyString()))
                .thenReturn(Optional.of(diffFile("a.txt", "new")));
        when(reviewEngine.analyze(any())).thenReturn(List.of(
                new Finding("rule", Severity.MINOR, "a.txt", 1, "message")));
        when(reviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse response = reviewService.review(request);

        assertThat(response.status()).isEqualTo(ReviewStatus.PENDING);
        assertThat(response.findings()).hasSize(1);
        verify(reviewRepository).save(any(Review.class));
        verify(gitHubApiClient, never()).submitReview(anyString(), anyString(), anyInt(), any());
    }

    @Test
    void reviewShouldSubmitToGitHubWhenEnabled() {
        reviewService = new ReviewService(gitHubApiClient, enabled, reviewRepository, diffParser, reviewEngine);
        ReviewRequest request = new ReviewRequest("owner", "repo", 42);
        when(gitHubApiClient.getPullRequest(anyString(), anyString(), anyInt())).thenReturn(pr());
        when(gitHubApiClient.getPullRequestFiles(anyString(), anyString(), anyInt()))
                .thenReturn(List.of(new PullRequestFile("a.txt", null, "modified", "@@ -1 +1 @@\n-old\n+new")));
        when(diffParser.parse(anyString(), any(), anyString(), anyString()))
                .thenReturn(Optional.of(diffFile("a.txt", "new")));
        when(reviewEngine.analyze(any())).thenReturn(List.of(
                new Finding("rule", Severity.MINOR, "a.txt", 1, "message")));
        when(reviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(gitHubApiClient.submitReview(anyString(), anyString(), anyInt(), any()))
                .thenReturn(new SubmittedReview(99L, "html", "COMMENT"));

        ReviewResponse response = reviewService.review(request);

        assertThat(response.status()).isEqualTo(ReviewStatus.COMPLETED);
        assertThat(response.githubReviewId()).isEqualTo(99L);
        ArgumentCaptor<CreateReviewRequest> captor = ArgumentCaptor.forClass(CreateReviewRequest.class);
        verify(gitHubApiClient).submitReview(anyString(), anyString(), anyInt(), captor.capture());
        assertThat(captor.getValue().comments()).hasSize(1);
        assertThat(captor.getValue().comments().get(0).line()).isEqualTo(1);
        assertThat(captor.getValue().comments().get(0).side()).isEqualTo("RIGHT");
    }

    @Test
    void failedSubmissionShouldMarkReviewPartial() {
        reviewService = new ReviewService(gitHubApiClient, enabled, reviewRepository, diffParser, reviewEngine);
        ReviewRequest request = new ReviewRequest("owner", "repo", 42);
        when(gitHubApiClient.getPullRequest(anyString(), anyString(), anyInt())).thenReturn(pr());
        when(gitHubApiClient.getPullRequestFiles(anyString(), anyString(), anyInt()))
                .thenReturn(List.of(new PullRequestFile("a.txt", null, "modified", "@@ -1 +1 @@\n-old\n+new")));
        when(diffParser.parse(anyString(), any(), anyString(), anyString()))
                .thenReturn(Optional.of(diffFile("a.txt", "new")));
        when(reviewEngine.analyze(any())).thenReturn(List.of());
        when(reviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(gitHubApiClient.submitReview(anyString(), anyString(), anyInt(), any()))
                .thenThrow(new GitHubApiException("boom", 500, null));

        ReviewResponse response = reviewService.review(request);

        assertThat(response.status()).isEqualTo(ReviewStatus.PARTIAL);
        assertThat(response.failureReason()).isNotNull();
    }

    private PullRequest pr() {
        return new PullRequest(1L, 42, "title", "open",
                new GitHubDtos.PullRequestHead("branch", "abc123", null),
                new GitHubDtos.PullRequestHead("main", "def456", null),
                null);
    }

    private DiffFile diffFile(String name, String content) {
        DiffLine line = new DiffLine(DiffSide.RIGHT, null, 1, content, true, false);
        return new DiffFile(name, null, "modified", List.of(new DiffHunk(1, 0, 1, 1, List.of(line))));
    }
}
