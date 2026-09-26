package com.codereviewer.domain.service;

import com.codereviewer.domain.models.DiffFile;
import com.codereviewer.domain.models.DiffLine;
import com.codereviewer.domain.models.Finding;
import com.codereviewer.domain.models.Severity;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for rules that inspect only the added lines of a diff.
 * Subclasses implement {@link #checkLine} and never worry about diff traversal.
 */
public abstract class AbstractLineRule implements Rule {

    /** Stable identifier of the rule, used in findings. */
    protected abstract String ruleId();

    @Override
    public String id() {
        return ruleId();
    }

    @Override
    public List<Finding> check(DiffFile file) {
        List<Finding> findings = new ArrayList<>();
        for (DiffLine line : file.addedLines()) {
            Finding finding = checkLine(file.filename(), line);
            if (finding != null) {
                findings.add(finding);
            }
        }
        return findings;
    }

    /**
     * Checks a single added line.
     *
     * @param filename file the line belongs to
     * @param line     the added line
     * @return the finding, or null when the line is fine
     */
    protected abstract Finding checkLine(String filename, DiffLine line);

    /** Builds a finding for this rule. */
    protected Finding finding(String filename, DiffLine line, Severity severity, String message) {
        return new Finding(ruleId(), severity, filename, line.newLine(), message);
    }
}
