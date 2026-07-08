package io.github.ivanmilovanov.agentic.cli.runner.service;

import io.github.ivanmilovanov.agentic.cli.runner.api.AgentRunner;
import io.github.ivanmilovanov.agentic.cli.runner.config.AgentRunnerProperties;
import io.github.ivanmilovanov.agentic.cli.runner.model.AgentResultDto;

import java.nio.file.Path;

/**
 * Точка входа для использования библиотеки: собирает {@link AgentRunner} из настроек
 * по умолчанию и делегирует ему выполнение запросов.
 */
public class AgentRunnerService implements AgentRunner {

    private final AgentRunner agentRunner;

    /**
     * Запускает агента в указанной рабочей области (workspace).
     * Внутри workspace может находиться директория {@code .qwen/} со скилами.
     *
     * @param workspace путь к рабочей области
     */
    public AgentRunnerService(Path workspace) {
        this(AgentRunnerFactory.defaultFactory(workspace).create(AgentRunnerProperties.loadDefault()));
    }

    AgentRunnerService(AgentRunner delegate) {
        this.agentRunner = delegate;
    }

    @Override
    public AgentResultDto executeUserPrompt(String prompt) throws Exception {
        return agentRunner.executeUserPrompt(prompt);
    }

    @Override
    public AgentResultDto executeSkillPrompt(String skillName, String prompt) throws Exception {
        return agentRunner.executeSkillPrompt(skillName, prompt);
    }
}
