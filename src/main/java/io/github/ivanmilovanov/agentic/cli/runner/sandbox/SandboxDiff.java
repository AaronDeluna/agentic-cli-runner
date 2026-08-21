package io.github.ivanmilovanov.agentic.cli.runner.sandbox;

import io.github.ivanmilovanov.agentic.cli.runner.model.FileChangeDto;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Считает, что агент изменил в песочнице: сравнивает исходный проект с временной копией
 * и формирует патч в unified-формате (как {@code git diff}) — чистой Java, без внешних утилит.
 * Каталоги-исключения и каталог конфига CLI обрабатываются так же, как при копировании.
 */
@Slf4j
public final class SandboxDiff {

    private static final int CONTEXT = 3;

    private SandboxDiff() {
    }

    /**
     * @param original     корень исходного (нетронутого) проекта
     * @param modified     корень песочницы после работы агента
     * @param excludeNames имена каталогов, которые не учитываются в diff
     * @param keepDirName  имя каталога конфига CLI, который не исключается
     * @return текст патча; пустая строка, если изменений нет
     * @throws IOException при ошибке чтения файлов
     */
    public static String diff(Path original, Path modified, Set<String> excludeNames, String keepDirName)
            throws IOException {
        StringBuilder out = new StringBuilder();
        for (FileChangeDto change : changes(original, modified, excludeNames, keepDirName)) {
            out.append(change.getDiff());
        }
        return out.toString();
    }

    /**
     * Структурированный список изменений файлов агентом (для встраивания в лог запуска).
     *
     * @return изменения по файлам; пустой список, если ничего не изменилось
     * @throws IOException при ошибке чтения файлов
     */
    public static List<FileChangeDto> changes(Path original, Path modified, Set<String> excludeNames, String keepDirName)
            throws IOException {
        Path base = original.toAbsolutePath().normalize();
        Path mod = modified.toAbsolutePath().normalize();

        Set<String> relPaths = new TreeSet<>();
        relPaths.addAll(collectFiles(base, excludeNames, keepDirName));
        relPaths.addAll(collectFiles(mod, excludeNames, keepDirName));

        List<FileChangeDto> result = new ArrayList<>();
        for (String rel : relPaths) {
            Path a = base.resolve(rel);
            Path b = mod.resolve(rel);
            boolean inA = Files.isRegularFile(a);
            boolean inB = Files.isRegularFile(b);
            if (inA && inB) {
                if (contentEquals(a, b)) {
                    continue;
                }
                result.add(buildFileChange(rel, a, b));
            } else if (inA) {
                result.add(buildFileChange(rel, a, null));
            } else if (inB) {
                result.add(buildFileChange(rel, null, b));
            }
        }
        return result;
    }

    private static FileChangeDto buildFileChange(String rel, Path a, Path b) throws IOException {
        String changeType = a == null ? "added" : (b == null ? "deleted" : "modified");
        boolean binary = (a != null && isBinary(a)) || (b != null && isBinary(b));

        StringBuilder out = new StringBuilder();
        out.append("diff --git a/").append(rel).append(" b/").append(rel).append('\n');
        if (a == null) {
            out.append("new file\n");
        } else if (b == null) {
            out.append("deleted file\n");
        }
        out.append("--- ").append(a == null ? "/dev/null" : "a/" + rel).append('\n');
        out.append("+++ ").append(b == null ? "/dev/null" : "b/" + rel).append('\n');

        if (binary) {
            out.append("Binary files differ\n");
        } else {
            List<String> linesA = a == null ? List.of() : readLines(a);
            List<String> linesB = b == null ? List.of() : readLines(b);
            out.append(unifiedHunks(linesA, linesB));
        }
        return new FileChangeDto(rel, changeType, out.toString());
    }

    // ---- Сбор файлов ----

    private static Set<String> collectFiles(Path root, Set<String> excludeNames, String keepDirName)
            throws IOException {
        Set<String> result = new TreeSet<>();
        if (!Files.isDirectory(root)) {
            return result;
        }
        Files.walkFileTree(root, new java.nio.file.SimpleFileVisitor<>() {
            @Override
            public java.nio.file.FileVisitResult preVisitDirectory(
                    Path dir, java.nio.file.attribute.BasicFileAttributes attrs) {
                Path current = dir.toAbsolutePath().normalize();
                String name = dir.getFileName() == null ? "" : dir.getFileName().toString();
                if (!current.equals(root)
                        && excludeNames.contains(name)
                        && !name.equals(keepDirName)) {
                    return java.nio.file.FileVisitResult.SKIP_SUBTREE;
                }
                return java.nio.file.FileVisitResult.CONTINUE;
            }

            @Override
            public java.nio.file.FileVisitResult visitFile(
                    Path file, java.nio.file.attribute.BasicFileAttributes attrs) {
                if (attrs.isRegularFile()) {
                    result.add(root.relativize(file).toString().replace('\\', '/'));
                }
                return java.nio.file.FileVisitResult.CONTINUE;
            }

            @Override
            public java.nio.file.FileVisitResult visitFileFailed(Path file, IOException exc) {
                return java.nio.file.FileVisitResult.CONTINUE;
            }
        });
        return result;
    }

    // ---- Сравнение содержимого ----

    private static boolean contentEquals(Path a, Path b) throws IOException {
        return Files.mismatch(a, b) == -1L;
    }

    private static boolean isBinary(Path file) throws IOException {
        byte[] sample = new byte[4096];
        int read;
        try (var in = Files.newInputStream(file)) {
            read = in.read(sample);
        }
        for (int i = 0; i < read; i++) {
            if (sample[i] == 0) {
                return true;
            }
        }
        return false;
    }

    private static List<String> readLines(Path file) throws IOException {
        String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        if (content.isEmpty()) {
            return List.of();
        }
        // split, сохраняя пустую последнюю строку только если файл ею не заканчивается
        return new ArrayList<>(Arrays.asList(content.split("\n", -1)));
    }

    // ---- Unified diff по строкам (LCS) ----

    static String unifiedHunks(List<String> a, List<String> b) {
        List<int[]> lcs = lcs(a, b);
        // Строим список правок как последовательности операций.
        List<Edit> edits = buildEdits(a, b, lcs);
        return renderHunks(a, b, edits);
    }

    private enum Op { EQUAL, DELETE, INSERT }

    private static final class Edit {
        final Op op;
        final int aIndex;
        final int bIndex;

        Edit(Op op, int aIndex, int bIndex) {
            this.op = op;
            this.aIndex = aIndex;
            this.bIndex = bIndex;
        }
    }

    // Возвращает пары совпавших индексов (i в a, j в b) по возрастанию.
    private static List<int[]> lcs(List<String> a, List<String> b) {
        int n = a.size();
        int m = b.size();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                if (a.get(i).equals(b.get(j))) {
                    dp[i][j] = dp[i + 1][j + 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
                }
            }
        }
        List<int[]> pairs = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < n && j < m) {
            if (a.get(i).equals(b.get(j))) {
                pairs.add(new int[]{i, j});
                i++;
                j++;
            } else if (dp[i + 1][j] >= dp[i][j + 1]) {
                i++;
            } else {
                j++;
            }
        }
        return pairs;
    }

    private static List<Edit> buildEdits(List<String> a, List<String> b, List<int[]> lcs) {
        List<Edit> edits = new ArrayList<>();
        int i = 0;
        int j = 0;
        for (int[] pair : lcs) {
            while (i < pair[0]) {
                edits.add(new Edit(Op.DELETE, i, -1));
                i++;
            }
            while (j < pair[1]) {
                edits.add(new Edit(Op.INSERT, -1, j));
                j++;
            }
            edits.add(new Edit(Op.EQUAL, i, j));
            i++;
            j++;
        }
        while (i < a.size()) {
            edits.add(new Edit(Op.DELETE, i, -1));
            i++;
        }
        while (j < b.size()) {
            edits.add(new Edit(Op.INSERT, -1, j));
            j++;
        }
        return edits;
    }

    private static String renderHunks(List<String> a, List<String> b, List<Edit> edits) {
        StringBuilder out = new StringBuilder();
        int n = edits.size();
        int idx = 0;
        while (idx < n) {
            // ищем следующее изменение
            while (idx < n && edits.get(idx).op == Op.EQUAL) {
                idx++;
            }
            if (idx >= n) {
                break;
            }
            int hunkStart = Math.max(0, idx - CONTEXT);
            int changeEnd = idx;
            // расширяем до конца группы изменений с учётом контекста
            int k = idx;
            while (k < n) {
                if (edits.get(k).op != Op.EQUAL) {
                    changeEnd = k;
                    k++;
                } else {
                    // если после равного блока в пределах 2*CONTEXT снова изменение — не рвём хунк
                    int gap = 0;
                    int t = k;
                    while (t < n && edits.get(t).op == Op.EQUAL) {
                        gap++;
                        t++;
                    }
                    if (t < n && gap <= CONTEXT * 2) {
                        k = t;
                    } else {
                        break;
                    }
                }
            }
            int hunkEnd = Math.min(n, changeEnd + 1 + CONTEXT);
            out.append(renderHunk(a, b, edits, hunkStart, hunkEnd));
            idx = hunkEnd;
        }
        return out.toString();
    }

    private static String renderHunk(List<String> a, List<String> b, List<Edit> edits, int start, int end) {
        int aStart = -1;
        int bStart = -1;
        int aCount = 0;
        int bCount = 0;
        StringBuilder body = new StringBuilder();
        for (int i = start; i < end; i++) {
            Edit e = edits.get(i);
            switch (e.op) {
                case EQUAL:
                    if (aStart < 0) {
                        aStart = e.aIndex;
                        bStart = e.bIndex;
                    }
                    aCount++;
                    bCount++;
                    body.append(' ').append(a.get(e.aIndex)).append('\n');
                    break;
                case DELETE:
                    if (aStart < 0) {
                        aStart = e.aIndex;
                        bStart = 0;
                    }
                    aCount++;
                    body.append('-').append(a.get(e.aIndex)).append('\n');
                    break;
                case INSERT:
                    if (bStart < 0) {
                        bStart = e.bIndex;
                        aStart = aStart < 0 ? 0 : aStart;
                    }
                    bCount++;
                    body.append('+').append(b.get(e.bIndex)).append('\n');
                    break;
                default:
                    break;
            }
        }
        String header = "@@ -" + toRange(aStart, aCount) + " +" + toRange(bStart, bCount) + " @@\n";
        return header + body;
    }

    private static String toRange(int start, int count) {
        // unified diff нумерует строки с 1; пустой диапазон обозначается как start,0
        int displayStart = count == 0 ? start : start + 1;
        return displayStart + "," + count;
    }
}
