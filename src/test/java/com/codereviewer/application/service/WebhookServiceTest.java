package com.codereviewer.application.service;

import com.codereviewer.application.dto.ReviewRequest;
import com.codereviewer.application.dto.ReviewResponse;
import com.codereviewer.application.dto.WebhookPayload;
import com.codereviewer.infrastructure.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private ReviewService reviewService;
    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private WebhookService webhookService;

    @Test
    void shouldTriggerReviewForOpenedAction() {
        when(reviewService.review(new ReviewRequest("owner", "repo", 7)))
                .thenReturn(new ReviewResponse(1L, "owner/repo", 7, "sha", null, null, null, null, null));
        WebhookPayload payload = payload("opened");

        ReviewResponse result = webhookService.handle(payload);

        assertThat(result).isNotNull();
        verify(reviewService).review(new ReviewRequest("owner", "repo", 7));
    }

    @Test
    void shouldTriggerReviewForSynchronizeAndReopened() {
        when(reviewService.review(new ReviewRequest("owner", "repo", 7))).thenReturn(null);
        webhookService.handle(payload("synchronize"));
        webhookService.handle(payload("reopened"));

        verify(reviewService, org.mockito.Mockito.times(2)).review(new ReviewRequest("owner", "repo", 7));
    }

    @Test
    void shouldIgnoreOtherActions() {
        ReviewResponse result = webhookService.handle(payload("closed"));

        assertThat(result).isNull();
        verifyNoInteractions(reviewService);
    }

    @Test
    void shouldIgnorePayloadWithoutPullRequest() {
        ReviewResponse result = webhookService.handle(new WebhookPayload("opened", null, null));

        assertThat(result).isNull();
        verifyNoInteractions(reviewService);
    }

    @Test
    void shouldIgnoreNullPayload() {
        ReviewResponse result = webhookService.handle(null);

        assertThat(result).isNull();
        verifyNoInteractions(reviewService);
    }

    private WebhookPayload payload(String action) {
        return new WebhookPayload(
                action,
                new WebhookPayload.PullRequestPayload(7, "title", "open",
                        new WebhookPayload.HeadPayload("branch", "sha"),
                        new WebhookPayload.HeadPayload("main", "sha")),
                new WebhookPayload.RepositoryPayload("repo",
                        new WebhookPayload.OwnerPayload("owner")));
    }
}
