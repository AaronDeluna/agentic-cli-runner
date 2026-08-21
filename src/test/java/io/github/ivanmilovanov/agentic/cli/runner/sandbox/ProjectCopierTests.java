package io.github.ivanmilovanov.agentic.cli.runner.sandbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectCopierTests {

    @Test
    void copiesFilesAndNestedDirectories(@TempDir Path src, @TempDir Path dest) throws IOException {
        Files.writeString(src.resolve("pom.xml"), "<project/>");
        Files.createDirectories(src.resolve("app/sub"));
        Files.writeString(src.resolve("app/sub/Main.java"), "class Main {}");

        ProjectCopier.copy(src, dest.resolve("copy"), Set.of(), ".qwen");

        Path copy = dest.resolve("copy");
        assertThat(Files.readString(copy.resolve("pom.xml"))).isEqualTo("<project/>");
        assertThat(Files.readString(copy.resolve("app/sub/Main.java"))).isEqualTo("class Main {}");
    }

    @Test
    void skipsExcludedDirectories(@TempDir Path src, @TempDir Path dest) throws IOException {
        Files.writeString(src.resolve("keep.txt"), "keep");
        Files.createDirectories(src.resolve(".git"));
        Files.writeString(src.resolve(".git/HEAD"), "ref");
        Files.createDirectories(src.resolve("node_modules/lib"));
        Files.writeString(src.resolve("node_modules/lib/index.js"), "x");

        ProjectCopier.copy(src, dest.resolve("copy"), Set.of(".git", "node_modules"), ".qwen");

        Path copy = dest.resolve("copy");
        assertThat(Files.exists(copy.resolve("keep.txt"))).isTrue();
        assertThat(Files.exists(copy.resolve(".git"))).isFalse();
        assertThat(Files.exists(copy.resolve("node_modules"))).isFalse();
    }

    @Test
    void neverExcludesCliConfigDirectoryEvenIfListed(@TempDir Path src, @TempDir Path dest) throws IOException {
        Files.createDirectories(src.resolve(".qwen"));
        Files.writeString(src.resolve(".qwen/settings.json"), "{}");

        // .qwen намеренно добавлен в исключения — копировщик обязан его сохранить.
        ProjectCopier.copy(src, dest.resolve("copy"), Set.of(".qwen", ".git"), ".qwen");

        assertThat(Files.readString(dest.resolve("copy/.qwen/settings.json"))).isEqualTo("{}");
    }
}
