package io.github.ivanmilovanov.agentic.cli.runner.sandbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SandboxDiffTests {

    @Test
    void emptyDiffWhenNothingChanged(@TempDir Path a, @TempDir Path b) throws IOException {
        Files.writeString(a.resolve("f.txt"), "same\n");
        Files.writeString(b.resolve("f.txt"), "same\n");

        assertThat(SandboxDiff.diff(a, b, Set.of(), ".qwen")).isEmpty();
    }

    @Test
    void reportsModifiedFile(@TempDir Path a, @TempDir Path b) throws IOException {
        Files.writeString(a.resolve("f.txt"), "line1\nline2\nline3\n");
        Files.writeString(b.resolve("f.txt"), "line1\nCHANGED\nline3\n");

        String diff = SandboxDiff.diff(a, b, Set.of(), ".qwen");

        assertThat(diff).contains("diff --git a/f.txt b/f.txt");
        assertThat(diff).contains("-line2");
        assertThat(diff).contains("+CHANGED");
    }

    @Test
    void reportsAddedFile(@TempDir Path a, @TempDir Path b) throws IOException {
        Files.writeString(b.resolve("new.txt"), "hello\n");

        String diff = SandboxDiff.diff(a, b, Set.of(), ".qwen");

        assertThat(diff).contains("new file");
        assertThat(diff).contains("--- /dev/null");
        assertThat(diff).contains("+hello");
    }

    @Test
    void reportsDeletedFile(@TempDir Path a, @TempDir Path b) throws IOException {
        Files.writeString(a.resolve("gone.txt"), "bye\n");

        String diff = SandboxDiff.diff(a, b, Set.of(), ".qwen");

        assertThat(diff).contains("deleted file");
        assertThat(diff).contains("+++ /dev/null");
        assertThat(diff).contains("-bye");
    }

    @Test
    void unifiedHunksProduceStandardHeader() {
        String hunks = SandboxDiff.unifiedHunks(
                List.of("a", "b", "c"),
                List.of("a", "x", "c"));

        assertThat(hunks).contains("@@ -");
        assertThat(hunks).contains("-b");
        assertThat(hunks).contains("+x");
        assertThat(hunks).contains(" a");
        assertThat(hunks).contains(" c");
    }
}
