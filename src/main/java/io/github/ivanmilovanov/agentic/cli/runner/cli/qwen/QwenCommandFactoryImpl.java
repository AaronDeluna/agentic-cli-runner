package io.github.ivanmilovanov.agentic.cli.runner.cli.qwen;

import io.github.ivanmilovanov.agentic.cli.runner.cli.CommandFactory;
import io.github.ivanmilovanov.agentic.cli.runner.cli.CommandResolver;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Собирает командную строку для запуска Qwen CLI: исполняемый файл, базовые аргументы,
 * опции логирования и сам промпт.
 */
public class QwenCommandFactoryImpl implements CommandFactory {

    private final CommandResolver resolver;
    private final List<String> baseArgs;
    private final List<String> prefix;

    /**
     * @param resolver способ поиска исполняемого файла {@code qwen}
     * @param baseArgs аргументы, добавляемые к каждому запуску (например, режим вывода)
     * @param prefix   префикс команды (например, {@code cmd /c} на Windows)
     */
    public QwenCommandFactoryImpl(CommandResolver resolver, List<String> baseArgs, List<String> prefix) {
        this.resolver = resolver;
        this.baseArgs = baseArgs;
        this.prefix = prefix;
    }

    @Override
    public List<String> buildCommand(String prompt, Path logDir) {
        String executable = resolver.resolveExecutable("qwen");
        List<String> cmd = new ArrayList<>();
        if (prefix != null) cmd.addAll(prefix);
        cmd.add(executable);
        if (baseArgs != null) cmd.addAll(baseArgs);
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
