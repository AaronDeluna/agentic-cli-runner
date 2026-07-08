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

        assertThat(properties.getProperty(AgentRunnerProperties.CLI_PROPERTY)).isEqualTo("QWEN");
    }

    @Test
    void loadsBaseArgsAsCommaSeparatedList() {
        Properties properties = AgentRunnerProperties.loadDefault();

        List<String> baseArgs = AgentRunnerProperties.getBaseArgs(properties, "qwen");

        assertThat(baseArgs).containsExactly("--output-format", "stream-json", "--approval-mode", "yolo");
    }

    @Test
    void getBaseArgsReturnsEmptyListWhenPropertyMissing() {
        Properties properties = new Properties();

        assertThat(AgentRunnerProperties.getBaseArgs(properties, "qwen")).isEmpty();
    }

    @Test
    void expandsEnvVariablesInFallbackPaths() {
        String home = System.getenv("HOME");
        assumeHomeIsSet(home);

        Properties properties = new Properties();
        properties.setProperty("agent.cli.qwen.fallback.mac", "${env.HOME}/.local/bin");

        List<Path> fallbackPaths = AgentRunnerProperties.getFallbackPaths(properties, "qwen", OsType.MAC);

        assertThat(fallbackPaths).containsExactly(Path.of(home, ".local", "bin"));
    }

    @Test
    void getFallbackPathsReturnsEmptyListWhenPropertyMissing() {
        Properties properties = new Properties();

        assertThat(AgentRunnerProperties.getFallbackPaths(properties, "qwen", OsType.LINUX)).isEmpty();
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
        assertThat(AgentRunnerProperties.getPrefix(properties, "qwen", OsType.LINUX)).isEmpty();
    }

    private static void assumeHomeIsSet(String home) {
        org.junit.jupiter.api.Assumptions.assumeTrue(home != null && !home.isBlank());
    }
}
