package io.github.ivanmilovanov.agentic.cli.runner.cli;

import io.github.ivanmilovanov.agentic.cli.runner.exception.CommandNotFoundException;

/**
 * Находит путь к исполняемому файлу CLI по его имени.
 */
public interface CommandResolver {

    /**
     * Возвращает абсолютный путь к исполняемому файлу команды.
     *
     * @throws CommandNotFoundException если исполняемый файл не найден
     */
    String resolveExecutable(String commandName) throws CommandNotFoundException;
}
