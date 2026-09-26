package com.codereviewer.application.service;

import com.codereviewer.application.dto.ReviewRequest;
import com.codereviewer.application.dto.ReviewResponse;
import com.codereviewer.application.dto.WebhookPayload;
import com.codereviewer.infrastructure.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Processes GitHub webhook events. Only {@code pull_request} events with relevant
 * actions trigger a review; everything else is acknowledged and ignored.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private static final List<String> REVIEWABLE_ACTIONS = List.of("opened", "synchronize", "reopened");

    private final ReviewService reviewService;
    private final ReviewRepository reviewRepository;

    /**
     * Handles a webhook payload.
     *
     * @return the review result, or null when the event does not trigger a review
     */
    public ReviewResponse handle(WebhookPayload payload) {
        if (payload == null || payload.pull_request() == null) {
            log.info("Ignoring webhook without pull request data");
            return null;
        }

        String action = payload.action();
        if (!REVIEWABLE_ACTIONS.contains(action)) {
            log.info("Ignoring pull_request action '{}'", action);
            return null;
        }

        String owner = payload.repository().owner().login();
        String repo = payload.repository().name();
        Integer number = payload.pull_request().number();

        log.info("Processing pull_request '{}' event for {}/{}#{}", action, owner, repo, number);
        return reviewService.review(new ReviewRequest(owner, repo, number));
    }
}
