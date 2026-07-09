package io.github.ivanmilovanov.agentic.cli.runner.cli;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StreamJsonCommandFactoryTests {

    private static final String RESOLVED_QWEN = "/usr/local/bin/qwen";
    private static final List<String> QWEN_ARGS =
            List.of("--output-format", "stream-json", "--approval-mode", "yolo");

    @Test
    void buildsCommandWithExecutableArgsAndPromptLast() {
        CommandResolver resolver = mock(CommandResolver.class);
        when(resolver.resolveExecutable("qwen")).thenReturn(RESOLVED_QWEN);

        StreamJsonCommandFactory factory =
                new StreamJsonCommandFactory(resolver, "qwen", QWEN_ARGS, List.of());

        List<String> command = factory.buildCommand("do something");

        assertThat(command.get(0)).isEqualTo(RESOLVED_QWEN);
        assertThat(command).containsSequence("--output-format", "stream-json");
        assertThat(command.get(command.size() - 1)).isEqualTo("do something");
    }

    @Test
    void resolvesExecutableByGivenName() {
        CommandResolver resolver = mock(CommandResolver.class);
        when(resolver.resolveExecutable("some-cli")).thenReturn("/opt/bin/some-cli");

        StreamJsonCommandFactory factory =
                new StreamJsonCommandFactory(resolver, "some-cli", QWEN_ARGS, List.of());

        List<String> command = factory.buildCommand("prompt");

        assertThat(command.get(0)).isEqualTo("/opt/bin/some-cli");
    }

    @Test
    void prependsPrefixWhenConfigured() {
        CommandResolver resolver = mock(CommandResolver.class);
        when(resolver.resolveExecutable("qwen")).thenReturn(RESOLVED_QWEN);

        StreamJsonCommandFactory factory =
                new StreamJsonCommandFactory(resolver, "qwen", QWEN_ARGS, List.of("cmd", "/c"));

        List<String> command = factory.buildCommand("prompt");

        assertThat(command.get(0)).isEqualTo("cmd");
        assertThat(command.get(1)).isEqualTo("/c");
        assertThat(command.get(2)).isEqualTo(RESOLVED_QWEN);
    }

    @Test
    void commandIsUnmodifiable() {
        CommandResolver resolver = mock(CommandResolver.class);
        when(resolver.resolveExecutable("qwen")).thenReturn(RESOLVED_QWEN);

        StreamJsonCommandFactory factory =
                new StreamJsonCommandFactory(resolver, "qwen", QWEN_ARGS, List.of());
        List<String> command = factory.buildCommand("prompt");

        assertThat(command).isUnmodifiable();
    }
}
