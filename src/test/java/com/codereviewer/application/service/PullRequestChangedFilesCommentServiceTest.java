package com.codereviewer.application.service;

import com.codereviewer.infrastructure.github.GitHubApiClient;
import com.codereviewer.infrastructure.github.GitHubDtos.PullRequestCommentRequest;
import com.codereviewer.infrastructure.github.GitHubDtos.PullRequestFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PullRequestChangedFilesCommentServiceTest {

    @Mock
    private GitHubApiClient gitHubApiClient;

    @Test
    void shouldPostCommentListingEveryChangedFile() {
        PullRequestChangedFilesCommentService service = service();
        when(gitHubApiClient.getPullRequestFiles("owner", "repo", 42)).thenReturn(List.of(
                file("src/App.java", null, "modified"),
                file("src/New.java", null, "added"),
                file("src/Renamed.java", "src/Old.java", "renamed")));
        ArgumentCaptor<PullRequestCommentRequest> request =
                ArgumentCaptor.forClass(PullRequestCommentRequest.class);

        service.commentOnChangedFiles("owner", "repo", 42);

        verify(gitHubApiClient).createPullRequestComment(
                org.mockito.ArgumentMatchers.eq("owner"),
                org.mockito.ArgumentMatchers.eq("repo"),
                org.mockito.ArgumentMatchers.eq(42), request.capture());
        assertThat(request.getValue().body())
                .contains("Changed files 1-3 of 3")
                .contains("`src/App.java` — modified")
                .contains("`src/New.java` — added")
                .contains("`src/Old.java` → `src/Renamed.java` — renamed");
    }

    @Test
    void shouldSplitLargeReportsIntoCommentsOfOneHundredFiles() {
        PullRequestChangedFilesCommentService service = service();
        List<PullRequestFile> files = IntStream.rangeClosed(1, 101)
                .mapToObj(index -> file("file-" + index + ".txt", null, "modified"))
                .toList();
        when(gitHubApiClient.getPullRequestFiles("owner", "repo", 42)).thenReturn(files);
        ArgumentCaptor<PullRequestCommentRequest> requests =
                ArgumentCaptor.forClass(PullRequestCommentRequest.class);

        service.commentOnChangedFiles("owner", "repo", 42);

        verify(gitHubApiClient, times(2)).createPullRequestComment(
                org.mockito.ArgumentMatchers.eq("owner"),
                org.mockito.ArgumentMatchers.eq("repo"),
                org.mockito.ArgumentMatchers.eq(42), requests.capture());
        assertThat(requests.getAllValues()).extracting(PullRequestCommentRequest::body)
                .anySatisfy(body -> assertThat(body).contains("Changed files 1-100 of 101"))
                .anySatisfy(body -> assertThat(body).contains("Changed files 101-101 of 101"));
    }

    @Test
    void shouldNotPostAnEmptyChangedFilesComment() {
        PullRequestChangedFilesCommentService service = service();
        when(gitHubApiClient.getPullRequestFiles("owner", "repo", 42)).thenReturn(List.of());

        service.commentOnChangedFiles("owner", "repo", 42);

        verify(gitHubApiClient, never()).createPullRequestComment(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any());
    }

    private PullRequestChangedFilesCommentService service() {
        return new PullRequestChangedFilesCommentService(gitHubApiClient);
    }

    private PullRequestFile file(String filename, String previousFilename, String status) {
        return new PullRequestFile(filename, previousFilename, status, null);
    }
}
