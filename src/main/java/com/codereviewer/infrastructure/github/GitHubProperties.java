package com.codereviewer.infrastructure.github;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GitHub integration settings bound from the {@code github.*} properties.
 */
@ConfigurationProperties(prefix = "github")
public record GitHubProperties(
        String apiBaseUrl,
        String token,
        boolean enabled
) {
}
