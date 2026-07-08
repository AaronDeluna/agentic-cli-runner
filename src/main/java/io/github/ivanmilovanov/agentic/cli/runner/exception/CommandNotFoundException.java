package io.github.ivanmilovanov.agentic.cli.runner.exception;

/**
 * Исключение о том, что исполняемый файл CLI-команды не найден ни одним из способов поиска.
 */
public class CommandNotFoundException extends AgentRunnerConfigurationException {

    public CommandNotFoundException(String message) {
        super(message);
    }
}
