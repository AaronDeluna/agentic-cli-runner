package io.github.ivanmilovanov.agentic.cli.runner.log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.ivanmilovanov.agentic.cli.runner.config.AgentLogLevel;
import io.github.ivanmilovanov.agentic.cli.runner.context.AgentRunContext;
import io.github.ivanmilovanov.agentic.cli.runner.model.AgentRunLogDto;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Записывает результат запуска агента в файл {@code <buildDir>/agentic-cli-runner/<uuid>.json}.
 */
@Slf4j
public class RunnerLogWriter {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Сохраняет запись лога. Ошибки записи логируются и не пробрасываются вызывающему коду.
     *
     * @param context  контекст запуска (определяет путь файла лога)
     * @param entry    запись лога
     * @param logLevel уровень детализации ({@link AgentLogLevel#FULL} — как есть,
     *                 {@link AgentLogLevel#COMPACT} — без служебных полей событий)
     */
    public void write(AgentRunContext context, AgentRunLogDto entry, AgentLogLevel logLevel) {
        try {
            Path logPath = context.getLogFile();
            Files.createDirectories(logPath.getParent());
            AgentRunLogDto effectiveEntry = logLevel == AgentLogLevel.COMPACT ? compact(entry) : entry;
            objectMapper.writeValue(logPath.toFile(), effectiveEntry);
            log.info("Лог запуска агента сохранён ({}): {}", logLevel, logPath);
        } catch (IOException e) {
            log.warn("Не удалось записать лог запуска агента", e);
        }
    }

    // Копия записи с рекурсивно вырезанными служебными полями событий; исходные события не мутируются.
    private AgentRunLogDto compact(AgentRunLogDto entry) {
        List<JsonNode> events = entry.getEvents();
        if (events == null) {
            return entry;
        }
        return AgentRunLogDto.builder()
                .runId(entry.getRunId())
                .agentSet(entry.getAgentSet())
                .startedAt(entry.getStartedAt())
                .finishedAt(entry.getFinishedAt())
                .skillName(entry.getSkillName())
                .finalResult(entry.getFinalResult())
                .events(EventCompactor.strip(events))
                .fileChanges(entry.getFileChanges())
                .build();
    }
}
