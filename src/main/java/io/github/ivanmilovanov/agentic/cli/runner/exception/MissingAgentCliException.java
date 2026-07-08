package io.github.ivanmilovanov.agentic.cli.runner.exception;

/**
 * Исключение о том, что свойство с именем CLI для запуска не задано.
 */
public class MissingAgentCliException extends AgentRunnerConfigurationException {

    public MissingAgentCliException(String propertyName) {
        super("Не передано название CLI для запуска: " + propertyName);
    }
}
