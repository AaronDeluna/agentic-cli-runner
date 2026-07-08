package io.github.ivanmilovanov.agentic.cli.runner.exception;

/**
 * Исключение о том, что предыдущее имя модели не сохранено — восстанавливать нечего.
 */
public class NotFoundSaveModelNameException extends RuntimeException {

    public NotFoundSaveModelNameException() {
        super("Нет сохранённого предыдущего имени модели. Сначала вызовите updateModelNameAndSave.");
    }
}
