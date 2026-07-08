package io.github.ivanmilovanov.agentic.cli.runner.cli;

import io.github.ivanmilovanov.agentic.cli.runner.config.OsType;
import io.github.ivanmilovanov.agentic.cli.runner.exception.CommandNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Env-var lookups in {@link OsAwareCommandResolver} read directly from the process
 * environment, so these tests mutate it via the well-known ProcessEnvironment reflection
 * trick. Requires the test JVM to be started with
 * {@code --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED}
 * (configured in the surefire plugin).
 */
class OsAwareCommandResolverTests {

    private static final String ENV_KEY = "QWEN_PATH";
    // Command names unique enough to never collide with a real binary already on PATH.
    private static final String FALLBACK_ONLY_COMMAND = "qwen-fallback-fixture";
    private static final String DIRECT_FILE_COMMAND = "qwen-direct-fixture";
    private static final String MISSING_COMMAND = "qwen-definitely-missing-fixture";

    @AfterEach
    void clearEnv() throws Exception {
        mutateEnv(map -> map.remove(ENV_KEY));
    }

    @Test
    void envVarTakesPriorityOverFallbackPaths(@TempDir Path tempDir) throws Exception {
        // QWEN_PATH is only consulted for the "qwen" command (env key = "<COMMAND>_PATH").
        Path envExecutable = createExecutable(tempDir, "qwen-from-env");
        Path fallbackExecutable = createExecutable(tempDir, "qwen");
        mutateEnv(map -> map.put(ENV_KEY, envExecutable.toString()));

        Map<OsType, List<Path>> fallbacks = Map.of(OsType.detect(), List.of(tempDir));
        OsAwareCommandResolver resolver = new OsAwareCommandResolver(fallbacks);

        String resolved = resolver.resolveExecutable("qwen");

        assertThat(resolved).isEqualTo(envExecutable.toString());
        assertThat(resolved).isNotEqualTo(fallbackExecutable.toString());
    }

    @Test
    void fallsBackToConfiguredDirectoryWhenEnvVarNotSet(@TempDir Path tempDir) throws Exception {
        Path fallbackExecutable = createExecutable(tempDir, FALLBACK_ONLY_COMMAND);

        Map<OsType, List<Path>> fallbacks = Map.of(OsType.detect(), List.of(tempDir));
        OsAwareCommandResolver resolver = new OsAwareCommandResolver(fallbacks);

        String resolved = resolver.resolveExecutable(FALLBACK_ONLY_COMMAND);

        assertThat(resolved).isEqualTo(fallbackExecutable.toString());
    }

    @Test
    void resolvesFallbackFileDirectlyWhenPathPointsToExecutableFile(@TempDir Path tempDir) throws Exception {
        Path directExecutable = createExecutable(tempDir, "some-file-name");

        Map<OsType, List<Path>> fallbacks = Map.of(OsType.detect(), List.of(directExecutable));
        OsAwareCommandResolver resolver = new OsAwareCommandResolver(fallbacks);

        String resolved = resolver.resolveExecutable(DIRECT_FILE_COMMAND);

        assertThat(resolved).isEqualTo(directExecutable.toString());
    }

    @Test
    void throwsCommandNotFoundExceptionWhenNothingResolvable(@TempDir Path tempDir) {
        Map<OsType, List<Path>> fallbacks = Map.of(OsType.detect(), List.of(tempDir));
        OsAwareCommandResolver resolver = new OsAwareCommandResolver(fallbacks);

        assertThatThrownBy(() -> resolver.resolveExecutable(MISSING_COMMAND))
                .isInstanceOf(CommandNotFoundException.class)
                .hasMessageContaining(MISSING_COMMAND);
    }

    private static Path createExecutable(Path dir, String name) throws IOException {
        Path file = dir.resolve(name);
        Files.writeString(file, "#!/bin/sh\necho test\n");
        try {
            Set<PosixFilePermission> perms = EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE
            );
            Files.setPosixFilePermissions(file, perms);
        } catch (UnsupportedOperationException e) {
            file.toFile().setExecutable(true);
        }
        return file;
    }

    @FunctionalInterface
    private interface EnvMutation {
        void apply(Map<String, String> env);
    }

    @SuppressWarnings("unchecked")
    private static void mutateEnv(EnvMutation mutation) throws Exception {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            mutation.apply((Map<String, String>) theEnvironmentField.get(null));

            Field theCaseInsensitiveEnvironmentField =
                    processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            mutation.apply((Map<String, String>) theCaseInsensitiveEnvironmentField.get(null));
        } catch (NoSuchFieldException e) {
            // Non-Windows JDKs don't expose theCaseInsensitiveEnvironment; fall back to
            // unwrapping the UnmodifiableMap returned by System.getenv().
            Map<String, String> env = System.getenv();
            for (Class<?> cl : Collections.class.getDeclaredClasses()) {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    mutation.apply((Map<String, String>) field.get(env));
                }
            }
        }
    }
}
