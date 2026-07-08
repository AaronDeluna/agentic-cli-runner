package io.github.ivanmilovanov.agentic.cli.runner.api;

import io.github.ivanmilovanov.agentic.cli.runner.model.AgentResultDto;

/**
 * Запускает агентский CLI (например, Qwen) с промптом пользователя или именованным скилом.
 */
public interface AgentRunner {

    /** Запускает агента с произвольным пользовательским промптом. */
    AgentResultDto execute(String prompt) throws Exception;

    /** Запускает агента с указанным скилом и промптом для него. */
    AgentResultDto executeSkill(String skillName, String prompt) throws Exception;
}
