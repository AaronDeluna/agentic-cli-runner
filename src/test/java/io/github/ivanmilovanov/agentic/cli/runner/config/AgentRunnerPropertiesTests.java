package io.github.ivanmilovanov.agentic.cli.runner.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRunnerPropertiesTests {

    @Test
    void loadsAgentCliFromClasspathProperties() {
        Properties properties = AgentRunnerProperties.loadDefault();

        assertThat(AgentRunnerProperties.getCliName(properties)).isEqualTo("qwen");
    }

    @Test
    void expandsEnvVariablesInFallbackPaths() {
        String home = System.getenv("HOME");
        assumeHomeIsSet(home);

        Properties properties = new Properties();
        properties.setProperty("agent.cli.fallback.mac", "${env.HOME}/.local/bin");

        List<Path> fallbackPaths = AgentRunnerProperties.getFallbackPaths(properties, OsType.MAC);

        assertThat(fallbackPaths).containsExactly(Path.of(home, ".local", "bin"));
    }

    @Test
    void getFallbackPathsReturnsEmptyListWhenPropertyMissing() {
        Properties properties = new Properties();

        assertThat(AgentRunnerProperties.getFallbackPaths(properties, OsType.LINUX)).isEmpty();
    }

    @Test
    void getFallbackPathsSplitsMultiplePathsBySemicolon() {
        Properties properties = new Properties();
        properties.setProperty("agent.cli.fallback.linux", "/opt/qwen;/usr/local/bin");

        List<Path> fallbackPaths = AgentRunnerProperties.getFallbackPaths(properties, OsType.LINUX);

        assertThat(fallbackPaths).containsExactly(Path.of("/opt/qwen"), Path.of("/usr/local/bin"));
    }

    @Test
    void getPrefixIsOnlyPopulatedForWindows() {
        Properties properties = new Properties();
        properties.setProperty("agent.cli.prefix.windows", "cmd,/c");

        assertThat(AgentRunnerProperties.getPrefix(properties, OsType.WINDOWS)).containsExactly("cmd", "/c");
        assertThat(AgentRunnerProperties.getPrefix(properties, OsType.MAC)).isEmpty();
        assertThat(AgentRunnerProperties.getPrefix(properties, OsType.LINUX)).isEmpty();
    }

    private static void assumeHomeIsSet(String home) {
        org.junit.jupiter.api.Assumptions.assumeTrue(home != null && !home.isBlank());
    }
}
