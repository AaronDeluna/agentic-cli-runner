package io.github.ivanmilovanov.agentic.cli.runner.runner;

import io.github.ivanmilovanov.agentic.cli.runner.cli.CommandFactory;
import io.github.ivanmilovanov.agentic.cli.runner.executor.CommandExecutor;
import io.github.ivanmilovanov.agentic.cli.runner.log.RunnerLogWriter;
import io.github.ivanmilovanov.agentic.cli.runner.model.CommandResultDto;
import io.github.ivanmilovanov.agentic.cli.runner.parser.AgentStreamJsonParser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRunnerImplSandboxTests {

    private final CommandFactory commandFactory = prompt -> List.of("qwen", prompt);

    @Test
    void sandboxRunsInTmpCopyAndDeletesItAfter(@TempDir Path base) throws Exception {
        Path workspace = base.resolve("run-1/sorce");
        Files.createDirectories(workspace.resolve(".qwen/skills"));
        Files.writeString(workspace.resolve("pom.xml"), "<project/>");
        Files.writeString(workspace.resolve(".qwen/skills/s.md"), "skill");

        AtomicReference<Path> execDirSeen = new AtomicReference<>();
        AtomicReference<Boolean> copySeen = new AtomicReference<>(false);
        CommandExecutor executor = request -> {
            Path wd = request.getWorkingDirectory();
            execDirSeen.set(wd);
            // во время выполнения агент должен сидеть в копии, где лежат файлы проекта и скилы
            copySeen.set(Files.exists(wd.resolve("pom.xml")) && Files.exists(wd.resolve(".qwen/skills/s.md")));
            return new CommandResultDto("", "", 0, false);
        };

        AgentRunnerImpl runner = new AgentRunnerImpl(
                executor, new AgentStreamJsonParser(), new RunnerLogWriter(),
                workspace, Duration.ofMinutes(1), commandFactory, true);

        runner.execute("сделай что-нибудь");

        // агент работал в sorce/tmp
        assertThat(execDirSeen.get()).isEqualTo(workspace.resolve("tmp"));
        assertThat(copySeen.get()).isTrue();
        // после запуска копия удалена, а оригинал цел
        assertThat(workspace.resolve("tmp")).doesNotExist();
        assertThat(workspace.resolve("pom.xml")).isRegularFile();
        // лог запуска сохранён рядом (logs — сосед sorce)
        assertThat(workspace.getParent().resolve("logs/log.json")).isRegularFile();
    }

    @Test
    void withoutSandboxRunsInWorkspaceDirectly(@TempDir Path base) throws Exception {
        Path workspace = base.resolve("run-2/sorce");
        Files.createDirectories(workspace);
        Files.writeString(workspace.resolve("pom.xml"), "<project/>");

        AtomicReference<Path> execDirSeen = new AtomicReference<>();
        CommandExecutor executor = request -> {
            execDirSeen.set(request.getWorkingDirectory());
            return new CommandResultDto("", "", 0, false);
        };

        AgentRunnerImpl runner = new AgentRunnerImpl(
                executor, new AgentStreamJsonParser(), new RunnerLogWriter(),
                workspace, Duration.ofMinutes(1), commandFactory, false);

        runner.execute("prompt");

        assertThat(execDirSeen.get()).isEqualTo(workspace);
        assertThat(workspace.resolve("tmp")).doesNotExist();
    }
}
