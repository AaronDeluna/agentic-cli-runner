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
 * Записывает результат запуска агента в файл {@code log.json} внутри директории логов запуска.
 */
@Slf4j
public class RunnerLogWriter {

    private static final String LOG_FILE = "log.json";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /** Сохраняет запись лога; ошибки записи логируются, но не пробрасываются вызывающему коду. */
    public void write(AgentRunContext context, AgentRunLogDto entry) {
        try {
            Files.createDirectories(context.getRunDir());
            Path logPath = context.getRunDir().resolve(LOG_FILE);
            objectMapper.writeValue(logPath.toFile(), entry);
            log.info("Лог запуска агента сохранён: {}", logPath);
        } catch (IOException e) {
            log.warn("Не удалось записать лог запуска агента", e);
        }
    }
}
