package io.github.ivanmilovanov.agentic.cli.runner.sandbox;

import io.github.ivanmilovanov.agentic.cli.runner.context.AgentRunContext;
import io.github.ivanmilovanov.agentic.cli.runner.model.FileChangeDto;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Изоляция запуска агента. Определяет, где физически стартует CLI и можно ли ему
 * писать за пределы рабочей директории.
 * <p>
 * Жизненный цикл на один запуск: {@link #prepare(AgentRunContext)} →
 * {@link #wrapCommand(List, Path)} → выполнение → {@link #finish(AgentRunContext, Path)}
 * (последний вызывается в {@code finally}).
 * </p>
 */
public interface Sandbox {

    /**
     * Готовит рабочую директорию запуска и возвращает её.
     *
     * @param context контекст запуска (рабочая область, runId)
     * @return директория, в которой реально стартует CLI (для песочницы — временная копия)
     * @throws IOException при ошибке подготовки (например, копирования проекта)
     */
    Path prepare(AgentRunContext context) throws IOException;

    /**
     * Оборачивает команду средствами ОС-изоляции, если это поддерживается.
     * По умолчанию возвращает команду без изменений.
     *
     * @param command    исходная команда запуска CLI
     * @param sandboxDir директория, из которой стартует CLI (её вернул {@link #prepare})
     * @return команда для запуска (возможно, обёрнутая, например в {@code sandbox-exec})
     */
    default List<String> wrapCommand(List<String> command, Path sandboxDir) {
        return command;
    }

    /**
     * Возвращает изменения файлов, сделанные агентом (для встраивания в лог запуска).
     * Вызывается до {@link #cleanup(Path)}, пока временная директория ещё существует.
     * По умолчанию — пустой список (без песочницы изменения не отслеживаются).
     *
     * @param context    контекст запуска (исходная рабочая область)
     * @param sandboxDir директория, которую вернул {@link #prepare}
     * @return список изменений файлов; пустой, если изменений нет
     */
    default List<FileChangeDto> summarizeChanges(AgentRunContext context, Path sandboxDir) {
        return List.of();
    }

    /**
     * Освобождает ресурсы песочницы: удаляет временную директорию.
     * По умолчанию — ничего не делает. Вызывается в {@code finally}, поэтому не должен
     * бросать исключения наружу.
     *
     * @param sandboxDir директория, которую вернул {@link #prepare}
     */
    default void cleanup(Path sandboxDir) {
    }
}
