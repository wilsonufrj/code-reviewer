package com.codereviewer.application.web;

import com.codereviewer.application.dto.ReviewResponse;
import com.codereviewer.application.dto.WebhookPayload;
import com.codereviewer.application.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives GitHub webhook events. Payloads are large and may contain secrets:
 * never log them raw.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Receive GitHub webhook events")
public class WebhookController {

    private final WebhookService webhookService;

    /**
     * GitHub webhook entry point. Returns 202 when the event is accepted for
     * processing and 200 when it is acknowledged but ignored.
    */
    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Process a GitHub pull-request webhook")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Pull-request event processed"),
            @ApiResponse(responseCode = "200", description = "Event acknowledged and ignored"),
            @ApiResponse(responseCode = "502", description = "GitHub API request failed")
    })
    public ResponseEntity<ReviewResponse> handleWebhook(
            @Parameter(description = "GitHub event type", example = "pull_request")
            @RequestHeader(value = "X-GitHub-Event", required = false) String event,
            @RequestBody WebhookPayload payload) {

        if (!"pull_request".equals(event)) {
            log.info("Ignoring webhook event type '{}'", event);
            return ResponseEntity.ok().build();
        }

        ReviewResponse result = webhookService.handle(payload);
        if (result == null) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }
}
