package com.codereviewer.infrastructure.github;

/**
 * Thrown when a call to the GitHub API fails. Callers must degrade gracefully:
 * findings are always persisted before any submission is attempted.
 */
public class GitHubApiException extends RuntimeException {

    private final int statusCode;

    public GitHubApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
