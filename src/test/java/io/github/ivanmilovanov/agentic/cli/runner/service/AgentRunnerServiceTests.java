package io.github.ivanmilovanov.agentic.cli.runner.service;

import io.github.ivanmilovanov.agentic.cli.runner.api.AgentRunner;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AgentRunnerServiceTests {

    @Test
    void noArgConstructorBuildsServiceUsingCurrentDirectory() {
        // Конфигурация CLI резолвится лениво (в buildCommand), поэтому сборку сервиса
        // можно проверить без установленного CLI: конструктор не должен падать.
        assertThatCode(AgentRunnerService::new).doesNotThrowAnyException();
    }

    @Test
    void explicitWorkspaceConstructorBuildsService(@org.junit.jupiter.api.io.TempDir Path workspace) {
        AgentRunner runner = new AgentRunnerService(workspace);

        assertThat(runner).isInstanceOf(AgentRunner.class);
    }
}
