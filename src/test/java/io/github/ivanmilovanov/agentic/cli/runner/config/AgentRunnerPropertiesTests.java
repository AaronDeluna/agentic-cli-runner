package io.github.ivanmilovanov.agentic.cli.runner.config;

import io.github.ivanmilovanov.agentic.cli.runner.exception.AgentRunnerConfigurationException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentRunnerPropertiesTests {

    @Test
    void loadsAgentCliNameFromClasspathProperties() {
        Properties properties = AgentRunnerProperties.loadDefault();

        assertThat(AgentRunnerProperties.getCliName(properties)).isEqualTo("qwen");
    }

    @Test
    void loadsArgsAsCommaSeparatedList() {
        Properties properties = new Properties();
        properties.setProperty("agent.cli.qwen.args", "--output-format,stream-json,--approval-mode,yolo");

        assertThat(AgentRunnerProperties.getArgs(properties, "qwen"))
                .containsExactly("--output-format", "stream-json", "--approval-mode", "yolo");
    }

    @Test
    void getArgsReturnsEmptyListWhenPropertyMissing() {
        assertThat(AgentRunnerProperties.getArgs(new Properties(), "qwen")).isEmpty();
    }

    @Test
    void expandsEnvVariablesInFallbackPaths() {
        String home = System.getenv("HOME");
        org.junit.jupiter.api.Assumptions.assumeTrue(home != null && !home.isBlank());

        Properties properties = new Properties();
        properties.setProperty("agent.cli.qwen.fallback.mac", "${env.HOME}/.local/bin");

        List<Path> fallbackPaths = AgentRunnerProperties.getFallbackPaths(properties, "qwen", OsType.MAC);

        assertThat(fallbackPaths).containsExactly(Path.of(home, ".local", "bin"));
    }

    @Test
    void getFallbackPathsSplitsMultiplePathsBySemicolon() {
        Properties properties = new Properties();
        properties.setProperty("agent.cli.qwen.fallback.linux", "/opt/qwen;/usr/local/bin");

        List<Path> fallbackPaths = AgentRunnerProperties.getFallbackPaths(properties, "qwen", OsType.LINUX);

        assertThat(fallbackPaths).containsExactly(Path.of("/opt/qwen"), Path.of("/usr/local/bin"));
    }

    @Test
    void getPrefixIsOnlyPopulatedForWindows() {
        Properties properties = new Properties();
        properties.setProperty("agent.cli.qwen.prefix.windows", "cmd,/c");

        assertThat(AgentRunnerProperties.getPrefix(properties, "qwen", OsType.WINDOWS)).containsExactly("cmd", "/c");
        assertThat(AgentRunnerProperties.getPrefix(properties, "qwen", OsType.MAC)).isEmpty();
    }

    @Test
    void getTimeoutReturnsDefaultWhenPropertyMissing() {
        Duration fallback = Duration.ofMinutes(15);

        assertThat(AgentRunnerProperties.getTimeout(new Properties(), fallback)).isEqualTo(fallback);
    }

    @Test
    void getTimeoutFromPropertyOverridesDefault() {
        Properties properties = new Properties();
        properties.setProperty("agent.timeout", "30");

        assertThat(AgentRunnerProperties.getTimeout(properties, Duration.ofMinutes(15)))
                .isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void getTimeoutRejectsNonNumericValue() {
        Properties properties = new Properties();
        properties.setProperty("agent.timeout", "abc");

        assertThatThrownBy(() -> AgentRunnerProperties.getTimeout(properties, Duration.ofMinutes(15)))
                .isInstanceOf(AgentRunnerConfigurationException.class);
    }

    @Test
    void getTimeoutRejectsNonPositiveValue() {
        Properties properties = new Properties();
        properties.setProperty("agent.timeout", "0");

        assertThatThrownBy(() -> AgentRunnerProperties.getTimeout(properties, Duration.ofMinutes(15)))
                .isInstanceOf(AgentRunnerConfigurationException.class);
    }

    @Test
    void sandboxIsDisabledByDefault() {
        assertThat(AgentRunnerProperties.isSandbox(new Properties())).isFalse();
    }

    @Test
    void sandboxIsEnabledWhenPropertyTrue() {
        Properties properties = new Properties();
        properties.setProperty("agent.sandbox", "true");

        assertThat(AgentRunnerProperties.isSandbox(properties)).isTrue();
    }

    @Test
    void osEnforcementIsEnabledByDefault() {
        assertThat(AgentRunnerProperties.isSandboxOsEnforcement(new Properties())).isTrue();
    }

    @Test
    void osEnforcementCanBeDisabled() {
        Properties properties = new Properties();
        properties.setProperty("agent.sandbox.os-enforcement", "false");

        assertThat(AgentRunnerProperties.isSandboxOsEnforcement(properties)).isFalse();
    }

    @Test
    void sandboxExcludesFallBackToDefaultWhenPropertyMissing() {
        assertThat(AgentRunnerProperties.getSandboxExcludes(new Properties()))
                .isEqualTo(AgentRunnerProperties.DEFAULT_SANDBOX_EXCLUDES);
    }

    @Test
    void sandboxExcludesParsedAsCommaSeparatedList() {
        Properties properties = new Properties();
        properties.setProperty("agent.sandbox.exclude", ".git, node_modules ,target");

        assertThat(AgentRunnerProperties.getSandboxExcludes(properties))
                .containsExactly(".git", "node_modules", "target");
    }

    @Test
    void sandboxExcludesEmptyWhenPropertyBlank() {
        Properties properties = new Properties();
        properties.setProperty("agent.sandbox.exclude", "");

        assertThat(AgentRunnerProperties.getSandboxExcludes(properties)).isEmpty();
    }
}
