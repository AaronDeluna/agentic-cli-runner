package io.github.ivanmilovanov.agentic.cli.runner.runner;

import io.github.ivanmilovanov.agentic.cli.runner.config.AgentLogLevel;
import io.github.ivanmilovanov.agentic.cli.runner.executor.CommandExecutor;
import io.github.ivanmilovanov.agentic.cli.runner.log.RunnerLogWriter;
import io.github.ivanmilovanov.agentic.cli.runner.model.AgentResultDto;
import io.github.ivanmilovanov.agentic.cli.runner.model.CommandResultDto;
import io.github.ivanmilovanov.agentic.cli.runner.parser.AgentStreamJsonParser;
import io.github.ivanmilovanov.agentic.cli.runner.sandbox.NoopSandbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRunnerImplTests {

    private static final String STREAM_JSON = """
            {"type":"system","uuid":"u-1","session_id":"s-1","model":"gpt-4.1-mini"}
            {"type":"result","uuid":"u-2","session_id":"s-2","result":"done","usage":{"total_tokens":10}}
            """;

    private AgentResultDto runWithLevel(Path workspace, AgentLogLevel level) throws Exception {
        CommandExecutor executor = request -> new CommandResultDto(STREAM_JSON, "", 0, false);
        AgentRunnerImpl runner = new AgentRunnerImpl(
                executor,
                new AgentStreamJsonParser(),
                new RunnerLogWriter(),
                workspace,
                Duration.ofMinutes(1),
                prompt -> List.of("noop"),
                new NoopSandbox(),
                level
        );
        return runner.execute("do something");
    }

    @Test
    void eventsJsonIsCompactedForJudgeWhenLevelCompact(@TempDir Path workspace) throws Exception {
        AgentResultDto result = runWithLevel(workspace, AgentLogLevel.COMPACT);

        // Именно getEventsJson() уходит потребителю (модели-судье) — при COMPACT из него
        // должны быть вырезаны служебные поля, иначе настройка не экономит токены.
        assertThat(result.getEventsJson())
                .doesNotContain("uuid", "session_id", "usage")
                .contains("\"type\"", "\"result\" : \"done\"");
    }

    @Test
    void eventsJsonIsRawWhenLevelFull(@TempDir Path workspace) throws Exception {
        AgentResultDto result = runWithLevel(workspace, AgentLogLevel.FULL);

        assertThat(result.getEventsJson()).contains("uuid", "session_id");
    }
}
