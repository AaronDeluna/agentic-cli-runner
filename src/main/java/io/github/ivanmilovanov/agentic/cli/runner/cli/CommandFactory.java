package io.github.ivanmilovanov.agentic.cli.runner.cli;

import java.nio.file.Path;
import java.util.List;

/**
 * Собирает командную строку для запуска конкретного CLI-агента.
 */
public interface CommandFactory {

    /**
     * Строит список аргументов команды для запуска CLI с указанным промптом.
     *
     * @param logDir директория для логов CLI; если {@code null}, логирование не настраивается
     */
    List<String> buildCommand(String prompt, Path logDir);
}
