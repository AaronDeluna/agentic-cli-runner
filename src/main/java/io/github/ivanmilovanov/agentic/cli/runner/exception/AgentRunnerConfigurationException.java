package io.github.ivanmilovanov.agentic.cli.runner.exception;

/**
 * Базовое исключение для ошибок конфигурации агента-раннера (настройки, пути, CLI).
 */
public class AgentRunnerConfigurationException extends RuntimeException {

    public AgentRunnerConfigurationException(String message) {
        super(message);
    }

    public AgentRunnerConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
