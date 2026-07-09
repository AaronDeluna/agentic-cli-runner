package io.github.ivanmilovanov.agentic.cli.runner.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Собирает командную строку для запуска агентского CLI: исполняемый файл, обязательные
 * аргументы и сам промпт. Имя бинаря и аргументы передаются извне — так под каждый CLI
 * используются свои флаги, без хардкода в коде.
 */
public class StreamJsonCommandFactory implements CommandFactory {

    private final CommandResolver resolver;
    private final String executableName;
    private final List<String> args;
    private final List<String> prefix;

    /**
     * @param resolver       способ поиска исполняемого файла
     * @param executableName имя исполняемого файла CLI
     * @param args           обязательные аргументы запуска
     * @param prefix         префикс команды (например, {@code cmd /c} на Windows)
     */
    public StreamJsonCommandFactory(
            CommandResolver resolver,
            String executableName,
            List<String> args,
            List<String> prefix
    ) {
        this.resolver = resolver;
        this.executableName = executableName;
        this.args = args;
        this.prefix = prefix;
    }

    @Override
    public List<String> buildCommand(String prompt) {
        String executable = resolver.resolveExecutable(executableName);
        List<String> cmd = new ArrayList<>();
        if (prefix != null) cmd.addAll(prefix);
        cmd.add(executable);
        cmd.addAll(args);
        cmd.add(prompt);
        return Collections.unmodifiableList(cmd);
    }
}
