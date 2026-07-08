package io.github.ivanmilovanov.agentic.cli.runner.cli.qwen;

import io.github.ivanmilovanov.agentic.cli.runner.cli.CommandResolver;
import io.github.ivanmilovanov.agentic.cli.runner.config.OsType;
import io.github.ivanmilovanov.agentic.cli.runner.exception.CommandNotFoundException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Ищет исполняемый файл CLI-команды: сначала через переменную окружения {@code <COMMAND>_PATH},
 * затем в системном {@code PATH}, затем по fallback-путям, заданным для текущей ОС.
 */
public class OsAwareCommandResolver implements CommandResolver {

    private final OsType os;
    private final Map<OsType, List<Path>> fallbackPaths;

    /**
     * @param fallbackPaths запасные пути поиска исполняемого файла по типу ОС
     */
    public OsAwareCommandResolver(Map<OsType, List<Path>> fallbackPaths) {
        this.os = OsType.detect();
        this.fallbackPaths = fallbackPaths;
    }

    /**
     * @throws CommandNotFoundException если исполняемый файл не найден ни одним из способов
     */
    @Override
    public String resolveExecutable(String commandName) {
        // 1. Проверяем переменную окружения QWEN_PATH (или обобщённо <COMMAND>_PATH)
        String envPath = System.getenv(commandName.toUpperCase() + "_PATH");
        if (envPath != null && !envPath.isBlank()) {
            Path path = Paths.get(envPath);
            if (Files.isRegularFile(path) && Files.isExecutable(path)) {
                return path.toString();
            }
        }

        // 2. Поиск в PATH (с учётом расширений на Windows)
        String inPath = findInPath(commandName);
        if (inPath != null) return inPath;

        // 3. Fallback-пути из конфигурации (уже с подставленными переменными)
        List<Path> paths = fallbackPaths.getOrDefault(os, List.of());
        for (Path p : paths) {
            // Если p — файл, проверяем напрямую
            if (Files.isRegularFile(p) && Files.isExecutable(p)) {
                return p.toString();
            }
            // Если p — директория, ищем внутри файл с именем commandName
            Path candidate = p.resolve(commandName);
            if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                return candidate.toString();
            }
            // На Windows также пробуем с расширениями (если директория)
            if (os == OsType.WINDOWS) {
                for (String ext : getExecutableExtensions()) {
                    Path withExt = p.resolve(commandName + ext);
                    if (Files.isRegularFile(withExt) && Files.isExecutable(withExt)) {
                        return withExt.toString();
                    }
                }
            }
        }

        throw new CommandNotFoundException(
                "Не найден исполняемый файл для команды: " + commandName +
                        " (ОС: " + os + ", fallback-пути: " + paths + ")"
        );
    }

    private String findInPath(String command) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) return null;
        String[] dirs = pathEnv.split(File.pathSeparator);
        for (String dir : dirs) {
            Path file = Paths.get(dir).resolve(command);
            if (Files.isRegularFile(file) && Files.isExecutable(file)) {
                return file.toString();
            }
            // На Windows проверяем с расширениями
            if (os == OsType.WINDOWS) {
                for (String ext : getExecutableExtensions()) {
                    Path withExt = Paths.get(dir).resolve(command + ext);
                    if (Files.isRegularFile(withExt) && Files.isExecutable(withExt)) {
                        return withExt.toString();
                    }
                }
            }
        }
        return null;
    }

    private static String[] getExecutableExtensions() {
        // Получаем PATHEXT из окружения или используем стандартный набор
        String pathext = System.getenv("PATHEXT");
        if (pathext == null || pathext.isBlank()) {
            return new String[] { ".exe", ".cmd", ".bat", ".com" };
        }
        return pathext.split(";");
    }
}
