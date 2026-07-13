package io.github.ivanmilovanov.agentic.cli.runner.context;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Контекст однократного запуска агента.
 * <p>
 * Библиотека сама генерирует {@code runId} и сама решает, куда писать лог запуска —
 * в {@code <buildDir>/agentic-cli-runner/<uuid>.json}: для Maven {@code buildDir} = {@code target},
 * для Gradle = {@code build}. {@code workspace} — это просто рабочая директория (cwd), из которой
 * стартует CLI; про её внутреннюю раскладку библиотека ничего не предполагает.
 * </p>
 */
public class AgentRunContext {

    private static final String LOG_DIR = "agentic-cli-runner";

    private final String runId;
    private final Path workspace;
    private final Path logFile;

    /**
     * @param workspace рабочая директория запуска (cwd для CLI); внутри может лежать
     *                  {@code .qwen/} со скилами
     */
    public AgentRunContext(Path workspace) {
        this.runId = UUID.randomUUID().toString();
        this.workspace = workspace;
        this.logFile = detectBuildDirectory().resolve(LOG_DIR).resolve(runId + ".json");
    }

    /** Идентификатор запуска (UUID). */
    public String getRunId() {
        return runId;
    }

    /** Рабочая директория запуска (cwd), из которой стартует CLI. */
    public Path getWorkspace() {
        return workspace;
    }

    /** Файл лога этого запуска: {@code <buildDir>/agentic-cli-runner/<uuid>.json}. */
    public Path getLogFile() {
        return logFile;
    }

    // build-каталог по cwd JVM: pom.xml -> target, build.gradle[.kts] -> build, иначе target.
    private static Path detectBuildDirectory() {
        Path cwd = Path.of("").toAbsolutePath();
        if (Files.isRegularFile(cwd.resolve("pom.xml"))) {
            return cwd.resolve("target");
        }
        if (Files.isRegularFile(cwd.resolve("build.gradle"))
                || Files.isRegularFile(cwd.resolve("build.gradle.kts"))) {
            return cwd.resolve("build");
        }
        return cwd.resolve("target");
    }
}
