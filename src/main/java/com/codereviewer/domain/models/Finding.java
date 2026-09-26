package com.codereviewer.domain.models;

/**
 * A neutral review finding produced by a rule. This is the currency of the domain:
 * rules produce findings, the engine aggregates them, and adapters translate them
 * to whatever the target forge understands.
 *
 * @param rule      identifier of the rule that produced the finding (e.g. {@code tab-indentation})
 * @param severity  how important the finding is
 * @param file      path of the file in the new version of the repository
 * @param line      1-based line number in the new file
 * @param message   user-facing, actionable message in English
 */
public record Finding(String rule, Severity severity, String file, int line, String message) {
}
