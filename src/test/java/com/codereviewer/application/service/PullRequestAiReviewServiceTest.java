package com.codereviewer.application.service;

import com.codereviewer.infrastructure.ai.AiApiClient;
import com.codereviewer.infrastructure.ai.AiApiException;
import com.codereviewer.infrastructure.ai.AiProperties;
import com.codereviewer.infrastructure.github.GitHubApiClient;
import com.codereviewer.infrastructure.github.GitHubApiException;
import com.codereviewer.infrastructure.github.GitHubDtos.PullRequestCommentRequest;
import com.codereviewer.infrastructure.github.GitHubDtos.PullRequestFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PullRequestAiReviewServiceTest {

    @Mock
    private GitHubApiClient gitHubApiClient;
    @Mock
    private AiApiClient aiApiClient;

    @Test
    void shouldSendPatchesAndPostCombinedAiAndFileReport() {
        PullRequestAiReviewService service = service(50_000);
        when(gitHubApiClient.getPullRequestFiles("owner", "repo", 42)).thenReturn(List.of(
                file("src/App.java", null, "modified", "@@ -1 +1 @@\n-old\n+new"),
                file("assets/new.bin", "assets/old.bin", "renamed", null)));
        when(aiApiClient.complete(anyString())).thenReturn("Found one issue.");
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PullRequestCommentRequest> comment =
                ArgumentCaptor.forClass(PullRequestCommentRequest.class);

        service.reviewAndComment("owner", "repo", 42);

        verify(aiApiClient).complete(prompt.capture());
        assertThat(prompt.getValue())
                .contains("Focus on correctness, security, and maintainability")
                .contains("File: src/App.java")
                .contains("@@ -1 +1 @@\n-old\n+new")
                .contains("Previous file: assets/old.bin")
                .contains("[Patch unavailable]");
        verify(gitHubApiClient).createPullRequestComment(
                org.mockito.ArgumentMatchers.eq("owner"),
                org.mockito.ArgumentMatchers.eq("repo"),
                org.mockito.ArgumentMatchers.eq(42), comment.capture());
        assertThat(comment.getValue().body())
                .contains("## AI code review")
                .contains("Found one issue.")
                .contains("## Changed files")
                .contains("`src/App.java` — modified")
                .contains("`assets/old.bin` → `assets/new.bin` — renamed");
    }

    @Test
    void shouldSplitLargePatchIntoBoundedPrompts() {
        int promptLimit = 800;
        PullRequestAiReviewService service = service(promptLimit);
        String patch = ("+a very long changed line\n").repeat(100);
        when(gitHubApiClient.getPullRequestFiles("owner", "repo", 42))
                .thenReturn(List.of(file("large.txt", null, "modified", patch)));
        when(aiApiClient.complete(anyString())).thenReturn("review");
        ArgumentCaptor<String> prompts = ArgumentCaptor.forClass(String.class);

        service.reviewAndComment("owner", "repo", 42);

        verify(aiApiClient, atLeast(2)).complete(prompts.capture());
        assertThat(prompts.getAllValues())
                .allSatisfy(prompt -> assertThat(prompt.length()).isLessThanOrEqualTo(promptLimit))
                .anySatisfy(prompt -> assertThat(prompt).contains("Patch continuation segment"));
    }

    @Test
    void shouldResumeFromFailedAiChunkWithoutRepeatingSuccessfulChunk() {
        PullRequestAiReviewService service = service(800);
        String patch = ("+changed line\n").repeat(14);
        when(gitHubApiClient.getPullRequestFiles("owner", "repo", 42))
                .thenReturn(List.of(
                        file("first.txt", null, "modified", patch),
                        file("second.txt", null, "modified", patch)));
        when(aiApiClient.complete(anyString()))
                .thenReturn("first")
                .thenThrow(new AiApiException("temporary failure"))
                .thenReturn("second");

        assertThatThrownBy(() -> service.reviewAndComment("owner", "repo", 42))
                .isInstanceOf(AiApiException.class);
        service.reviewAndComment("owner", "repo", 42);

        verify(gitHubApiClient, times(1)).getPullRequestFiles("owner", "repo", 42);
        verify(aiApiClient, times(3)).complete(anyString());
        verify(gitHubApiClient, times(2)).createPullRequestComment(
                anyString(), anyString(), anyInt(), any());
    }

    @Test
    void shouldResumeAtFailedGithubComment() {
        PullRequestAiReviewService service = service(800);
        String patch = ("+changed line\n").repeat(14);
        when(gitHubApiClient.getPullRequestFiles("owner", "repo", 42))
                .thenReturn(List.of(
                        file("first.txt", null, "modified", patch),
                        file("second.txt", null, "modified", patch)));
        when(aiApiClient.complete(anyString())).thenReturn("review");
        doNothing()
                .doThrow(new GitHubApiException("temporary failure", 500, null))
                .doNothing()
                .when(gitHubApiClient)
                .createPullRequestComment(anyString(), anyString(), anyInt(), any());

        assertThatThrownBy(() -> service.reviewAndComment("owner", "repo", 42))
                .isInstanceOf(GitHubApiException.class);
        service.reviewAndComment("owner", "repo", 42);

        verify(aiApiClient, times(2)).complete(anyString());
        verify(gitHubApiClient, times(3)).createPullRequestComment(
                anyString(), anyString(), anyInt(), any());
    }

    @Test
    void shouldPostNoDiffMessageWithoutCallingAi() {
        PullRequestAiReviewService service = service(50_000);
        when(gitHubApiClient.getPullRequestFiles("owner", "repo", 42)).thenReturn(List.of());
        ArgumentCaptor<PullRequestCommentRequest> comment =
                ArgumentCaptor.forClass(PullRequestCommentRequest.class);

        service.reviewAndComment("owner", "repo", 42);

        verify(aiApiClient, never()).complete(anyString());
        verify(gitHubApiClient).createPullRequestComment(
                anyString(), anyString(), anyInt(), comment.capture());
        assertThat(comment.getValue().body()).contains("No diff was available for AI analysis");
    }

    @Test
    void shouldSplitLongAiOutputIntoNumberedCommentsBelowSafeLimit() {
        PullRequestAiReviewService service = service(50_000);
        when(gitHubApiClient.getPullRequestFiles("owner", "repo", 42))
                .thenReturn(List.of(file("file.txt", null, "modified", "+change")));
        when(aiApiClient.complete(anyString())).thenReturn("x".repeat(120_000));
        ArgumentCaptor<PullRequestCommentRequest> comments =
                ArgumentCaptor.forClass(PullRequestCommentRequest.class);

        service.reviewAndComment("owner", "repo", 42);

        verify(gitHubApiClient, times(3)).createPullRequestComment(
                anyString(), anyString(), anyInt(), comments.capture());
        assertThat(comments.getAllValues())
                .extracting(PullRequestCommentRequest::body)
                .allSatisfy(body -> {
                    assertThat(body.length()).isLessThanOrEqualTo(60_000);
                    assertThat(body).startsWith("## Automated AI review — part ");
                });
    }

    private PullRequestAiReviewService service(int promptLimit) {
        return new PullRequestAiReviewService(
                gitHubApiClient,
                aiApiClient,
                new AiProperties("http://ai/chat/completions", "model", null, promptLimit));
    }

    private PullRequestFile file(String filename, String previousFilename,
                                 String status, String patch) {
        return new PullRequestFile(filename, previousFilename, status, patch);
    }
}
