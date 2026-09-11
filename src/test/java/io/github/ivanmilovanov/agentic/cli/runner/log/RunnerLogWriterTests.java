package io.github.ivanmilovanov.agentic.cli.runner.log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ivanmilovanov.agentic.cli.runner.config.AgentLogLevel;
import io.github.ivanmilovanov.agentic.cli.runner.context.AgentRunContext;
import io.github.ivanmilovanov.agentic.cli.runner.model.AgentRunLogDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RunnerLogWriterTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RunnerLogWriter writer = new RunnerLogWriter();

    private static final String EVENT_JSON = """
            {
              "type": "assistant",
              "uuid": "3e8533be-0fcf-4060-91c5-331f76be62d0",
              "session_id": "336c2995-1fd8-4cc6-b3fe-3e0f0d5bd736",
              "message": {
                "role": "assistant",
                "usage": { "input_tokens": 18869, "output_tokens": 34, "total_tokens": 18903 }
              }
            }
            """;

    @Test
    void fullLevelKeepsServiceFields(@TempDir Path tempDir) throws Exception {
        Path logFile = tempDir.resolve("run.json");
        AgentRunContext context = contextWithLogFile(logFile);

        writer.write(context, entryWithEvent(), AgentLogLevel.FULL);

        JsonNode written = objectMapper.readTree(logFile.toFile());
        JsonNode event = written.get("events").get(0);
        assertThat(event.has("uuid")).isTrue();
        assertThat(event.has("session_id")).isTrue();
        assertThat(event.path("message").has("usage")).isTrue();
    }

    @Test
    void compactLevelStripsServiceFieldsRecursively(@TempDir Path tempDir) throws Exception {
        Path logFile = tempDir.resolve("run.json");
        AgentRunContext context = contextWithLogFile(logFile);

        writer.write(context, entryWithEvent(), AgentLogLevel.COMPACT);

        JsonNode written = objectMapper.readTree(logFile.toFile());
        JsonNode event = written.get("events").get(0);
        assertThat(event.has("uuid")).isFalse();
        assertThat(event.has("session_id")).isFalse();
        assertThat(event.path("message").has("usage")).isFalse();
        // Полезные поля остаются на месте.
        assertThat(event.path("type").asText()).isEqualTo("assistant");
        assertThat(event.path("message").path("role").asText()).isEqualTo("assistant");
    }

    @Test
    void compactLevelDoesNotMutateOriginalEvents(@TempDir Path tempDir) throws Exception {
        AgentRunContext context = contextWithLogFile(tempDir.resolve("run.json"));
        AgentRunLogDto entry = entryWithEvent();

        writer.write(context, entry, AgentLogLevel.COMPACT);

        assertThat(entry.getEvents().get(0).has("uuid")).isTrue();
    }

    private AgentRunLogDto entryWithEvent() throws Exception {
        JsonNode event = objectMapper.readTree(EVENT_JSON);
        return AgentRunLogDto.builder()
                .runId("run-1")
                .events(List.of(event))
                .build();
    }

    private static AgentRunContext contextWithLogFile(Path logFile) {
        AgentRunContext context = Mockito.mock(AgentRunContext.class);
        Mockito.when(context.getLogFile()).thenReturn(logFile);
        return context;
    }
}
