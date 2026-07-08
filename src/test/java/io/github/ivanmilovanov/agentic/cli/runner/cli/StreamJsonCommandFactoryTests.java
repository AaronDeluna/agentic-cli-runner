package io.github.ivanmilovanov.agentic.cli.runner.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StreamJsonCommandFactoryTests {

    private static final String RESOLVED_QWEN = "/usr/local/bin/qwen";
    private static final List<String> QWEN_ARGS =
            List.of("--output-format", "stream-json", "--approval-mode", "yolo");

    @Test
    void buildsCommandWithExecutableArgsAndPromptLast(@TempDir Path logDir) {
        CommandResolver resolver = mock(CommandResolver.class);
        when(resolver.resolveExecutable("qwen")).thenReturn(RESOLVED_QWEN);

        StreamJsonCommandFactory factory =
                new StreamJsonCommandFactory(resolver, "qwen", QWEN_ARGS, true, List.of());

        List<String> command = factory.buildCommand("do something", logDir);

        assertThat(command.get(0)).isEqualTo(RESOLVED_QWEN);
        assertThat(command).containsSequence("--output-format", "stream-json");
        assertThat(command.get(command.size() - 1)).isEqualTo("do something");
    }

    @Test
    void resolvesExecutableByGivenName() {
        CommandResolver resolver = mock(CommandResolver.class);
        when(resolver.resolveExecutable("some-cli")).thenReturn("/opt/bin/some-cli");

        StreamJsonCommandFactory factory =
                new StreamJsonCommandFactory(resolver, "some-cli", QWEN_ARGS, true, List.of());

        List<String> command = factory.buildCommand("prompt", null);

        assertThat(command.get(0)).isEqualTo("/opt/bin/some-cli");
    }

    @Test
    void addsLoggingArgsWhenSupportedAndLogDirProvided(@TempDir Path logDir) {
        CommandResolver resolver = mock(CommandResolver.class);
        when(resolver.resolveExecutable("qwen")).thenReturn(RESOLVED_QWEN);

        StreamJsonCommandFactory factory =
                new StreamJsonCommandFactory(resolver, "qwen", QWEN_ARGS, true, List.of());

        List<String> command = factory.buildCommand("prompt text", logDir);

        assertThat(command).containsSequence("--openai-logging", "true");
        assertThat(command).containsSequence("--openai-logging-dir", logDir.toAbsolutePath().toString());
    }

    @Test
    void omitsLoggingArgsWhenNotSupported(@TempDir Path logDir) {
        CommandResolver resolver = mock(CommandResolver.class);
        when(resolver.resolveExecutable("codex")).thenReturn("/opt/bin/codex");

        StreamJsonCommandFactory factory =
                new StreamJsonCommandFactory(resolver, "codex", List.of("exec", "--json"), false, List.of());

        List<String> command = factory.buildCommand("prompt text", logDir);

        assertThat(command).doesNotContain("--openai-logging");
    }

    @Test
    void omitsLoggingArgsWhenLogDirIsNull() {
        CommandResolver resolver = mock(CommandResolver.class);
        when(resolver.resolveExecutable("qwen")).thenReturn(RESOLVED_QWEN);

        StreamJsonCommandFactory factory =
                new StreamJsonCommandFactory(resolver, "qwen", QWEN_ARGS, true, List.of());

        List<String> command = factory.buildCommand("prompt text", null);

        assertThat(command).doesNotContain("--openai-logging");
        assertThat(command.get(command.size() - 1)).isEqualTo("prompt text");
    }

    @Test
    void prependsPrefixWhenConfigured(@TempDir Path logDir) {
        CommandResolver resolver = mock(CommandResolver.class);
        when(resolver.resolveExecutable("qwen")).thenReturn(RESOLVED_QWEN);

        StreamJsonCommandFactory factory =
                new StreamJsonCommandFactory(resolver, "qwen", QWEN_ARGS, true, List.of("cmd", "/c"));

        List<String> command = factory.buildCommand("prompt", logDir);

        assertThat(command.get(0)).isEqualTo("cmd");
        assertThat(command.get(1)).isEqualTo("/c");
        assertThat(command.get(2)).isEqualTo(RESOLVED_QWEN);
    }

    @Test
    void commandIsUnmodifiable(@TempDir Path logDir) {
        CommandResolver resolver = mock(CommandResolver.class);
        when(resolver.resolveExecutable("qwen")).thenReturn(RESOLVED_QWEN);

        StreamJsonCommandFactory factory =
                new StreamJsonCommandFactory(resolver, "qwen", QWEN_ARGS, true, List.of());
        List<String> command = factory.buildCommand("prompt", logDir);

        assertThat(command).isUnmodifiable();
    }
}
