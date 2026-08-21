package io.github.ivanmilovanov.agentic.cli.runner.sandbox;

import io.github.ivanmilovanov.agentic.cli.runner.context.AgentRunContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class NoopSandboxTests {

    @Test
    void prepareReturnsOriginalWorkspace(@TempDir Path workspace) {
        NoopSandbox sandbox = new NoopSandbox();
        AgentRunContext context = new AgentRunContext(workspace);

        assertThat(sandbox.prepare(context)).isEqualTo(workspace);
    }

    @Test
    void wrapCommandReturnsCommandUnchanged(@TempDir Path workspace) {
        NoopSandbox sandbox = new NoopSandbox();
        List<String> command = List.of("qwen", "--output-format", "stream-json", "prompt");

        assertThat(sandbox.wrapCommand(command, workspace)).isEqualTo(command);
    }

    @Test
    void summarizeChangesIsEmptyAndCleanupDoesNothing(@TempDir Path workspace) {
        NoopSandbox sandbox = new NoopSandbox();
        AgentRunContext context = new AgentRunContext(workspace);

        assertThat(sandbox.summarizeChanges(context, workspace)).isEmpty();
        assertThatCode(() -> sandbox.cleanup(workspace)).doesNotThrowAnyException();
    }
}
