package io.github.ivanmilovanov.agentic.cli.runner.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

import java.util.List;

/**
 * Полный результат выполнения агентского запуска: сырой вывод процесса и разобранные события.
 */
@Getter
public class AgentResultDto {

    private final String stdout;
    private final String stderr;
    private final int exitCode;
    private final boolean timedOut;
    private final List<JsonNode> events;
    private final String eventsJson;
    private final String finalResult;

    /**
     * Создаёт результат без разобранных событий stream-json.
     */
    public AgentResultDto(String stdout, String stderr, int exitCode, boolean timedOut) {
        this(stdout, stderr, exitCode, timedOut, List.of(), "[]", null);
    }

    public AgentResultDto(
            String stdout,
            String stderr,
            int exitCode,
            boolean timedOut,
            List<JsonNode> events,
            String eventsJson,
            String finalResult
    ) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.exitCode = exitCode;
        this.timedOut = timedOut;
        this.events = events;
        this.eventsJson = eventsJson;
        this.finalResult = finalResult;
    }
}
