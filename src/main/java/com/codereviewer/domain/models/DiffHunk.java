package com.codereviewer.domain.models;

import java.util.List;

/**
 * A hunk of a unified diff, delimited by a {@code @@ ... @@} header.
 *
 * @param oldStart  1-based start line in the old file (0 when the file is new)
 * @param oldCount  number of lines on the left side
 * @param newStart  1-based start line in the new file (0 when the file is deleted)
 * @param newCount  number of lines on the right side
 * @param lines     parsed lines of the hunk, in diff order
 */
public record DiffHunk(int oldStart, int oldCount, int newStart, int newCount,
                       List<DiffLine> lines) {
}
