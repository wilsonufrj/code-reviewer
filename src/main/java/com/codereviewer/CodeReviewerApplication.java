package com.codereviewer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CodeReviewerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeReviewerApplication.class, args);
    }
}
