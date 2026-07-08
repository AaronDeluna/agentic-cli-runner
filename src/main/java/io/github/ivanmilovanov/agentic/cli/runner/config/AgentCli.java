package io.github.ivanmilovanov.agentic.cli.runner.config;

import io.github.ivanmilovanov.agentic.cli.runner.exception.MissingAgentCliException;
import io.github.ivanmilovanov.agentic.cli.runner.exception.UnsupportedAgentCliException;

import java.util.Locale;

/**
 * Поддерживаемые агентские CLI, которые может запускать библиотека.
 */
public enum AgentCli {

    QWEN;

    /**
     * Разбирает значение свойства {@code agent.cli} в элемент перечисления.
     *
     * @throws MissingAgentCliException      если значение не задано
     * @throws UnsupportedAgentCliException если значение не соответствует ни одному CLI
     */
    public static AgentCli fromProperty(String value) {
        if (value == null || value.isBlank()) {
            throw new MissingAgentCliException(AgentRunnerProperties.CLI_PROPERTY);
        }

        String normalizedValue = value.trim().toUpperCase(Locale.ROOT);
        try {
            return AgentCli.valueOf(normalizedValue);
        } catch (IllegalArgumentException e) {
            throw new UnsupportedAgentCliException(value, e);
        }
    }
}
