package io.github.ivanmilovanov.agentic.cli.runner.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Результат выполнения команды: вывод процесса, код завершения и признак таймаута.
 */
@Getter
@AllArgsConstructor
public class CommandResultDto {

    private final String stdout;
    private final String stderr;
    private final int exitCode;
    private final boolean timedOut;
}
