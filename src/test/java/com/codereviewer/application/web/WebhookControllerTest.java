package com.codereviewer.application.web;

import com.codereviewer.application.dto.ReviewRequest;
import com.codereviewer.application.dto.ReviewResponse;
import com.codereviewer.application.dto.WebhookPayload;
import com.codereviewer.application.service.WebhookService;
import com.codereviewer.domain.models.ReviewStatus;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    @Mock
    private WebhookService webhookService;

    @InjectMocks
    private WebhookController webhookController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(webhookController).build();
    }

    @Test
    void pullRequestEventShouldBeAccepted() throws Exception {
        when(webhookService.handle(any(WebhookPayload.class))).thenReturn(new ReviewResponse(
                1L, "owner/repo", 7, "sha", ReviewStatus.PENDING, null, null,
                Instant.parse("2026-01-01T00:00:00Z"), List.of()));

        mockMvc.perform(post("/webhook")
                        .header("X-GitHub-Event", "pull_request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "opened",
                                  "repository": {"name": "repo", "owner": {"login": "owner"}},
                                  "pull_request": {"number": 7}
                                }
                                """))
                .andExpect(status().isAccepted());
    }

    @Test
    void nonPullRequestEventsShouldBeIgnored() throws Exception {
        mockMvc.perform(post("/webhook")
                        .header("X-GitHub-Event", "push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        verify(webhookService, never()).handle(any());
    }

    @Test
    void ignoredActionsShouldReturnOk() throws Exception {
        when(webhookService.handle(any(WebhookPayload.class))).thenReturn(null);

        mockMvc.perform(post("/webhook")
                        .header("X-GitHub-Event", "pull_request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "closed",
                                  "repository": {"name": "repo", "owner": {"login": "owner"}},
                                  "pull_request": {"number": 7}
                                }
                                """))
                .andExpect(status().isOk());
    }
}
