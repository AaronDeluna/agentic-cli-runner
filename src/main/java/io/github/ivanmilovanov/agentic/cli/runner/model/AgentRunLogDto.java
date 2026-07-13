package io.github.ivanmilovanov.agentic.cli.runner.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Запись лога запуска агента, сохраняемая в {@code <buildDir>/agentic-cli-runner/<uuid>.json}.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentRunLogDto {

    private final String runId;
    private final String agentSet;
    private final String startedAt;
    private final String finishedAt;
    private final String skillName;
    private final String finalResult;
    private final List<JsonNode> events;
}
