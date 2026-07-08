package io.github.ivanmilovanov.agentic.cli.runner.service;

import io.github.ivanmilovanov.agentic.cli.runner.api.AgentRunner;
import io.github.ivanmilovanov.agentic.cli.runner.cli.CommandFactory;
import io.github.ivanmilovanov.agentic.cli.runner.cli.OsAwareCommandResolver;
import io.github.ivanmilovanov.agentic.cli.runner.cli.StreamJsonCommandFactory;
import io.github.ivanmilovanov.agentic.cli.runner.config.AgentRunnerProperties;
import io.github.ivanmilovanov.agentic.cli.runner.config.OsType;
import io.github.ivanmilovanov.agentic.cli.runner.executor.ApacheCommandExecutor;
import io.github.ivanmilovanov.agentic.cli.runner.executor.CommandExecutor;
import io.github.ivanmilovanov.agentic.cli.runner.log.RunnerLogWriter;
import io.github.ivanmilovanov.agentic.cli.runner.parser.AgentStreamJsonParser;
import io.github.ivanmilovanov.agentic.cli.runner.runner.AgentRunnerImpl;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Собирает {@link AgentRunner} для CLI, выбранного в настройках ({@code agent.cli}).
 * Значение {@code agent.cli} используется как имя исполняемого файла — механика запуска
 * одинакова для всех совместимых CLI со stream-json выводом (Qwen Code, GigaCode и т.п.),
 * поэтому отдельных реализаций под каждый CLI не требуется.
 */
@Slf4j
public class AgentRunnerFactory {

    private final CommandExecutor commandExecutor;
    private final AgentStreamJsonParser agentStreamJsonParser;
    private final Path workingDirectory;
    private final Duration timeout;
    private final RunnerLogWriter runnerLogWriter;

    /** Создаёт фабрику с настройками по умолчанию (Apache-исполнитель, стандартный таймаут). */
    public static AgentRunnerFactory defaultFactory(Path workspace) {
        return new AgentRunnerFactory(
                new ApacheCommandExecutor(),
                new AgentStreamJsonParser(),
                workspace,
                AgentRunnerImpl.DEFAULT_TIMEOUT,
                new RunnerLogWriter()
        );
    }

    public AgentRunnerFactory(
            CommandExecutor commandExecutor,
            AgentStreamJsonParser agentStreamJsonParser,
            Path workingDirectory,
            Duration timeout,
            RunnerLogWriter runnerLogWriter
    ) {
        this.commandExecutor = commandExecutor;
        this.agentStreamJsonParser = agentStreamJsonParser;
        this.workingDirectory = workingDirectory;
        this.timeout = timeout;
        this.runnerLogWriter = runnerLogWriter;
    }

    /**
     * Создаёт {@link AgentRunner} для CLI, указанного в свойстве {@code agent.cli}.
     *
     * @throws io.github.ivanmilovanov.agentic.cli.runner.exception.MissingAgentCliException если CLI не указан
     */
    public AgentRunner create(Properties properties) {
        String cliName = AgentRunnerProperties.getCliName(properties);
        log.info("Запуск через CLI: {}", cliName);

        return new AgentRunnerImpl(
                commandExecutor,
                agentStreamJsonParser,
                runnerLogWriter,
                workingDirectory,
                timeout,
                createCommandFactory(properties)
        );
    }

    /** Собирает {@link CommandFactory} для CLI, указанного в {@code agent.cli}. */
    public static CommandFactory createCommandFactory(Properties props) {
        String cliName = AgentRunnerProperties.getCliName(props);

        // Fallback-пути поиска исполняемого файла по ОС
        Map<OsType, List<Path>> fallbacks = new HashMap<>();
        for (OsType os : OsType.values()) {
            List<Path> paths = AgentRunnerProperties.getFallbackPaths(props, os);
            if (!paths.isEmpty()) {
                fallbacks.put(os, paths);
            }
        }
        OsAwareCommandResolver resolver = new OsAwareCommandResolver(fallbacks);

        // Префикс команды (только для Windows)
        List<String> prefix = AgentRunnerProperties.getPrefix(props, OsType.detect());

        return new StreamJsonCommandFactory(resolver, cliName, prefix);
    }
}
