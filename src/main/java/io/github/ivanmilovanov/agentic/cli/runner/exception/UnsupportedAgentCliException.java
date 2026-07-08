package io.github.ivanmilovanov.agentic.cli.runner.exception;

/**
 * Исключение о том, что указанное имя CLI не соответствует ни одному поддерживаемому агенту.
 */
public class UnsupportedAgentCliException extends AgentRunnerConfigurationException {

    public UnsupportedAgentCliException(String cliName, Throwable cause) {
        super("Неподдерживаемая CLI для запуска: " + cliName, cause);
    }
}
