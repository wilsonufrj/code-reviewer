package com.codereviewer.domain.service.rules;

import com.codereviewer.domain.models.DiffLine;
import com.codereviewer.domain.models.Finding;
import com.codereviewer.domain.models.Severity;
import com.codereviewer.domain.service.AbstractLineRule;

import java.util.regex.Pattern;

/**
 * Flags trailing whitespace at the end of added lines.
 */
public class TrailingWhitespaceRule extends AbstractLineRule {

    private static final Pattern TRAILING_WHITESPACE = Pattern.compile("[ \\t]+$");

    @Override
    protected String ruleId() {
        return "trailing-whitespace";
    }

    @Override
    public String name() {
        return "Trailing whitespace check";
    }

    @Override
    protected Finding checkLine(String filename, DiffLine line) {
        if (TRAILING_WHITESPACE.matcher(line.content()).find()) {
            return finding(filename, line, Severity.MINOR,
                    "Trailing whitespace. Remove the extra spaces or tabs at the end of the line.");
        }
        return null;
    }
}
