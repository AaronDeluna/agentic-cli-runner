package io.github.ivanmilovanov.agentic.cli.runner.executor;

import io.github.ivanmilovanov.agentic.cli.runner.exception.MissingCommandPartsException;
import io.github.ivanmilovanov.agentic.cli.runner.model.CommandRequestDto;
import io.github.ivanmilovanov.agentic.cli.runner.model.CommandResultDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.OutputStream;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Реализация {@link CommandExecutor} на Apache Commons Exec: запуск процесса,
 * ограничение по таймауту через watchdog, сбор stdout/stderr.
 * <p>
 * Вывод логируется построчно по мере поступления и одновременно накапливается целиком;
 * итоговый результат разбирается после завершения процесса. Перед выводом в живой лог
 * строки stdout прогоняются через {@code stdoutLineFormatter}.
 */
@Slf4j
public class ApacheCommandExecutor implements CommandExecutor {

    private final UnaryOperator<String> stdoutLineFormatter;

    // Компактная строка команды для лога: длинные многострочные аргументы (например,
    // seatbelt-профиль sandbox-exec) схлопываются в одну строку с усечением — на само
    // выполнение это не влияет, только на читаемость лога.
    private static final int MAX_LOGGED_ARG_LENGTH = 120;

    /**
     * Создаёт исполнитель без форматирования живого лога (stdout выводится как есть).
     */
    public ApacheCommandExecutor() {
        this(UnaryOperator.identity());
    }

    /**
     * @param stdoutLineFormatter форматирует строку stdout для живого лога;
     *                            на накопление полного stdout не влияет
     */
    public ApacheCommandExecutor(UnaryOperator<String> stdoutLineFormatter) {
        this.stdoutLineFormatter = stdoutLineFormatter != null ? stdoutLineFormatter : UnaryOperator.identity();
    }

    /**
     * @throws MissingCommandPartsException если команда в запросе пустая
     */
    @Override
    public CommandResultDto execute(CommandRequestDto request) throws Exception {
        List<String> commandParts = request.getCommand();
        if (commandParts == null || commandParts.isEmpty()) {
            throw new MissingCommandPartsException();
        }

        CommandLine command = new CommandLine(commandParts.get(0));
        for (int i = 1; i < commandParts.size(); i++) {
            command.addArgument(commandParts.get(i), false);
        }

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        OutputStream stdoutStream = liveStream(stdout, "[CLI]", stdoutLineFormatter);
        OutputStream stderrStream = liveStream(stderr, "[CLI-ERR]", UnaryOperator.identity());

        ExecuteWatchdog watchdog = ExecuteWatchdog.builder()
                .setTimeout(request.getTimeout())
                .get();

        DefaultExecutor executor = DefaultExecutor.builder()
                .get();

        executor.setWatchdog(watchdog);
        executor.setStreamHandler(new PumpStreamHandler(stdoutStream, stderrStream));
        executor.setExitValues(null);
        if (request.getWorkingDirectory() != null) {
            executor.setWorkingDirectory(request.getWorkingDirectory().toFile());
        }

        log.info("Executing command: {}, workingDirectory={}, timeout={}",
                compactCommand(commandParts),
                request.getWorkingDirectory(),
                request.getTimeout());

        int exitCode = executor.execute(command);

        return new CommandResultDto(
                stdout.toString(),
                stderr.toString(),
                exitCode,
                watchdog.killedProcess()
        );
    }

    private static String compactCommand(List<String> parts) {
        return parts.stream()
                .map(ApacheCommandExecutor::compactArg)
                .collect(Collectors.joining(" "));
    }

    private static String compactArg(String arg) {
        if (arg == null) {
            return "";
        }
        String oneLine = arg.replace('\n', ' ').replace('\r', ' ');
        if (oneLine.length() <= MAX_LOGGED_ARG_LENGTH) {
            return oneLine;
        }
        return oneLine.substring(0, MAX_LOGGED_ARG_LENGTH) + "…(+" + (oneLine.length() - MAX_LOGGED_ARG_LENGTH) + " chars)";
    }

    // На каждую строку: дописывает её в accumulator (для разбора) и логирует через formatter.
    private static OutputStream liveStream(StringBuilder accumulator, String tag, UnaryOperator<String> formatter) {
        return new LogOutputStream() {
            @Override
            protected void processLine(String line, int level) {
                accumulator.append(line).append('\n');
                log.info("{} {}", tag, formatter.apply(line));
            }
        };
    }
}
