package io.github.ivanmilovanov.agentic.cli.runner.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

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
}
