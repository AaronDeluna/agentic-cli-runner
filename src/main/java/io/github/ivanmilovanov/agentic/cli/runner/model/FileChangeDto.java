package io.github.ivanmilovanov.agentic.cli.runner.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Одно изменение файла, сделанное агентом в песочнице (относительно исходного проекта).
 * Попадает в лог запуска в массив {@code fileChanges} — чтобы результат работы агента
 * можно было проанализировать по одному JSON-файлу.
 */
@Getter
@AllArgsConstructor
public class FileChangeDto {

    /** Путь файла относительно корня рабочей области. */
    private final String path;

    /** Тип изменения: {@code added} | {@code modified} | {@code deleted}. */
    private final String changeType;

    /** Изменение в unified-формате (как {@code git diff}); для бинарных файлов — пометка. */
    private final String diff;
}
