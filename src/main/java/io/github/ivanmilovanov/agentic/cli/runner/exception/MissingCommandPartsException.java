package io.github.ivanmilovanov.agentic.cli.runner.exception;

/**
 * Исключение о том, что команда для выполнения оказалась пустой.
 */
public class MissingCommandPartsException extends AgentRunnerConfigurationException {

    public MissingCommandPartsException() {
        super("Команда не может быть пустой");
    }
}
