package com.codereviewer.application.web;

import com.codereviewer.application.dto.ReviewRequest;
import com.codereviewer.application.dto.ReviewResponse;
import com.codereviewer.application.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints to trigger and inspect reviews.
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Trigger reviews and query persisted review results")
public class ReviewController {

    private final ReviewService reviewService;
    private final com.codereviewer.infrastructure.repository.ReviewRepository reviewRepository;

    /** Triggers a review of a pull request. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Review a pull request")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Review completed or persisted for dry-run mode"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "502", description = "GitHub API request failed")
    })
    public ReviewResponse review(@Valid @RequestBody ReviewRequest request) {
        return reviewService.review(request);
    }

    /** Lists all reviews of a repository. */
    @GetMapping("/repositories/{owner}/{repo}")
    @Operation(summary = "List reviews for a repository")
    @ApiResponse(responseCode = "200", description = "Reviews ordered from newest to oldest")
    public List<ReviewResponse> listForRepository(@PathVariable String owner,
                                                  @PathVariable String repo) {
        String repository = owner + "/" + repo;
        return reviewRepository.findByRepositoryOrderByCreatedAtDesc(repository).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    /** Returns the latest review of a pull request. */
    @GetMapping("/repositories/{owner}/{repo}/pulls/{number}")
    @Operation(summary = "Get the latest review for a pull request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Latest review found"),
            @ApiResponse(responseCode = "404", description = "No review exists for the pull request")
    })
    public ReviewResponse latestForPullRequest(@PathVariable String owner,
                                               @PathVariable String repo,
                                               @PathVariable Integer number) {
        String repository = owner + "/" + repo;
        return reviewRepository
                .findByRepositoryAndPullRequestNumberOrderByCreatedAtDesc(repository, number)
                .map(ReviewResponse::from)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No review found for " + repository + "#" + number));
    }
}
