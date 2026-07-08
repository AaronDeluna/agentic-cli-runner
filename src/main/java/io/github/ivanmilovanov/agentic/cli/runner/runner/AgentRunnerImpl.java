package io.github.ivanmilovanov.agentic.cli.runner.runner;

import io.github.ivanmilovanov.agentic.cli.runner.api.AgentRunner;
import io.github.ivanmilovanov.agentic.cli.runner.cli.CommandFactory;
import io.github.ivanmilovanov.agentic.cli.runner.context.AgentRunContext;
import io.github.ivanmilovanov.agentic.cli.runner.exception.InvalidSkillNameException;
import io.github.ivanmilovanov.agentic.cli.runner.executor.CommandExecutor;
import io.github.ivanmilovanov.agentic.cli.runner.log.RunnerLogWriter;
import io.github.ivanmilovanov.agentic.cli.runner.model.AgentLogDto;
import io.github.ivanmilovanov.agentic.cli.runner.model.AgentResultDto;
import io.github.ivanmilovanov.agentic.cli.runner.model.AgentRunLogDto;
import io.github.ivanmilovanov.agentic.cli.runner.model.CommandRequestDto;
import io.github.ivanmilovanov.agentic.cli.runner.model.CommandResultDto;
import io.github.ivanmilovanov.agentic.cli.runner.parser.AgentStreamJsonParser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Реализация {@link AgentRunner} для агентских CLI со stream-json выводом:
 * строит команду, запускает процесс, разбирает вывод и сохраняет лог запуска.
 */
@Slf4j
public class AgentRunnerImpl implements AgentRunner {

    /** Таймаут выполнения по умолчанию, если явно не задан другой. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(3);

    private final CommandExecutor commandExecutor;
    private final AgentStreamJsonParser agentStreamJsonParser;
    private final RunnerLogWriter runnerLogWriter;
    private final Duration timeout;
    private final CommandFactory commandFactory;
    @Getter
    private final AgentRunContext agentRunContext;

    public AgentRunnerImpl(
            CommandExecutor commandExecutor,
            AgentStreamJsonParser agentStreamJsonParser,
            RunnerLogWriter runnerLogWriter,
            Path workingDirectory,
            Duration timeout,
            CommandFactory commandFactory
    ) {
        this.commandExecutor = commandExecutor;
        this.agentStreamJsonParser = agentStreamJsonParser;
        this.runnerLogWriter = runnerLogWriter;
        this.agentRunContext = new AgentRunContext(workingDirectory);
        this.timeout = timeout;
        this.commandFactory = commandFactory;
    }

    @Override
    public AgentResultDto execute(String prompt) throws Exception {
        return run(null, prompt);
    }

    /**
     * @throws InvalidSkillNameException если имя скила пустое или содержит фрагменты пути
     */
    @Override
    public AgentResultDto executeSkill(String skillName, String prompt) throws Exception {
        log.info("[SKILL_EXECUTION]: {}", skillName);
        validateSkillName(skillName);
        return run(skillName, "/" + skillName + " " + prompt);
    }

    private AgentResultDto run(String skillName, String prompt) throws Exception {
        log.info("[USER_QUERY]: {}", prompt);
        Files.createDirectories(agentRunContext.getRunDir());

        List<String> command = commandFactory.buildCommand(prompt, agentRunContext.getRunDir());

        Instant startedAt = Instant.now();
        CommandResultDto result = commandExecutor.execute(new CommandRequestDto(
                command,
                agentRunContext.getWorkspace(),
                timeout
        ));
        Instant finishedAt = Instant.now();

        AgentLogDto agentLog = agentStreamJsonParser.parse(result.getStdout());
        log.info("[AGENT_RESPONSE]: \n{}", agentLog.getEventsJson());
        AgentResultDto agentResult = new AgentResultDto(
                result.getStdout(),
                result.getStderr(),
                result.getExitCode(),
                result.isTimedOut(),
                agentLog.getEvents(),
                agentLog.getEventsJson(),
                agentLog.getFinalResult()
        );

        AgentRunLogDto logEntry = AgentRunLogDto.builder()
                .runId(agentRunContext.getRunId())
                .startedAt(startedAt.toString())
                .finishedAt(finishedAt.toString())
                .skillName(skillName)
                .finalResult(agentResult.getFinalResult())
                .events(agentResult.getEvents())
                .build();
        runnerLogWriter.write(agentRunContext, logEntry);

        return agentResult;
    }

    private static void validateSkillName(String skillName) {
        if (skillName == null || skillName.isBlank()) {
            throw new InvalidSkillNameException("Skill name must not be blank");
        }
        if (skillName.contains("/") || skillName.contains("\\") || skillName.contains("..")) {
            throw new InvalidSkillNameException("Skill name must not contain path fragments: " + skillName);
        }
    }
}
