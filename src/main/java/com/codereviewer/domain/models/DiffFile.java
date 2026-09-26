package com.codereviewer.domain.models;

import java.util.List;

/**
 * A parsed file from a pull request diff.
 *
 * @param filename  path of the file in the new version of the repository
 * @param previousFilename  original path when the file was renamed, otherwise null
 * @param status    change status as reported by GitHub (added, modified, removed, renamed...)
 * @param hunks     parsed hunks; empty for binary files or pure renames with no content change
 */
public record DiffFile(String filename, String previousFilename, String status,
                       List<DiffHunk> hunks) {

    public boolean isRename() {
        return previousFilename != null && !previousFilename.isBlank();
    }

    /** All added lines of the file, in order. */
    public List<DiffLine> addedLines() {
        return hunks.stream()
                .flatMap(h -> h.lines().stream())
                .filter(DiffLine::added)
                .toList();
    }
}
