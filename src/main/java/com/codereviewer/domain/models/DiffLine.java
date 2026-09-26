package com.codereviewer.domain.models;

/**
 * A single line of a unified diff.
 *
 * @param side      which side of the diff the line belongs to
 * @param oldLine   1-based line number in the old file, {@code null} for additions
 * @param newLine   1-based line number in the new file, {@code null} for deletions
 * @param content   the line content without the leading +/-/space marker
 * @param added     true when this line is an addition
 * @param deleted   true when this line is a deletion
 */
public record DiffLine(DiffSide side, Integer oldLine, Integer newLine, String content,
                       boolean added, boolean deleted) {

    public boolean isContext() {
        return !added && !deleted;
    }
}
