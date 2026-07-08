package io.github.ivanmilovanov.agentic.cli.runner.context;

import java.nio.file.Path;

/**
 * Контекст однократного запуска агента — типизированный вид над готовой структурой запуска:
 * <pre>
 * &lt;workspace&gt;/&lt;uuid&gt;/sorce/   — рабочая область запуска (.qwen со скилами, структура проекта)
 * &lt;workspace&gt;/&lt;uuid&gt;/logs/    — логи запуска (log.json и openai-логи)
 * </pre>
 * Конструктор принимает путь к {@code sorce}; остальные пути и runId вычисляются от него.
 * Название {@code sorce} — намеренная опечатка, сохранённая для обратной совместимости.
 */
public class AgentRunContext {

    private static final String LOGS_DIR = "logs";

    private final String runId;
    private final Path sourceDir;
    private final Path logsDir;

    /** @param sourceDir путь к директории {@code sorce} запуска */
    public AgentRunContext(Path sourceDir) {
        this.sourceDir = sourceDir;
        Path runRoot = sourceDir.getParent();
        this.runId = runRoot.getFileName().toString();
        this.logsDir = runRoot.resolve(LOGS_DIR);
    }

    /** Идентификатор запуска (имя директории {@code <uuid>}). */
    public String getRunId() {
        return runId;
    }

    /**
     * Рабочая директория запуска ({@code sorce}): здесь лежит {@code .qwen} со скилами,
     * отсюда стартует CLI, сюда же пишутся правки settings.json.
     */
    public Path getWorkspace() {
        return sourceDir;
    }

    /**
     * Директория логов запуска ({@code logs}): сюда пишется {@code log.json} и openai-логи.
     */
    public Path getRunDir() {
        return logsDir;
    }
}
