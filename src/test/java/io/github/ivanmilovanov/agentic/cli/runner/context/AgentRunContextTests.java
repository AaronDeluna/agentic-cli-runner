package io.github.ivanmilovanov.agentic.cli.runner.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AgentRunContextTests {

    @Test
    void workspaceIsTheSorceDirectory(@TempDir Path workspace) throws IOException {
        Path runRoot = Files.createDirectory(workspace.resolve(UUID.randomUUID().toString()));
        Path sourceDir = Files.createDirectory(runRoot.resolve("sorce"));

        AgentRunContext context = new AgentRunContext(sourceDir);

        assertThat(context.getWorkspace()).isEqualTo(sourceDir);
        assertThat(context.getWorkspace().getFileName().toString()).isEqualTo("sorce");
    }

    @Test
    void runDirIsTheLogsDirectorySiblingOfSorce(@TempDir Path workspace) throws IOException {
        Path runRoot = Files.createDirectory(workspace.resolve(UUID.randomUUID().toString()));
        Path sourceDir = Files.createDirectory(runRoot.resolve("sorce"));

        AgentRunContext context = new AgentRunContext(sourceDir);

        assertThat(context.getRunDir().getFileName().toString()).isEqualTo("logs");
        assertThat(context.getRunDir().getParent()).isEqualTo(runRoot);
    }

    @Test
    void runIdIsTheParentDirectoryNameAndAValidUuid(@TempDir Path workspace) throws IOException {
        UUID expectedRunId = UUID.randomUUID();
        Path runRoot = Files.createDirectory(workspace.resolve(expectedRunId.toString()));
        Path sourceDir = Files.createDirectory(runRoot.resolve("sorce"));

        AgentRunContext context = new AgentRunContext(sourceDir);

        assertThat(context.getRunId()).isNotNull();
        assertThat(context.getRunId()).isEqualTo(expectedRunId.toString());
        assertThatCode(() -> UUID.fromString(context.getRunId())).doesNotThrowAnyException();
    }
}
