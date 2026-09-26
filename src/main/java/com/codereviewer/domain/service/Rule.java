package com.codereviewer.domain.service;

import com.codereviewer.domain.models.DiffFile;
import com.codereviewer.domain.models.Finding;

import java.util.List;

/**
 * A review rule. Implementations must be stateless and safe to call concurrently.
 */
public interface Rule {

    /** Stable identifier used in findings and persisted data (e.g. {@code tab-indentation}). */
    String id();

    /** Human-readable name shown in summaries (e.g. {@code "Tab indentation check"}). */
    String name();

    /** Applies the rule to one parsed diff file and returns the findings it found. */
    List<Finding> check(DiffFile file);
}
