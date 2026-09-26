package com.codereviewer.domain.service.rules;

import com.codereviewer.domain.models.DiffLine;
import com.codereviewer.domain.models.Finding;
import com.codereviewer.domain.models.Severity;
import com.codereviewer.domain.service.AbstractLineRule;

import java.util.regex.Pattern;

/**
 * Flags lines longer than 120 characters.
 */
public class LineLengthRule extends AbstractLineRule {

    static final int MAX_LENGTH = 120;

    @Override
    protected String ruleId() {
        return "line-length";
    }

    @Override
    public String name() {
        return "Line length check";
    }

    @Override
    protected Finding checkLine(String filename, DiffLine line) {
        if (line.content().length() > MAX_LENGTH) {
            return finding(filename, line, Severity.MINOR,
                    "Line exceeds " + MAX_LENGTH + " characters. Break it into smaller statements.");
        }
        return null;
    }
}
