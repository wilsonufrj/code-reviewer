package com.codereviewer.domain.service.rules;

import com.codereviewer.domain.models.DiffLine;
import com.codereviewer.domain.models.Finding;
import com.codereviewer.domain.models.Severity;
import com.codereviewer.domain.service.AbstractLineRule;

/**
 * Flags lines indented with tabs instead of spaces.
 */
public class TabIndentationRule extends AbstractLineRule {

    @Override
    protected String ruleId() {
        return "tab-indentation";
    }

    @Override
    public String name() {
        return "Tab indentation check";
    }

    @Override
    protected Finding checkLine(String filename, DiffLine line) {
        if (line.content().startsWith("\t")) {
            return finding(filename, line, Severity.MINOR,
                    "Line is indented with a tab. Use spaces for indentation.");
        }
        return null;
    }
}
