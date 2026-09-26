package com.codereviewer.application.service;

import com.codereviewer.application.dto.ReviewRequest;
import com.codereviewer.application.dto.ReviewResponse;
import com.codereviewer.domain.models.DiffFile;
import com.codereviewer.domain.models.Finding;
import com.codereviewer.domain.models.Review;
import com.codereviewer.domain.models.ReviewFinding;
import com.codereviewer.domain.models.ReviewStatus;
import com.codereviewer.domain.models.Severity;
import com.codereviewer.domain.service.DiffParser;
import com.codereviewer.domain.service.ReviewEngine;
import com.codereviewer.infrastructure.github.GitHubApiClient;
import com.codereviewer.infrastructure.github.GitHubDtos.CreateReviewRequest;
import com.codereviewer.infrastructure.github.GitHubDtos.PullRequest;
import com.codereviewer.infrastructure.github.GitHubDtos.PullRequestFile;
import com.codereviewer.infrastructure.github.GitHubDtos.ReviewComment;
import com.codereviewer.infrastructure.github.GitHubDtos.SubmittedReview;
import com.codereviewer.infrastructure.github.GitHubProperties;
import com.codereviewer.infrastructure.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the review use case: fetch → parse → analyze → persist → post.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final GitHubApiClient gitHubApiClient;
    private final GitHubProperties gitHubProperties;
    private final ReviewRepository reviewRepository;
    private final DiffParser diffParser;
    private final ReviewEngine reviewEngine;

    /**
     * Runs a full review of a pull request.
     *
     * @return the persisted review with its findings
     */
    @Transactional
    public ReviewResponse review(ReviewRequest request) {

        String repository = request.owner() + "/" + request.repo();

        PullRequest pr = gitHubApiClient.getPullRequest(request.owner(), request.repo(),
                request.pullRequestNumber());
                
        List<PullRequestFile> files = gitHubApiClient.getPullRequestFiles(request.owner(),
                request.repo(), request.pullRequestNumber());

        List<DiffFile> parsedFiles = files.stream()
                .map(f -> diffParser.parse(f.filename(), f.previousFilename(), f.status(), f.patch()))
                .flatMap(Optional::stream)
                .toList();

        List<Finding> findings = reviewEngine.analyze(parsedFiles);

        Review review = new Review(repository, request.pullRequestNumber(), pr.head().sha());
        review.addAllFindings(findings.stream().map(ReviewFinding::from).toList());
        reviewRepository.save(review);

        if (gitHubProperties.enabled()) {
            submitToGitHub(request, pr, findings, review);
        } else {
            log.info("Dry-run mode: skipping GitHub submission for {}/{}#{}",
                    request.owner(), request.repo(), request.pullRequestNumber());
        }

        return ReviewResponse.from(review);
    }

    private void submitToGitHub(ReviewRequest request, PullRequest pr, List<Finding> findings,
                                Review review) {
        try {
            List<ReviewComment> comments = findings.stream()
                    .map(f -> new ReviewComment(f.file(), f.line(), "RIGHT", formatComment(f)))
                    .toList();

            CreateReviewRequest reviewRequest = new CreateReviewRequest(
                    pr.head().sha(),
                    buildSummaryBody(findings),
                    "COMMENT",
                    comments);

            SubmittedReview submitted = gitHubApiClient.submitReview(
                    request.owner(), request.repo(), request.pullRequestNumber(), reviewRequest);

            review.setGithubReviewId(submitted.id());
            review.setStatus(ReviewStatus.COMPLETED);
        } catch (RuntimeException e) {
            log.error("Failed to submit review to GitHub for {}#{}: {}",
                    request.repo(), request.pullRequestNumber(), e.getMessage());
            review.setStatus(ReviewStatus.PARTIAL);
            review.setFailureReason(truncate(e.getMessage()));
        }
    }

    private String buildSummaryBody(List<Finding> findings) {
        if (findings.isEmpty()) {
            return "Automated review found no issues. Nice work!";
        }
        long blockers = findings.stream().filter(f -> f.severity() == Severity.BLOCKER).count();
        long critical = findings.stream().filter(f -> f.severity() == Severity.CRITICAL).count();
        long major = findings.stream().filter(f -> f.severity() == Severity.MAJOR).count();
        long minor = findings.stream().filter(f -> f.severity() == Severity.MINOR).count();
        long info = findings.stream().filter(f -> f.severity() == Severity.INFO).count();
        return "Automated review found " + findings.size() + " issue(s): "
                + blockers + " blocker, " + critical + " critical, " + major + " major, "
                + minor + " minor, " + info + " info.";
    }

    private String formatComment(Finding f) {
        return "[" + f.severity() + "] " + f.message() + " (rule: " + f.rule() + ")";
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
