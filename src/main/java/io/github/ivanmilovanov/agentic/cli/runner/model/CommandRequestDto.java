package io.github.ivanmilovanov.agentic.cli.runner.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Запрос на выполнение команды: аргументы, рабочая директория и таймаут.
 */
@Getter
@AllArgsConstructor
public class CommandRequestDto {

    private final List<String> command;
    private final Path workingDirectory;
    private final Duration timeout;
}
