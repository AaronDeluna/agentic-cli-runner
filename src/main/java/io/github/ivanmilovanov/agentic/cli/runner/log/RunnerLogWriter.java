package io.github.ivanmilovanov.agentic.cli.runner.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.ivanmilovanov.agentic.cli.runner.context.AgentRunContext;
import io.github.ivanmilovanov.agentic.cli.runner.model.AgentRunLogDto;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
     * @param context контекст запуска (определяет путь файла лога)
     * @param entry   запись лога
     */
    public void write(AgentRunContext context, AgentRunLogDto entry) {
        try {
            Path logPath = context.getLogFile();
            Files.createDirectories(logPath.getParent());
            objectMapper.writeValue(logPath.toFile(), entry);
            log.info("Лог запуска агента сохранён: {}", logPath);
        } catch (IOException e) {
            log.warn("Не удалось записать лог запуска агента", e);
        }
    }
}
