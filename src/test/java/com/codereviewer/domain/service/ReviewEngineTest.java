package com.codereviewer.domain.service;

import com.codereviewer.domain.models.DiffFile;
import com.codereviewer.domain.models.DiffHunk;
import com.codereviewer.domain.models.DiffLine;
import com.codereviewer.domain.models.DiffSide;
import com.codereviewer.domain.models.Finding;
import com.codereviewer.domain.models.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewEngineTest {

    @Test
    void shouldAggregateFindingsFromAllRules() {
        Rule alwaysA = stubRule("rule-a", (file) -> List.of(new Finding("rule-a", Severity.MINOR, file.filename(), 2, "a")));
        Rule alwaysB = stubRule("rule-b", (file) -> List.of(new Finding("rule-b", Severity.MAJOR, file.filename(), 1, "b")));
        ReviewEngine engine = new ReviewEngine(List.of(alwaysA, alwaysB));

        List<Finding> findings = engine.analyze(List.of(fileNamed("x.java")));

        assertThat(findings).hasSize(2);
        assertThat(findings).extracting(Finding::rule).containsExactlyInAnyOrder("rule-a", "rule-b");
    }

    @Test
    void shouldSortFindingsByFileThenLine() {
        Rule rule = stubRule("rule", (file) -> List.of(
                new Finding("rule", Severity.MINOR, "b.txt", 1, "b1"),
                new Finding("rule", Severity.MINOR, "a.txt", 9, "a9"),
                new Finding("rule", Severity.MINOR, "a.txt", 2, "a2")));
        ReviewEngine engine = new ReviewEngine(List.of(rule));

        List<Finding> findings = engine.analyze(List.of(fileNamed("ignored")));

        assertThat(findings).extracting(Finding::file).containsExactly("a.txt", "a.txt", "b.txt");
        assertThat(findings).extracting(Finding::line).containsExactly(2, 9, 1);
    }

    @Test
    void failingRuleShouldNotAbortTheReview() {
        Rule failing = stubRule("failing", (file) -> {
            throw new IllegalStateException("boom");
        });
        Rule working = stubRule("working", (file) -> List.of(new Finding("working", Severity.INFO, file.filename(), 1, "ok")));
        ReviewEngine engine = new ReviewEngine(List.of(failing, working));

        List<Finding> findings = engine.analyze(List.of(fileNamed("x.java")));

        assertThat(findings).hasSize(2);
        assertThat(findings).extracting(Finding::rule).containsExactlyInAnyOrder("failing", "working");
    }

    private Rule stubRule(String id, java.util.function.Function<DiffFile, List<Finding>> behavior) {
        return new Rule() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String name() {
                return id;
            }

            @Override
            public List<Finding> check(DiffFile file) {
                return behavior.apply(file);
            }
        };
    }

    private DiffFile fileNamed(String name) {
        DiffLine line = new DiffLine(DiffSide.RIGHT, null, 1, "content", true, false);
        return new DiffFile(name, null, "modified", List.of(new DiffHunk(1, 0, 1, 1, List.of(line))));
    }
}
