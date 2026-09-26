package com.codereviewer.application.config;

import com.codereviewer.domain.service.DiffParser;
import com.codereviewer.domain.service.ReviewEngine;
import com.codereviewer.domain.service.Rule;
import com.codereviewer.domain.service.rules.DebugPrintRule;
import com.codereviewer.domain.service.rules.LineLengthRule;
import com.codereviewer.domain.service.rules.TabIndentationRule;
import com.codereviewer.domain.service.rules.TodoRule;
import com.codereviewer.domain.service.rules.TrailingWhitespaceRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Registers the domain rules and engine as beans so the domain itself stays
 * framework-free.
 */
@Configuration
public class DomainConfig {

    @Bean
    public DiffParser diffParser() {
        return new DiffParser();
    }

    @Bean
    public ReviewEngine reviewEngine(List<Rule> rules) {
        return new ReviewEngine(rules);
    }

    @Bean
    public Rule tabIndentationRule() {
        return new TabIndentationRule();
    }

    @Bean
    public Rule debugPrintRule() {
        return new DebugPrintRule();
    }

    @Bean
    public Rule todoRule() {
        return new TodoRule();
    }

    @Bean
    public Rule trailingWhitespaceRule() {
        return new TrailingWhitespaceRule();
    }

    @Bean
    public Rule lineLengthRule() {
        return new LineLengthRule();
    }
}
