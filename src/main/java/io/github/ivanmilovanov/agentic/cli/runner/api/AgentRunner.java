package io.github.ivanmilovanov.agentic.cli.runner.api;

import io.github.ivanmilovanov.agentic.cli.runner.model.AgentResultDto;

/**
 * Запуск агентского CLI с произвольным промптом или с именованным скилом.
 */
public interface AgentRunner {

    /**
     * Выполняет промпт.
     *
     * @param prompt текст запроса
     * @return результат запуска: вывод, код возврата, разобранные события
     */
    AgentResultDto execute(String prompt) throws Exception;

    /**
     * Выполняет скил с переданным промптом.
     *
     * @param skillName имя скила
     * @param prompt    текст запроса для скила
     * @return результат запуска: вывод, код возврата, разобранные события
     */
    AgentResultDto executeSkill(String skillName, String prompt) throws Exception;
}
