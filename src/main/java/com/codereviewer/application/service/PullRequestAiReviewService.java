package com.codereviewer.application.service;

import com.codereviewer.infrastructure.ai.AiApiClient;
import com.codereviewer.infrastructure.ai.AiProperties;
import com.codereviewer.infrastructure.github.GitHubApiClient;
import com.codereviewer.infrastructure.github.GitHubDtos.PullRequestCommentRequest;
import com.codereviewer.infrastructure.github.GitHubDtos.PullRequestFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Sends pull-request patches to AI and posts the resulting review to GitHub. */
@Service
@RequiredArgsConstructor
public class PullRequestAiReviewService {

    private static final int COMMENT_CONTENT_LIMIT = 59_800;
    private static final String REVIEW_INSTRUCTION = """
            Review the following pull request changes. Respond in concise Markdown.
            Focus on correctness, security, and maintainability. Provide actionable findings,
            citing the file and changed line when possible. If there are no material issues,
            say so clearly. Return only the review intended for the pull request comment.
            """;

    private final GitHubApiClient gitHubApiClient;
    private final AiApiClient aiApiClient;
    private final AiProperties aiProperties;
    private final Map<ReviewKey, ReviewProgress> progressByPullRequest = new HashMap<>();

    /** Reviews and comments on a PR, resuming any incomplete work held in memory. */
    public synchronized void reviewAndComment(String owner, String repository,
                                              int pullRequestNumber) {

        ReviewKey key = new ReviewKey(owner, repository, pullRequestNumber);
        ReviewProgress progress = progressByPullRequest.get(key);

        if (progress == null) {
            List<PullRequestFile> files = gitHubApiClient.getPullRequestFiles(
                    owner, repository, pullRequestNumber);


            progress = createProgress(files);
            progressByPullRequest.put(key, progress);
        }

        completeAiReviews(progress);
        prepareComments(progress);
        postRemainingComments(owner, repository, pullRequestNumber, progress);
        progressByPullRequest.remove(key);
    }

    private ReviewProgress createProgress(List<PullRequestFile> files) {
        if (files == null || files.isEmpty()) {
            ReviewProgress progress = new ReviewProgress(List.of());
            progress.comments = List.of("""
                    ## AI code review

                    No diff was available for AI analysis.

                    ## Changed files

                    No changed files were returned by GitHub.
                    """);
            return progress;
        }
        return new ReviewProgress(buildPromptChunks(files));
    }

    private void completeAiReviews(ReviewProgress progress) {
        while (progress.responses.size() < progress.promptChunks.size()) {
            PromptChunk chunk = progress.promptChunks.get(progress.responses.size());
            progress.responses.add(aiApiClient.complete(chunk.prompt()));
        }
    }

    private void prepareComments(ReviewProgress progress) {
        if (progress.comments != null) {
            return;
        }

        List<String> parts = new ArrayList<>();

        for (int index = 0; index < progress.promptChunks.size(); index++) {
            PromptChunk chunk = progress.promptChunks.get(index);
            String body = "## AI code review\n\n"
                    + progress.responses.get(index)
                    + "\n\n## Changed files\n\n"
                    + String.join("\n", chunk.fileReportLines());
            parts.addAll(splitAtLineBoundaries(body, COMMENT_CONTENT_LIMIT));
        }

        if (parts.size() > 1) {
            List<String> numberedParts = new ArrayList<>(parts.size());
            for (int index = 0; index < parts.size(); index++) {
                numberedParts.add("## Automated AI review — part " + (index + 1)
                        + " of " + parts.size() + "\n\n" + parts.get(index));
            }
            progress.comments = List.copyOf(numberedParts);
        } else {
            progress.comments = List.copyOf(parts);
        }
    }

    private void postRemainingComments(String owner, String repository, int pullRequestNumber,
                                       ReviewProgress progress) {
        while (progress.nextComment < progress.comments.size()) {
            gitHubApiClient.createPullRequestComment(
                    owner, repository, pullRequestNumber,
                    new PullRequestCommentRequest(progress.comments.get(progress.nextComment)));
            progress.nextComment++;
        }
    }

    private List<PromptChunk> buildPromptChunks(List<PullRequestFile> files) {
        int maxPromptCharacters = aiProperties.maxPromptCharacters();
        int maxSegmentCharacters = maxPromptCharacters - REVIEW_INSTRUCTION.length() - 2;
        if (maxSegmentCharacters < 256) {
            throw new IllegalStateException(
                    "ai.max-prompt-characters must leave at least 256 characters for file data");
        }

        List<FileSegment> segments = new ArrayList<>();
        files.forEach(file -> segments.addAll(buildFileSegments(file, maxSegmentCharacters)));

        List<PromptChunk> chunks = new ArrayList<>();
        StringBuilder prompt = new StringBuilder(REVIEW_INSTRUCTION);
        Set<String> reportLines = new LinkedHashSet<>();

        for (FileSegment segment : segments) {
            int separatorLength = prompt.length() == REVIEW_INSTRUCTION.length() ? 0 : 2;
            if (prompt.length() + separatorLength + segment.content().length()
                    > maxPromptCharacters) {
                chunks.add(new PromptChunk(prompt.toString(), List.copyOf(reportLines)));
                prompt = new StringBuilder(REVIEW_INSTRUCTION);
                reportLines = new LinkedHashSet<>();
                separatorLength = 0;
            }
            if (separatorLength > 0) {
                prompt.append("\n\n");
            }
            prompt.append(segment.content());
            reportLines.add(segment.reportLine());
        }
        if (prompt.length() > REVIEW_INSTRUCTION.length()) {
            chunks.add(new PromptChunk(prompt.toString(), List.copyOf(reportLines)));
        }
        return List.copyOf(chunks);
    }

    private List<FileSegment> buildFileSegments(PullRequestFile file, int maxCharacters) {
        String metadata = buildMetadata(file);
        String reportLine = buildReportLine(file);
        if (file.patch() == null || file.patch().isBlank()) {
            return List.of(new FileSegment(
                    metadata + "Patch:\n[Patch unavailable]", reportLine));
        }

        String complete = metadata + "Patch:\n" + file.patch();
        if (complete.length() <= maxCharacters) {
            return List.of(new FileSegment(complete, reportLine));
        }

        String segmentPrefix = metadata
                + "Patch continuation segment 0000000000 of 0000000000:\n";
        int patchCapacity = maxCharacters - segmentPrefix.length();
        if (patchCapacity < 1) {
            throw new IllegalStateException(
                    "ai.max-prompt-characters is too small for file metadata: " + file.filename());
        }

        List<String> patchParts = splitAtLineBoundaries(file.patch(), patchCapacity);
        List<FileSegment> segments = new ArrayList<>(patchParts.size());
        for (int index = 0; index < patchParts.size(); index++) {
            int partNumber = index + 1;
            String label = partNumber == 1
                    ? "Patch segment 1 of " + patchParts.size()
                    : "Patch continuation segment " + partNumber + " of " + patchParts.size();
            segments.add(new FileSegment(
                    metadata + label + ":\n" + patchParts.get(index),
                    reportLine + " (patch segment " + partNumber + " of " + patchParts.size() + ")"));
        }
        return segments;
    }

    private String buildMetadata(PullRequestFile file) {
        StringBuilder metadata = new StringBuilder()
                .append("--- FILE START ---\n")
                .append("File: ").append(file.filename()).append("\n")
                .append("Status: ").append(file.status()).append("\n");
        if (file.previousFilename() != null && !file.previousFilename().isBlank()) {
            metadata.append("Previous file: ").append(file.previousFilename()).append("\n");
        }
        return metadata.toString();
    }

    private String buildReportLine(PullRequestFile file) {
        String filename = escapeBackticks(file.filename());
        if ("renamed".equals(file.status()) && file.previousFilename() != null) {
            return "- `" + escapeBackticks(file.previousFilename()) + "` → `" + filename
                    + "` — " + file.status();
        }
        return "- `" + filename + "` — " + file.status();
    }

    private String escapeBackticks(String value) {
        return value.replace("`", "\\`");
    }

    private static List<String> splitAtLineBoundaries(String value, int maximumLength) {
        if (value.length() <= maximumLength) {
            return List.of(value);
        }

        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < value.length()) {
            int end = Math.min(start + maximumLength, value.length());
            if (end < value.length()) {
                int newline = value.lastIndexOf('\n', end - 1);
                if (newline >= start + (maximumLength / 2)) {
                    end = newline + 1;
                }
            }
            parts.add(value.substring(start, end));
            start = end;
        }
        return parts;
    }

    private record ReviewKey(String owner, String repository, int pullRequestNumber) {
    }

    private record FileSegment(String content, String reportLine) {
    }

    private record PromptChunk(String prompt, List<String> fileReportLines) {
    }

    private static final class ReviewProgress {
        private final List<PromptChunk> promptChunks;
        private final List<String> responses = new ArrayList<>();
        private List<String> comments;
        private int nextComment;

        private ReviewProgress(List<PromptChunk> promptChunks) {
            this.promptChunks = promptChunks;
        }
    }
}
