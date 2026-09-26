package com.codereviewer.application.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Provides the top-level metadata displayed by Swagger UI. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI codeReviewerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Code Reviewer API")
                .version("0.1.0")
                .description("Automated GitHub pull-request reviews and public repository metadata."));
    }
}
