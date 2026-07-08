package io.github.ivanmilovanov.agentic.cli.runner.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Результат разбора stream-json вывода CLI-агента: события и извлечённый финальный ответ.
 */
@Getter
@AllArgsConstructor
public class AgentLogDto {

    private final List<JsonNode> events;
    private final String eventsJson;
    private final String finalResult;

}
