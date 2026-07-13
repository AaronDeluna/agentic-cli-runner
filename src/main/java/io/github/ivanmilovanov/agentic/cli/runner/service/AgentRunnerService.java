package io.github.ivanmilovanov.agentic.cli.runner.service;

import io.github.ivanmilovanov.agentic.cli.runner.api.AgentRunner;
import io.github.ivanmilovanov.agentic.cli.runner.config.AgentRunnerProperties;
import io.github.ivanmilovanov.agentic.cli.runner.model.AgentResultDto;

import java.nio.file.Path;

/**
 * Точка входа библиотеки: собирает {@link AgentRunner} из настроек по умолчанию
 * и делегирует ему выполнение запросов.
 */
public class AgentRunnerService implements AgentRunner {

    private final AgentRunner agentRunner;

    /**
     * Создаёт сервис, выполняющий запросы в текущей рабочей директории (cwd).
     */
    public AgentRunnerService() {
        this(Path.of("").toAbsolutePath());
    }

    /**
     * Создаёт сервис, выполняющий запросы в указанной рабочей директории.
     *
     * @param workspace рабочая директория запуска CLI; может содержать {@code .qwen/} со скилами
     */
    public AgentRunnerService(Path workspace) {
        this(AgentRunnerFactory.defaultFactory(workspace).create(AgentRunnerProperties.loadDefault()));
    }

    AgentRunnerService(AgentRunner delegate) {
        this.agentRunner = delegate;
    }

    @Override
    public AgentResultDto execute(String prompt) throws Exception {
        return agentRunner.execute(prompt);
    }

    @Override
    public AgentResultDto executeSkill(String skillName, String prompt) throws Exception {
        return agentRunner.executeSkill(skillName, prompt);
    }
}
