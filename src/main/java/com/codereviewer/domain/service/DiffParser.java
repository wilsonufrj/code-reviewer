package com.codereviewer.domain.service;

import com.codereviewer.domain.models.DiffFile;
import com.codereviewer.domain.models.DiffHunk;
import com.codereviewer.domain.models.DiffLine;
import com.codereviewer.domain.models.DiffSide;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses unified diff patches (the {@code patch} field of the GitHub PR files response)
 * into the neutral {@link DiffFile} model.
 *
 * <p>Line-number mapping rules (the tricky part of this codebase):
 * <ul>
 *   <li>GitHub review comments address blob lines via {@code line} + {@code side=RIGHT},
 *       not diff positions.</li>
 *   <li>For additions, the new-file line number is what must be reported.</li>
 *   <li>Binary files and pure renames have no patch; {@link #parse} returns
 *       {@link Optional#empty()} for those.</li>
 * </ul>
 */
public class DiffParser {

    private static final Pattern HUNK_HEADER = Pattern.compile("^@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@.*");

    /**
     * Parses a patch for a single file.
     *
     * @param filename          path of the file in the new version
     * @param previousFilename  original path when renamed, otherwise null
     * @param status            change status reported by GitHub
     * @param patch             the unified diff patch, may be null for binary files/renames
     * @return the parsed file, or empty when there is no patch to analyze
     */
    public Optional<DiffFile> parse(String filename, String previousFilename, String status,
                                    String patch) {
        if (patch == null || patch.isBlank()) {
            return Optional.empty();
        }

        List<DiffHunk> hunks = new ArrayList<>();
        List<DiffLine> lines = null;
        int oldStart = 0;
        int oldCount = 0;
        int newStart = 0;
        int newCount = 0;
        int oldLine = 0;
        int newLine = 0;

        // split() without limit drops trailing empty strings, so the newline that
        // terminates the patch does not become a phantom context line.
        for (String raw : patch.split("\\R")) {
            if (raw.startsWith("@@")) {
                if (lines != null) {
                    hunks.add(new DiffHunk(oldStart, oldCount, newStart, newCount, lines));
                }
                Matcher m = HUNK_HEADER.matcher(raw);
                if (!m.find()) {
                    throw new IllegalArgumentException("Malformed hunk header: " + raw);
                }
                oldStart = Integer.parseInt(m.group(1));
                oldCount = m.group(2) != null ? Integer.parseInt(m.group(2)) : 1;
                newStart = m.group(3) != null ? Integer.parseInt(m.group(3)) : 1;
                newCount = m.group(4) != null ? Integer.parseInt(m.group(4)) : 1;
                oldLine = oldStart;
                newLine = newStart;
                lines = new ArrayList<>();
            } else if (lines != null) {
                if (raw.isEmpty()) {
                    // An empty diff line is context for an empty content line.
                    lines.add(new DiffLine(DiffSide.RIGHT, oldLine, newLine, "", false, false));
                    oldLine++;
                    newLine++;
                    continue;
                }
                char marker = raw.charAt(0);
                String content = raw.length() > 1 ? raw.substring(1) : "";
                switch (marker) {
                    case '+' -> {
                        lines.add(new DiffLine(DiffSide.RIGHT, null, newLine, content, true, false));
                        newLine++;
                    }
                    case '-' -> {
                        lines.add(new DiffLine(DiffSide.LEFT, oldLine, null, content, false, true));
                        oldLine++;
                    }
                    case '\\' -> {
                        // "\ No newline at end of file" — metadata, not a content line.
                    }
                    default -> {
                        lines.add(new DiffLine(DiffSide.RIGHT, oldLine, newLine, content, false, false));
                        oldLine++;
                        newLine++;
                    }
                }
            }
        }
        if (lines != null) {
            hunks.add(new DiffHunk(oldStart, oldCount, newStart, newCount, lines));
        }

        return Optional.of(new DiffFile(filename, previousFilename, status, hunks));
    }
}
