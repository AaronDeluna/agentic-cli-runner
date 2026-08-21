package io.github.ivanmilovanov.agentic.cli.runner.sandbox;

import io.github.ivanmilovanov.agentic.cli.runner.config.OsType;
import io.github.ivanmilovanov.agentic.cli.runner.context.AgentRunContext;
import io.github.ivanmilovanov.agentic.cli.runner.model.FileChangeDto;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Песочница на копии проекта: перед запуском копирует проект во временную директорию
 * (без каталогов-исключений, но с каталогом конфига CLI), запускает агента там и
 * запрещает запись наружу. Перед удалением отдаёт список изменений файлов для лога запуска.
 * <p>
 * Два слоя защиты:
 * <ol>
 *   <li>изоляция через копию + временную рабочую директорию (всегда);</li>
 *   <li>жёсткий запрет записи средствами ОС ({@code sandbox-exec}/{@code bwrap}/{@code firejail})
 *       — если {@code osEnforcement=true} и ОС это поддерживает.</li>
 * </ol>
 */
@Slf4j
public class CopyingSandbox implements Sandbox {

    private static final String DIR_PREFIX = "agentic-sandbox-";

    private final String cliConfigDirName;
    private final Set<String> excludeNames;
    private final boolean osEnforcement;
    private final OsType os;

    /**
     * @param cliName       имя активного CLI ({@code agent.cli}); каталог конфига — {@code .<cliName>}
     * @param excludes      имена каталогов-исключений при копировании
     * @param osEnforcement включать ли жёсткий ОС-слой запрета записи
     * @param os            тип ОС
     */
    public CopyingSandbox(String cliName, List<String> excludes, boolean osEnforcement, OsType os) {
        this.cliConfigDirName = "." + cliName.toLowerCase();
        this.excludeNames = new LinkedHashSet<>(excludes);
        this.osEnforcement = osEnforcement;
        this.os = os;
    }

    @Override
    public Path prepare(AgentRunContext context) throws IOException {
        Path source = context.getWorkspace().toAbsolutePath().normalize();
        Path sandboxDir = Path.of(System.getProperty("java.io.tmpdir"))
                .resolve(DIR_PREFIX + context.getRunId());

        // Копируем рабочую область как есть: каталог конфига CLI (.<cli>) едет в копию
        // и защищён от случайного исключения.
        log.info("Песочница: копирую проект {} -> {}", source, sandboxDir);
        ProjectCopier.copy(source, sandboxDir, excludeNames, cliConfigDirName);
        return sandboxDir;
    }

    @Override
    public List<String> wrapCommand(List<String> command, Path sandboxDir) {
        if (!osEnforcement) {
            log.info("Песочница: ОС-изоляция записи отключена (agent.sandbox.os-enforcement=false)");
            return command;
        }
        return OsSandboxCommandWrapper.wrap(command, sandboxDir, os);
    }

    @Override
    public List<FileChangeDto> summarizeChanges(AgentRunContext context, Path sandboxDir) {
        try {
            Path source = context.getWorkspace().toAbsolutePath().normalize();
            // Каталог конфига CLI (.<cli>) из diff НЕ исключаем: в копии он проектный, поэтому любые
            // изменения агента внутри него (например, созданные файлы) — настоящие, их надо показывать.
            List<FileChangeDto> changes = SandboxDiff.changes(source, sandboxDir, excludeNames, cliConfigDirName);
            if (changes.isEmpty()) {
                log.info("Песочница: агент не изменил файлов");
            } else {
                log.info("Песочница: агент изменил файлов: {}", changes.size());
            }
            return changes;
        } catch (Exception e) {
            log.warn("Песочница: не удалось вычислить изменения файлов", e);
            return List.of();
        }
    }

    @Override
    public void cleanup(Path sandboxDir) {
        deleteRecursively(sandboxDir);
    }

    private static void deleteRecursively(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    log.warn("Песочница: не удалось удалить {}: {}", p, e.getMessage());
                }
            });
            log.info("Песочница: временная директория удалена: {}", dir);
        } catch (IOException e) {
            log.warn("Песочница: не удалось удалить временную директорию {}: {}", dir, e.getMessage());
        }
    }
}
