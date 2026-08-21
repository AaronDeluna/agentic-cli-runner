package io.github.ivanmilovanov.agentic.cli.runner.sandbox;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

/**
 * Копирует дерево проекта в целевую директорию «1 в 1», пропуская каталоги-исключения.
 * <p>
 * Каталог конфига CLI ({@code keepDirName}, например {@code .qwen}) не выбрасывается,
 * даже если его имя попало в список исключений — он нужен агенту в корне копии.
 * Символьные ссылки переносятся как ссылки; при ошибке отдельный файл пропускается,
 * чтобы одна проблема не срывала подготовку всей песочницы.
 * </p>
 */
@Slf4j
public final class ProjectCopier {

    private ProjectCopier() {
    }

    /**
     * @param source       корень исходного проекта
     * @param dest         целевая директория (создаётся)
     * @param excludeNames имена каталогов, которые не копируются (сверяются по имени на любом уровне)
     * @param keepDirName  имя каталога, который нельзя исключать (конфиг CLI, например {@code .qwen})
     * @throws IOException при ошибке обхода/создания директорий
     */
    public static void copy(Path source, Path dest, Set<String> excludeNames, String keepDirName) throws IOException {
        Path normalizedSource = source.toAbsolutePath().normalize();
        Path normalizedDest = dest.toAbsolutePath().normalize();
        Files.createDirectories(normalizedDest);

        Files.walkFileTree(normalizedSource, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path current = dir.toAbsolutePath().normalize();
                // Защита от рекурсии, если dest вдруг окажется внутри source.
                if (current.equals(normalizedDest)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                String name = dir.getFileName() == null ? "" : dir.getFileName().toString();
                if (!current.equals(normalizedSource)
                        && excludeNames.contains(name)
                        && !name.equals(keepDirName)) {
                    log.debug("Песочница: пропускаю каталог '{}'", name);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Files.createDirectories(normalizedDest.resolve(normalizedSource.relativize(current)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                Path target = normalizedDest.resolve(normalizedSource.relativize(file.toAbsolutePath().normalize()));
                try {
                    if (attrs.isSymbolicLink()) {
                        Files.createSymbolicLink(target, Files.readSymbolicLink(file));
                    } else {
                        Files.copy(file, target,
                                StandardCopyOption.COPY_ATTRIBUTES,
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException | UnsupportedOperationException e) {
                    log.warn("Песочница: не удалось скопировать '{}', пропускаю: {}", file, e.getMessage());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                log.warn("Песочница: не удалось прочитать '{}', пропускаю: {}", file, exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
