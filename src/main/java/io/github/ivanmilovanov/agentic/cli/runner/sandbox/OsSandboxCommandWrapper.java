package io.github.ivanmilovanov.agentic.cli.runner.sandbox;

import io.github.ivanmilovanov.agentic.cli.runner.config.OsType;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Оборачивает команду запуска CLI в ОС-песочницу, физически запрещающую запись
 * за пределы {@code sandboxDir} (и системного temp).
 * <ul>
 *   <li>macOS — {@code sandbox-exec} с профилем «всё читать можно, писать — только в песочницу»;</li>
 *   <li>Linux — {@code bwrap} (bubblewrap) или {@code firejail}, если установлены;</li>
 *   <li>прочие ОС (в т.ч. Windows) — обёртки нет, команда возвращается как есть (мягкий слой).</li>
 * </ul>
 * Если нужный инструмент недоступен, метод логирует предупреждение и откатывается на
 * запуск без ОС-изоляции — за счёт копии проекта реальный проект всё равно не затрагивается.
 */
@Slf4j
public final class OsSandboxCommandWrapper {

    private OsSandboxCommandWrapper() {
    }

    /**
     * @param command    исходная команда (исполняемый файл CLI + аргументы)
     * @param sandboxDir директория, в которую разрешена запись
     * @param os         тип ОС
     * @return обёрнутая команда, либо исходная, если ОС-изоляция недоступна
     */
    public static List<String> wrap(List<String> command, Path sandboxDir, OsType os) {
        Path dir = sandboxDir.toAbsolutePath().normalize();
        switch (os) {
            case MAC:
                return wrapMac(command, dir);
            case LINUX:
                return wrapLinux(command, dir);
            default:
                log.warn("Песочница: жёсткая ОС-изоляция записи не поддерживается на {} — "
                        + "используется только изоляция через копию проекта", os);
                return command;
        }
    }

    private static List<String> wrapMac(List<String> command, Path sandboxDir) {
        if (!isExecutableOnPath("sandbox-exec")) {
            log.warn("Песочница: 'sandbox-exec' не найден — ОС-изоляция записи отключена");
            return command;
        }
        String profile = buildMacProfile(sandboxDir);
        List<String> wrapped = new ArrayList<>();
        wrapped.add("sandbox-exec");
        wrapped.add("-p");
        wrapped.add(profile);
        wrapped.addAll(command);
        log.info("Песочница: запуск через sandbox-exec, запись разрешена только в {}", sandboxDir);
        return wrapped;
    }

    // Профиль Seatbelt: всё разрешено, запись запрещена везде, кроме песочницы, temp и кэшей CLI.
    private static String buildMacProfile(Path sandboxDir) {
        StringBuilder p = new StringBuilder();
        p.append("(version 1)\n");
        p.append("(allow default)\n");
        p.append("(deny file-write*)\n");
        p.append(allowWrite(sandboxDir.toString()));
        p.append(allowWrite("/private/tmp"));
        p.append(allowWrite("/private/var/folders"));
        p.append(allowWrite("/dev"));
        String tmpDir = System.getenv("TMPDIR");
        if (tmpDir != null && !tmpDir.isBlank()) {
            p.append(allowWrite(tmpDir));
        }
        String home = System.getProperty("user.home");
        if (home != null && !home.isBlank()) {
            // Кэши/конфиги CLI в домашней папке — писать можно (это не реальный проект).
            p.append(allowWrite(home + "/.qwen"));
            p.append(allowWrite(home + "/.codex"));
            p.append(allowWrite(home + "/.claude"));
            p.append(allowWrite(home + "/.cache"));
            p.append(allowWrite(home + "/.npm"));
            p.append(allowWrite(home + "/Library/Caches"));
        }
        return p.toString();
    }

    private static String allowWrite(String path) {
        return "(allow file-write* (subpath \"" + path + "\"))\n";
    }

    private static List<String> wrapLinux(List<String> command, Path sandboxDir) {
        if (isExecutableOnPath("bwrap")) {
            List<String> wrapped = new ArrayList<>(List.of(
                    "bwrap",
                    "--die-with-parent",
                    "--ro-bind", "/", "/",
                    "--dev", "/dev",
                    "--proc", "/proc",
                    "--tmpfs", "/tmp",
                    "--bind", sandboxDir.toString(), sandboxDir.toString(),
                    "--chdir", sandboxDir.toString()
            ));
            // Кэши/конфиги CLI в домашней папке делаем записываемыми, если существуют.
            for (String cacheDir : linuxWritableHomeDirs()) {
                wrapped.add("--bind");
                wrapped.add(cacheDir);
                wrapped.add(cacheDir);
            }
            wrapped.addAll(command);
            log.info("Песочница: запуск через bwrap, запись разрешена только в {}", sandboxDir);
            return wrapped;
        }
        if (isExecutableOnPath("firejail")) {
            List<String> wrapped = new ArrayList<>(List.of(
                    "firejail",
                    "--quiet",
                    "--noprofile",
                    "--read-only=/",
                    "--read-write=" + sandboxDir,
                    "--read-write=/tmp"
            ));
            wrapped.addAll(command);
            log.info("Песочница: запуск через firejail, запись разрешена только в {}", sandboxDir);
            return wrapped;
        }
        log.warn("Песочница: ни 'bwrap', ни 'firejail' не найдены — ОС-изоляция записи отключена");
        return command;
    }

    private static List<String> linuxWritableHomeDirs() {
        String home = System.getProperty("user.home");
        if (home == null || home.isBlank()) {
            return List.of();
        }
        List<String> dirs = new ArrayList<>();
        for (String sub : List.of(".qwen", ".codex", ".claude", ".cache", ".npm", ".config")) {
            Path candidate = Path.of(home, sub);
            if (Files.isDirectory(candidate)) {
                dirs.add(candidate.toString());
            }
        }
        return dirs;
    }

    /**
     * Проверяет, доступен ли исполняемый файл в {@code PATH}.
     *
     * @param name имя исполняемого файла
     * @return {@code true}, если найден в одном из каталогов {@code PATH}
     */
    static boolean isExecutableOnPath(String name) {
        String path = System.getenv("PATH");
        if (path == null || path.isBlank()) {
            return false;
        }
        for (String dir : path.split(File.pathSeparator)) {
            if (dir.isBlank()) {
                continue;
            }
            Path candidate = Path.of(dir, name);
            if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                return true;
            }
        }
        return false;
    }
}
