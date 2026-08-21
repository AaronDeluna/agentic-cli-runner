package io.github.ivanmilovanov.agentic.cli.runner.sandbox;

import io.github.ivanmilovanov.agentic.cli.runner.config.OsType;
import io.github.ivanmilovanov.agentic.cli.runner.context.AgentRunContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CopyingSandboxTests {

    private static CopyingSandbox sandbox(boolean osEnforcement) {
        return new CopyingSandbox("qwen", List.of(".git"), osEnforcement, OsType.detect());
    }

    @Test
    void prepareCopiesProjectIntoTempDir(@TempDir Path workspace) throws IOException {
        Files.writeString(workspace.resolve("pom.xml"), "<project/>");
        Files.createDirectories(workspace.resolve(".git"));
        Files.writeString(workspace.resolve(".git/HEAD"), "ref");

        CopyingSandbox sandbox = sandbox(false);
        AgentRunContext context = new AgentRunContext(workspace);

        Path runDir = sandbox.prepare(context);
        try {
            assertThat(runDir.getFileName().toString()).contains(context.getRunId());
            assertThat(Files.readString(runDir.resolve("pom.xml"))).isEqualTo("<project/>");
            assertThat(Files.exists(runDir.resolve(".git"))).isFalse();
        } finally {
            sandbox.cleanup(runDir);
        }
    }

    @Test
    void cleanupDeletesTempDir(@TempDir Path workspace) throws IOException {
        Files.writeString(workspace.resolve("a.txt"), "a");

        CopyingSandbox sandbox = sandbox(false);
        AgentRunContext context = new AgentRunContext(workspace);
        Path runDir = sandbox.prepare(context);
        assertThat(Files.exists(runDir)).isTrue();

        sandbox.cleanup(runDir);

        assertThat(Files.exists(runDir)).isFalse();
    }

    @Test
    void summarizeChangesReportsAgentFileChanges(@TempDir Path workspace) throws IOException {
        Files.writeString(workspace.resolve("keep.txt"), "same\n");

        CopyingSandbox sandbox = sandbox(false);
        AgentRunContext context = new AgentRunContext(workspace);
        Path runDir = sandbox.prepare(context);
        try {
            // эмулируем работу агента в копии: создаём новый файл
            Files.writeString(runDir.resolve("created.txt"), "hello\n");

            var changes = sandbox.summarizeChanges(context, runDir);

            assertThat(changes).hasSize(1);
            assertThat(changes.get(0).getPath()).isEqualTo("created.txt");
            assertThat(changes.get(0).getChangeType()).isEqualTo("added");
            assertThat(changes.get(0).getDiff()).contains("+hello");
        } finally {
            sandbox.cleanup(runDir);
        }
    }

    @Test
    void summarizeChangesIncludesFilesInsideCliConfig(@TempDir Path workspace) throws IOException {
        // .qwen в копии = проектная, значит изменения агента внутри неё — настоящие, их надо показывать.
        Files.createDirectories(workspace.resolve(".qwen/skills"));
        Files.writeString(workspace.resolve(".qwen/skills/SKILL.md"), "skill\n");

        CopyingSandbox sandbox = sandbox(false);
        AgentRunContext context = new AgentRunContext(workspace);
        Path runDir = sandbox.prepare(context);
        try {
            // агент создал файл внутри .qwen
            Files.writeString(runDir.resolve(".qwen/skills/Generated.java"), "class Generated {}\n");

            var changes = sandbox.summarizeChanges(context, runDir);

            assertThat(changes).hasSize(1);
            assertThat(changes.get(0).getPath()).isEqualTo(".qwen/skills/Generated.java");
            assertThat(changes.get(0).getChangeType()).isEqualTo("added");
        } finally {
            sandbox.cleanup(runDir);
        }
    }

    @Test
    void wrapCommandReturnsCommandUnchangedWhenOsEnforcementDisabled(@TempDir Path workspace) {
        CopyingSandbox sandbox = sandbox(false);
        List<String> command = List.of("qwen", "prompt");

        assertThat(sandbox.wrapCommand(command, workspace)).isEqualTo(command);
    }
}
