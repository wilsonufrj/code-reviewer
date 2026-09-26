package com.codereviewer.application.web;

import com.codereviewer.application.dto.ReviewRequest;
import com.codereviewer.application.dto.ReviewResponse;
import com.codereviewer.application.service.ReviewService;
import com.codereviewer.domain.models.ReviewStatus;
import com.codereviewer.domain.models.Severity;
import com.codereviewer.application.dto.FindingResponse;
import com.codereviewer.infrastructure.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;
    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewController reviewController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reviewController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void postReviewShouldReturnCreated() throws Exception {
        when(reviewService.review(any(ReviewRequest.class))).thenReturn(new ReviewResponse(
                1L, "owner/repo", 42, "sha", ReviewStatus.PENDING, null, null,
                Instant.parse("2026-01-01T00:00:00Z"),
                List.of(new FindingResponse(1L, "rule", Severity.MINOR, "a.txt", 1, "message"))));

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"owner": "owner", "repo": "repo", "pullRequestNumber": 42}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.repository").value("owner/repo"))
                .andExpect(jsonPath("$.findings[0].rule").value("rule"));
    }

    @Test
    void postReviewShouldRejectInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"owner": "", "repo": "repo"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getLatestShouldReturnReview() throws Exception {
        when(reviewRepository.findByRepositoryAndPullRequestNumberOrderByCreatedAtDesc(
                "owner/repo", 42))
                .thenReturn(java.util.Optional.of(reviewEntity()));

        mockMvc.perform(get("/api/reviews/repositories/owner/repo/pulls/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pullRequestNumber").value(42))
                .andExpect(jsonPath("$.findings[0].file").value("a.txt"));
    }

    @Test
    void getLatestShouldReturn404WhenMissing() throws Exception {
        when(reviewRepository.findByRepositoryAndPullRequestNumberOrderByCreatedAtDesc(
                "owner/repo", 42))
                .thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/reviews/repositories/owner/repo/pulls/42"))
                .andExpect(status().isNotFound());
    }

    private com.codereviewer.domain.models.Review reviewEntity() {
        com.codereviewer.domain.models.Review review =
                new com.codereviewer.domain.models.Review("owner/repo", 42, "sha");
        review.setId(1L);
        review.setStatus(ReviewStatus.PENDING);
        review.addFinding(new com.codereviewer.domain.models.ReviewFinding(
                "rule", Severity.MINOR, "a.txt", 1, "message"));
        return review;
    }
}
