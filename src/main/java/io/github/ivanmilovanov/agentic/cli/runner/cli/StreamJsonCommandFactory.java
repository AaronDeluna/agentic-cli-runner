package io.github.ivanmilovanov.agentic.cli.runner.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Собирает командную строку для запуска агентского CLI: исполняемый файл, обязательные
 * аргументы, опции логирования и сам промпт. Имя бинаря, аргументы и признак поддержки
 * openai-логов передаются извне — так под каждый CLI используются свои флаги.
 */
public class StreamJsonCommandFactory implements CommandFactory {

    private final CommandResolver resolver;
    private final String executableName;
    private final List<String> args;
    private final boolean supportsOpenaiLoggingDir;
    private final List<String> prefix;

    /**
     * @param resolver                 способ поиска исполняемого файла
     * @param executableName           имя исполняемого файла CLI
     * @param args                     обязательные аргументы запуска
     * @param supportsOpenaiLoggingDir добавлять ли флаги {@code --openai-logging-dir}
     * @param prefix                   префикс команды (например, {@code cmd /c} на Windows)
     */
    public StreamJsonCommandFactory(
            CommandResolver resolver,
            String executableName,
            List<String> args,
            boolean supportsOpenaiLoggingDir,
            List<String> prefix
    ) {
        this.resolver = resolver;
        this.executableName = executableName;
        this.args = args;
        this.supportsOpenaiLoggingDir = supportsOpenaiLoggingDir;
        this.prefix = prefix;
    }

    @Override
    public List<String> buildCommand(String prompt, Path logDir) {
        String executable = resolver.resolveExecutable(executableName);
        List<String> cmd = new ArrayList<>();
        if (prefix != null) cmd.addAll(prefix);
        cmd.add(executable);
        cmd.addAll(args);
        if (logDir != null && supportsOpenaiLoggingDir) {
            cmd.add("--openai-logging");
            cmd.add("true");
            cmd.add("--openai-logging-dir");
            cmd.add(logDir.toAbsolutePath().toString());
        }
        cmd.add(prompt);
        return Collections.unmodifiableList(cmd);
    }
}
