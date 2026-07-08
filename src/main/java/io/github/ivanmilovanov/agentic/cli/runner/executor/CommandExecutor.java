package io.github.ivanmilovanov.agentic.cli.runner.executor;

import io.github.ivanmilovanov.agentic.cli.runner.model.CommandRequestDto;
import io.github.ivanmilovanov.agentic.cli.runner.model.CommandResultDto;

/**
 * Выполняет команду в отдельном процессе и возвращает результат её выполнения.
 */
public interface CommandExecutor {

    /** Запускает команду из запроса и дожидается её завершения (или таймаута). */
    CommandResultDto execute(CommandRequestDto request) throws Exception;
}
