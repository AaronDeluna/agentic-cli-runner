package io.github.ivanmilovanov.agentic.cli.runner.cli.qwen;

import io.github.ivanmilovanov.agentic.cli.runner.cli.CommandResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QwenCommandFactoryImplTests {

    private static final String RESOLVED_EXECUTABLE = "/usr/local/bin/qwen";
    private static final List<String> BASE_ARGS =
            List.of("--output-format", "stream-json", "--approval-mode", "yolo");

    @Test
    void buildsCommandWithResolvedExecutableBaseArgsAndPromptLast(@TempDir Path logDir) {
        CommandResolver resolver = mock(CommandResolver.class);
        when(resolver.resolveExecutable("qwen")).thenReturn(RESOLVED_EXECUTABLE);

        QwenCommandFactoryImpl factory = new QwenCommandFactoryImpl(resolver, BASE_ARGS, List.of());

        List<String> command = factory.buildCommand("do something", logDir);

        assertThat(command.get(0)).isEqualTo(RESOLVED_EXECUTABLE);
        assertThat(command).containsSequence("--output-format", "stream-json");
        assertThat(command).containsSequence("--approval-mode", "yolo");
        assertThat(command.get(command.size() - 1)).isEqualTo("do something");
    }

    @Test
    void addsLoggingArgsWhenLogDirProvided(@TempDir Path logDir) {
        CommandResolver resolver = mock(CommandResolver.class);
        when(resolver.resolveExecutable("qwen")).thenReturn(RESOLVED_EXECUTABLE);

        QwenCommandFactoryImpl factory = new QwenCommandFactoryImpl(resolver, BASE_ARGS, List.of());

        List<String> command = factory.buildCommand("prompt text", logDir);

        assertThat(command).containsSequence("--openai-logging", "true");
        assertThat(command).containsSequence("--openai-logging-dir", logDir.toAbsolutePath().toString());
    }

    @Test
    void omitsLoggingArgsWhenLogDirIsNull() {
        CommandResolver resolver = mock(CommandResolver.class);
        when(resolver.resolveExecutable("qwen")).thenReturn(RESOLVED_EXECUTABLE);

        QwenCommandFactoryImpl factory = new QwenCommandFactoryImpl(resolver, BASE_ARGS, List.of());

        List<String> command = factory.buildCommand("prompt text", null);

        assertThat(command).doesNotContain("--openai-logging");
        assertThat(command.get(command.size() - 1)).isEqualTo("prompt text");
    }

    @Test
    void prependsPrefixWhenConfigured(@TempDir Path logDir) {
        CommandResolver resolver = mock(CommandResolver.class);
        when(resolver.resolveExecutable("qwen")).thenReturn(RESOLVED_EXECUTABLE);

        List<String> prefix = List.of("cmd", "/c");
        QwenCommandFactoryImpl factory = new QwenCommandFactoryImpl(resolver, BASE_ARGS, prefix);

        List<String> command = factory.buildCommand("prompt", logDir);

        assertThat(command.get(0)).isEqualTo("cmd");
        assertThat(command.get(1)).isEqualTo("/c");
        assertThat(command.get(2)).isEqualTo(RESOLVED_EXECUTABLE);
    }

    @Test
    void commandIsUnmodifiable(@TempDir Path logDir) {
        CommandResolver resolver = mock(CommandResolver.class);
        when(resolver.resolveExecutable("qwen")).thenReturn(RESOLVED_EXECUTABLE);

        QwenCommandFactoryImpl factory = new QwenCommandFactoryImpl(resolver, BASE_ARGS, List.of());
        List<String> command = factory.buildCommand("prompt", logDir);

        assertThat(command).isUnmodifiable();
    }
}
