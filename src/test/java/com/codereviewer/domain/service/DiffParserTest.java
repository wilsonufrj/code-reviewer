package com.codereviewer.domain.service;

import com.codereviewer.domain.models.DiffFile;
import com.codereviewer.domain.models.DiffLine;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiffParserTest {

    private final DiffParser parser = new DiffParser();

    @Test
    void shouldReturnEmptyForNullPatch() {
        assertThat(parser.parse("logo.png", null, "added", null)).isEmpty();
    }

    @Test
    void shouldReturnEmptyForBlankPatch() {
        assertThat(parser.parse("logo.png", null, "added", "   ")).isEmpty();
    }

    @Test
    void shouldParseSingleHunkWithAdditionsAndDeletions() {
        String patch = """
                @@ -1,4 +1,4 @@
                 first
                -second
                +SECOND
                 third
                +fourth
                """;

        DiffFile file = parser.parse("a.txt", null, "modified", patch).orElseThrow();

        assertThat(file.hunks()).hasSize(1);
        DiffLine second = lineAt(file, 2);
        assertThat(second.added()).isTrue();
        assertThat(second.deleted()).isFalse();
        assertThat(second.newLine()).isEqualTo(2);
        assertThat(second.content()).isEqualTo("SECOND");

        DiffLine fourth = lineAt(file, 4);
        assertThat(fourth.added()).isTrue();
        assertThat(fourth.newLine()).isEqualTo(4);
        assertThat(fourth.content()).isEqualTo("fourth");
    }

    @Test
    void shouldParseMultipleHunks() {
        String patch = """
                @@ -1,2 +1,2 @@
                -a
                +b
                @@ -10,2 +10,2 @@
                -c
                +d
                """;

        DiffFile file = parser.parse("a.txt", null, "modified", patch).orElseThrow();

        assertThat(file.hunks()).hasSize(2);
        assertThat(file.hunks().get(1).newStart()).isEqualTo(10);
        assertThat(file.addedLines()).hasSize(2);
    }

    @Test
    void shouldTrackOldLineNumbersForDeletions() {
        String patch = """
                @@ -5,3 +5,3 @@
                 context
                -removed
                +added
                 context
                """;

        DiffFile file = parser.parse("a.txt", null, "modified", patch).orElseThrow();

        DiffLine removed = file.hunks().get(0).lines().get(1);
        assertThat(removed.deleted()).isTrue();
        assertThat(removed.oldLine()).isEqualTo(6);
        assertThat(removed.newLine()).isNull();
    }

    @Test
    void shouldSkipNoNewlineMarker() {
        String patch = """
                @@ -1 +1 @@
                -old
                +new
                \\ No newline at end of file
                """;

        DiffFile file = parser.parse("a.txt", null, "modified", patch).orElseThrow();

        assertThat(file.hunks().get(0).lines()).hasSize(2);
    }

    @Test
    void shouldHandleNewFile() {
        String patch = """
                @@ -0,0 +1,2 @@
                +hello
                +world
                """;

        DiffFile file = parser.parse("new.txt", null, "added", patch).orElseThrow();

        assertThat(file.addedLines()).extracting(DiffLine::newLine).containsExactly(1, 2);
    }

    @Test
    void shouldThrowOnMalformedHunkHeader() {
        String patch = """
                @@ not a real header
                +x
                """;

        assertThatThrownBy(() -> parser.parse("a.txt", null, "modified", patch))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private DiffLine lineAt(DiffFile file, int index) {
        return file.hunks().get(0).lines().get(index);
    }
}
