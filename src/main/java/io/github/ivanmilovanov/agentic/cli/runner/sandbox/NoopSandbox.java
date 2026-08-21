package io.github.ivanmilovanov.agentic.cli.runner.sandbox;

import io.github.ivanmilovanov.agentic.cli.runner.context.AgentRunContext;

import java.nio.file.Path;

/**
 * Пустая песочница: агент работает прямо в исходной рабочей директории.
 * Используется, когда {@code agent.sandbox=false} — поведение как до появления фичи.
 */
public class NoopSandbox implements Sandbox {

    /**
     * @return исходная рабочая область без изменений
     */
    @Override
    public Path prepare(AgentRunContext context) {
        return context.getWorkspace();
    }
}
