package com.codereviewer.domain.service;

import com.codereviewer.domain.models.DiffFile;
import com.codereviewer.domain.models.Finding;
import com.codereviewer.domain.models.Severity;

import java.util.Comparator;
import java.util.List;

/**
 * Runs all registered {@link Rule}s over the parsed diff files and aggregates the findings.
 * Pure domain logic: no I/O, no framework types.
 */
public class ReviewEngine {

    private final List<Rule> rules;

    public ReviewEngine(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    /**
     * Analyzes the given files with every registered rule.
     *
     * @param files parsed diff files (never null; may be empty)
     * @return aggregated findings, ordered by file then line
     */
    public List<Finding> analyze(List<DiffFile> files) {
        return files.stream()
                .flatMap(file -> rules.stream()
                        .flatMap(rule -> safeCheck(rule, file).stream()))
                .sorted(Comparator
                        .comparing(Finding::file)
                        .thenComparing(Finding::line))
                .toList();
    }

    /** A rule that throws must not abort the whole review. */
    private List<Finding> safeCheck(Rule rule, DiffFile file) {
        try {
            return rule.check(file);
        } catch (RuntimeException e) {
            return List.of(new Finding(
                    rule.id(),
                    Severity.INFO,
                    file.filename(),
                    1,
                    "Rule '" + rule.id() + "' failed to analyze this file and was skipped."));
        }
    }
}
