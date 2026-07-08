package io.github.ivanmilovanov.agentic.cli.runner.exception;

/**
 * Исключение о недопустимом имени скила (пустое или содержит фрагменты пути).
 */
public class InvalidSkillNameException extends RuntimeException {

    public InvalidSkillNameException(String message) {
        super(message);
    }
}
