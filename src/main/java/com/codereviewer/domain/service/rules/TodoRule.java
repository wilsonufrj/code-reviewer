package com.codereviewer.domain.service.rules;

import com.codereviewer.domain.models.DiffLine;
import com.codereviewer.domain.models.Finding;
import com.codereviewer.domain.models.Severity;
import com.codereviewer.domain.service.AbstractLineRule;

import java.util.regex.Pattern;

/**
 * Flags obvious TODO / FIXME markers left in added lines.
 */
public class TodoRule extends AbstractLineRule {

    private static final Pattern TODO = Pattern.compile("\\b(TODO|FIXME)\\b");

    @Override
    protected String ruleId() {
        return "todo-marker";
    }

    @Override
    public String name() {
        return "TODO/FIXME marker check";
    }

    @Override
    protected Finding checkLine(String filename, DiffLine line) {
        if (TODO.matcher(line.content()).find()) {
            return finding(filename, line, Severity.INFO,
                    "TODO/FIXME marker found. Track it in an issue or resolve it before merging.");
        }
        return null;
    }
}
