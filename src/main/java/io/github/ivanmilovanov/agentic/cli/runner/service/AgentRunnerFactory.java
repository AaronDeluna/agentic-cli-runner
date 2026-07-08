package io.github.ivanmilovanov.agentic.cli.runner.service;

import io.github.ivanmilovanov.agentic.cli.runner.api.AgentRunner;
import io.github.ivanmilovanov.agentic.cli.runner.cli.CommandFactory;
import io.github.ivanmilovanov.agentic.cli.runner.cli.qwen.OsAwareCommandResolver;
import io.github.ivanmilovanov.agentic.cli.runner.cli.qwen.QwenCommandFactoryImpl;
import io.github.ivanmilovanov.agentic.cli.runner.config.AgentCli;
import io.github.ivanmilovanov.agentic.cli.runner.config.AgentRunnerProperties;
import io.github.ivanmilovanov.agentic.cli.runner.config.OsType;
import io.github.ivanmilovanov.agentic.cli.runner.executor.ApacheCommandExecutor;
import io.github.ivanmilovanov.agentic.cli.runner.executor.CommandExecutor;
import io.github.ivanmilovanov.agentic.cli.runner.log.RunnerLogWriter;
import io.github.ivanmilovanov.agentic.cli.runner.parser.AgentStreamJsonParser;
import io.github.ivanmilovanov.agentic.cli.runner.runner.qwen.QwenAgentRunner;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Собирает {@link AgentRunner} для CLI, выбранного в настройках ({@code agent.cli}).
 * Чтобы добавить поддержку нового CLI, реализуйте {@link CommandFactory} и добавьте
 * соответствующую ветку в {@link #create(Properties)} и {@link #createCommandFactory}.
 */
@Slf4j
public class AgentRunnerFactory {

    private final CommandExecutor commandExecutor;
    private final AgentStreamJsonParser agentStreamJsonParser;
    private final Path workingDirectory;
    private final Duration timeout;
    private final RunnerLogWriter runnerLogWriter;

    /** Создаёт фабрику с настройками по умолчанию (Apache-исполнитель, стандартный таймаут Qwen). */
    public static AgentRunnerFactory defaultFactory(Path workspace) {
        return new AgentRunnerFactory(
                new ApacheCommandExecutor(),
                new AgentStreamJsonParser(),
                workspace,
                QwenAgentRunner.DEFAULT_TIMEOUT,
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
     * @throws io.github.ivanmilovanov.agentic.cli.runner.exception.MissingAgentCliException      если CLI не указан
     * @throws io.github.ivanmilovanov.agentic.cli.runner.exception.UnsupportedAgentCliException если CLI не поддерживается
     */
    public AgentRunner create(Properties properties) {
        AgentCli cli = AgentCli.fromProperty(properties.getProperty(AgentRunnerProperties.CLI_PROPERTY));
        log.info("Запуск через CLI: {}", cli);

        CommandFactory commandFactory = createCommandFactory(cli, properties);

        return switch (cli) {
            case QWEN -> new QwenAgentRunner(
                    commandExecutor,
                    agentStreamJsonParser,
                    runnerLogWriter,
                    workingDirectory,
                    timeout,
                    commandFactory
            );
        };
    }

    /** Собирает {@link CommandFactory} для указанного CLI на основе настроек. */
    public static CommandFactory createCommandFactory(AgentCli cli, Properties props) {
        String cliName = cli.name().toLowerCase();
        return switch (cli) {
            case QWEN -> {
                // Строим fallback-карту для всех ОС
                Map<OsType, List<Path>> fallbacks = new HashMap<>();
                for (OsType os : OsType.values()) {
                    List<Path> paths = AgentRunnerProperties.getFallbackPaths(props, cliName, os);
                    if (!paths.isEmpty()) {
                        fallbacks.put(os, paths);
                    }
                }
                OsAwareCommandResolver resolver = new OsAwareCommandResolver(fallbacks);

                // Базовые аргументы
                List<String> baseArgs = AgentRunnerProperties.getBaseArgs(props, cliName);

                // Префикс (только для Windows)
                List<String> prefix = AgentRunnerProperties.getPrefix(props, cliName, OsType.detect());

                yield new QwenCommandFactoryImpl(resolver, baseArgs, prefix);
            }
        };
    }
}
