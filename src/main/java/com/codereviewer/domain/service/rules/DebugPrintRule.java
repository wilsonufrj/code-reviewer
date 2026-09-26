package com.codereviewer.domain.service.rules;

import com.codereviewer.domain.models.DiffLine;
import com.codereviewer.domain.models.Finding;
import com.codereviewer.domain.models.Severity;
import com.codereviewer.domain.service.AbstractLineRule;

import java.util.regex.Pattern;

/**
 * Flags leftover debugging print statements (System.out / System.err / printStackTrace)
 * in added lines.
 */
public class DebugPrintRule extends AbstractLineRule {

    private static final Pattern DEBUG_PRINT = Pattern.compile(
            "System\\.(out|err)\\.(print|println|printf)\\s*\\(|printStackTrace\\s*\\(");

    @Override
    protected String ruleId() {
        return "debug-print";
    }

    @Override
    public String name() {
        return "Debug print statement check";
    }

    @Override
    protected Finding checkLine(String filename, DiffLine line) {
        if (DEBUG_PRINT.matcher(line.content()).find()) {
            return finding(filename, line, Severity.MAJOR,
                    "Debug print statement found. Remove it or replace it with a logger.");
        }
        return null;
    }
}
