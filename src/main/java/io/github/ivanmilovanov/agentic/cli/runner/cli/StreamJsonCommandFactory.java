package io.github.ivanmilovanov.agentic.cli.runner.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Собирает командную строку для запуска агентского CLI со stream-json выводом
 * (Qwen Code, GigaCode и другие совместимые форки): исполняемый файл,
 * обязательные аргументы, опции логирования и сам промпт.
 * <p>
 * Аргументы {@code --output-format stream-json} и {@code --approval-mode yolo} —
 * это контракт библиотеки, а не пользовательская настройка: парсер разбирает именно
 * stream-json, а headless-запуск не должен зависать на подтверждениях. Поэтому они
 * зашиты здесь и не выносятся в конфиг.
 * </p>
 */
public class StreamJsonCommandFactory implements CommandFactory {

    /** Обязательные аргументы запуска (контракт библиотеки). */
    private static final List<String> CONTRACT_ARGS = List.of(
            "--output-format", "stream-json",
            "--approval-mode", "yolo"
    );

    private final CommandResolver resolver;
    private final String executableName;
    private final List<String> prefix;

    /**
     * @param resolver       способ поиска исполняемого файла
     * @param executableName имя исполняемого файла CLI (например, {@code qwen}, {@code gigacode})
     * @param prefix         префикс команды (например, {@code cmd /c} на Windows)
     */
    public StreamJsonCommandFactory(CommandResolver resolver, String executableName, List<String> prefix) {
        this.resolver = resolver;
        this.executableName = executableName;
        this.prefix = prefix;
    }

    @Override
    public List<String> buildCommand(String prompt, Path logDir) {
        String executable = resolver.resolveExecutable(executableName);
        List<String> cmd = new ArrayList<>();
        if (prefix != null) cmd.addAll(prefix);
        cmd.add(executable);
        cmd.addAll(CONTRACT_ARGS);
        if (logDir != null) {
            cmd.add("--openai-logging");
            cmd.add("true");
            cmd.add("--openai-logging-dir");
            cmd.add(logDir.toAbsolutePath().toString());
        }
        cmd.add(prompt);
        return Collections.unmodifiableList(cmd);
    }
}
