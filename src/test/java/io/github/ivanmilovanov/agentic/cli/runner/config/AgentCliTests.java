package io.github.ivanmilovanov.agentic.cli.runner.config;

import io.github.ivanmilovanov.agentic.cli.runner.exception.MissingAgentCliException;
import io.github.ivanmilovanov.agentic.cli.runner.exception.UnsupportedAgentCliException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentCliTests {

    @Test
    void resolvesQwenFromProperty() {
        assertThat(AgentCli.fromProperty("QWEN")).isEqualTo(AgentCli.QWEN);
    }

    @Test
    void resolutionIsCaseInsensitiveAndTrimmed() {
        assertThat(AgentCli.fromProperty("  qwen  ")).isEqualTo(AgentCli.QWEN);
    }

    @Test
    void nullValueThrowsMissingAgentCliException() {
        assertThatThrownBy(() -> AgentCli.fromProperty(null))
                .isInstanceOf(MissingAgentCliException.class);
    }

    @Test
    void blankValueThrowsMissingAgentCliException() {
        assertThatThrownBy(() -> AgentCli.fromProperty("   "))
                .isInstanceOf(MissingAgentCliException.class);
    }

    @Test
    void unknownValueThrowsUnsupportedAgentCliException() {
        assertThatThrownBy(() -> AgentCli.fromProperty("UNKNOWN"))
                .isInstanceOf(UnsupportedAgentCliException.class)
                .hasMessageContaining("UNKNOWN");
    }
}
