package com.codereviewer.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for polling one GitHub repository for newly opened pull requests. */
@ConfigurationProperties(prefix = "github.pull-request-monitor")
public record PullRequestMonitorProperties(
        boolean enabled,
        String owner,
        String repository,
        long intervalMs
) {
    public void validate() {
        if (!enabled) {
            return;
        }
        if (owner == null || owner.isBlank()) {
            throw new IllegalStateException(
                    "github.pull-request-monitor.owner must be configured when monitoring is enabled");
        }
        if (repository == null || repository.isBlank()) {
            throw new IllegalStateException(
                    "github.pull-request-monitor.repository must be configured when monitoring is enabled");
        }
    }
}
