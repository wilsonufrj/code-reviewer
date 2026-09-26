package com.codereviewer.domain.service;

import com.codereviewer.domain.models.DiffFile;
import com.codereviewer.domain.models.DiffHunk;
import com.codereviewer.domain.models.DiffLine;
import com.codereviewer.domain.models.DiffSide;
import com.codereviewer.domain.models.Finding;
import com.codereviewer.domain.models.Severity;
import com.codereviewer.domain.service.rules.DebugPrintRule;
import com.codereviewer.domain.service.rules.LineLengthRule;
import com.codereviewer.domain.service.rules.TabIndentationRule;
import com.codereviewer.domain.service.rules.TodoRule;
import com.codereviewer.domain.service.rules.TrailingWhitespaceRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RulesTest {

    @Test
    void tabIndentationRuleShouldFlagTabs() {
        TabIndentationRule rule = new TabIndentationRule();
        DiffFile file = fileWithAddedLines("\tint x = 1;", "    int y = 2;");

        List<Finding> findings = rule.check(file);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).rule()).isEqualTo("tab-indentation");
        assertThat(findings.get(0).line()).isEqualTo(1);
        assertThat(findings.get(0).severity()).isEqualTo(Severity.MINOR);
    }

    @Test
    void debugPrintRuleShouldFlagSystemOut() {
        DebugPrintRule rule = new DebugPrintRule();
        DiffFile file = fileWithAddedLines("System.out.println(\"debug\");", "log.info(\"ok\");");

        List<Finding> findings = rule.check(file);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).rule()).isEqualTo("debug-print");
        assertThat(findings.get(0).severity()).isEqualTo(Severity.MAJOR);
    }

    @Test
    void todoRuleShouldFlagTodoAndFixme() {
        TodoRule rule = new TodoRule();
        DiffFile file = fileWithAddedLines("// TODO fix this", "// FIXME later", "// normal comment");

        List<Finding> findings = rule.check(file);

        assertThat(findings).hasSize(2);
        assertThat(findings).extracting(Finding::line).containsExactly(1, 2);
    }

    @Test
    void trailingWhitespaceRuleShouldFlagTrailingSpaces() {
        TrailingWhitespaceRule rule = new TrailingWhitespaceRule();
        DiffFile file = fileWithAddedLines("int x = 1;  ", "int y = 2;");

        List<Finding> findings = rule.check(file);

        assertThat(findings).hasSize(1);
        assertThat(findings).extracting(Finding::rule).containsExactly("trailing-whitespace");
    }

    @Test
    void lineLengthRuleShouldFlagLongLines() {
        LineLengthRule rule = new LineLengthRule();
        String longLine = "a".repeat(121);
        DiffFile file = fileWithAddedLines(longLine, "b".repeat(120));

        List<Finding> findings = rule.check(file);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).line()).isEqualTo(1);
    }

    @Test
    void rulesShouldIgnoreDeletedLines() {
        TabIndentationRule rule = new TabIndentationRule();
        DiffFile file = fileWithDeletedLines("\tdeleted line");

        assertThat(rule.check(file)).isEmpty();
    }

    private DiffFile fileWithAddedLines(String... contents) {
        List<DiffLine> lines = new java.util.ArrayList<>();
        int line = 1;
        for (String content : contents) {
            lines.add(new DiffLine(DiffSide.RIGHT, null, line++, content, true, false));
        }
        return new DiffFile("Test.java", null, "modified", List.of(new DiffHunk(1, 0, 1, lines.size(), lines)));
    }

    private DiffFile fileWithDeletedLines(String... contents) {
        List<DiffLine> lines = new java.util.ArrayList<>();
        int line = 1;
        for (String content : contents) diffLine(lines, line++, content);
        return new DiffFile("Test.java", null, "modified", List.of(new DiffHunk(1, lines.size(), 0, 0, lines)));
    }

    private void diffLine(List<DiffLine> lines, int line, String content) {
        lines.add(new DiffLine(DiffSide.LEFT, line, null, content, false, true));
    }
}
