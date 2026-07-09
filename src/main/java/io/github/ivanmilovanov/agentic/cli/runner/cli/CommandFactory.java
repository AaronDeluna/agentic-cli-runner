package io.github.ivanmilovanov.agentic.cli.runner.cli;

import java.util.List;

/**
 * Собирает командную строку для запуска конкретного CLI-агента.
 */
public interface CommandFactory {

    /**
     * Строит список аргументов команды для запуска CLI с указанным промптом.
     */
    List<String> buildCommand(String prompt);
}
