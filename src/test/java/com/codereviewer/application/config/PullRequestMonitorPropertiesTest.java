package com.codereviewer.application.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PullRequestMonitorPropertiesTest {

    @Test
    void disabledMonitorShouldNotRequireRepositoryConfiguration() {
        PullRequestMonitorProperties properties = new PullRequestMonitorProperties(
                false, null, null, 60_000);

        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

    @Test
    void enabledMonitorShouldRequireOwner() {
        PullRequestMonitorProperties properties = new PullRequestMonitorProperties(
                true, " ", "repo", 60_000);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("owner must be configured");
    }

    @Test
    void enabledMonitorShouldRequireRepository() {
        PullRequestMonitorProperties properties = new PullRequestMonitorProperties(
                true, "owner", null, 60_000);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("repository must be configured");
    }
}
