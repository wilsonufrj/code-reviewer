package com.codereviewer.application.service;

import com.codereviewer.infrastructure.github.GitHubApiClient;
import com.codereviewer.infrastructure.github.GitHubDtos.PullRequestCommentRequest;
import com.codereviewer.infrastructure.github.GitHubDtos.PullRequestFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Posts one or more PR comments listing every file changed by the pull request. */
@Service
@RequiredArgsConstructor
public class PullRequestChangedFilesCommentService {

    private static final int FILES_PER_COMMENT = 100;

    private final GitHubApiClient gitHubApiClient;

    public void commentOnChangedFiles(String owner, String repository, int pullRequestNumber) {
        List<PullRequestFile> files = gitHubApiClient.getPullRequestFiles(
                owner, repository, pullRequestNumber);
        for (int start = 0; start < files.size(); start += FILES_PER_COMMENT) {
            int end = Math.min(start + FILES_PER_COMMENT, files.size());
            String body = buildComment(files.subList(start, end), start, end, files.size());
            gitHubApiClient.createPullRequestComment(
                    owner, repository, pullRequestNumber, new PullRequestCommentRequest(body));
        }
    }

    private String buildComment(List<PullRequestFile> files, int start, int end, int total) {
        StringBuilder body = new StringBuilder("## Automated changed-files report\n\n")
                .append("Changed files ")
                .append(start + 1)
                .append("-")
                .append(end)
                .append(" of ")
                .append(total)
                .append(":\n\n");
        files.forEach(file -> body.append("- ")
                .append(formatFile(file))
                .append(" — ")
                .append(file.status())
                .append("\n"));
        return body.toString();
    }

    private String formatFile(PullRequestFile file) {
        String filename = escapeBackticks(file.filename());
        if ("renamed".equals(file.status()) && file.previousFilename() != null) {
            return "`" + escapeBackticks(file.previousFilename()) + "` → `" + filename + "`";
        }
        return "`" + filename + "`";
    }

    private String escapeBackticks(String value) {
        return value.replace("`", "\\`");
    }
}
