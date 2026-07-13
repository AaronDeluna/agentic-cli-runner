package io.github.ivanmilovanov.agentic.cli.runner.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AgentRunContextTests {

    @Test
    void workspaceIsExactlyThePassedDirectory(@TempDir Path workspace) {
        AgentRunContext context = new AgentRunContext(workspace);

        assertThat(context.getWorkspace()).isEqualTo(workspace);
    }

    @Test
    void runIdIsAFreshlyGeneratedUuid(@TempDir Path workspace) {
        AgentRunContext context = new AgentRunContext(workspace);

        assertThat(context.getRunId()).isNotBlank();
        assertThatCode(() -> UUID.fromString(context.getRunId())).doesNotThrowAnyException();
    }

    @Test
    void eachContextGetsItsOwnRunId(@TempDir Path workspace) {
        AgentRunContext first = new AgentRunContext(workspace);
        AgentRunContext second = new AgentRunContext(workspace);

        assertThat(first.getRunId()).isNotEqualTo(second.getRunId());
    }

    @Test
    void logFileLivesUnderBuildDirInAgenticCliRunnerNamedByRunId(@TempDir Path workspace) {
        AgentRunContext context = new AgentRunContext(workspace);

        Path logFile = context.getLogFile();
        // Тесты SDK гоняются Maven-ом, cwd содержит pom.xml → buildDir = target.
        assertThat(logFile.getFileName().toString()).isEqualTo(context.getRunId() + ".json");
        assertThat(logFile.getParent().getFileName().toString()).isEqualTo("agentic-cli-runner");
        assertThat(logFile.getParent().getParent().getFileName().toString()).isEqualTo("target");
    }
}
