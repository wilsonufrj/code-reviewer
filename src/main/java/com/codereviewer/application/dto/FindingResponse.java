package com.codereviewer.application.dto;

import com.codereviewer.domain.models.ReviewFinding;
import com.codereviewer.domain.models.Severity;

/**
 * Response DTO for a single finding.
 */
public record FindingResponse(
        Long id,
        String rule,
        Severity severity,
        String file,
        Integer line,
        String message
) {
    public static FindingResponse from(ReviewFinding finding) {
        return new FindingResponse(finding.getId(), finding.getRule(), finding.getSeverity(),
                finding.getFile(), finding.getLine(), finding.getMessage());
    }
}
