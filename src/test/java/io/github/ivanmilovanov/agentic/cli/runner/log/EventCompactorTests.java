package io.github.ivanmilovanov.agentic.cli.runner.log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventCompactorTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String EVENT_JSON = """
            {
              "type": "assistant",
              "uuid": "3e8533be-0fcf-4060-91c5-331f76be62d0",
              "session_id": "336c2995-1fd8-4cc6-b3fe-3e0f0d5bd736",
              "message": {
                "role": "assistant",
                "usage": { "input_tokens": 18869, "total_tokens": 18903 }
              }
            }
            """;

    @Test
    void stripToJsonRemovesServiceFieldsRecursively() throws Exception {
        JsonNode event = objectMapper.readTree(EVENT_JSON);

        String json = EventCompactor.stripToJson(List.of(event));

        assertThat(json).doesNotContain("uuid", "session_id", "usage");
        assertThat(json).contains("assistant");
    }

    @Test
    void stripDoesNotMutateOriginalEvents() throws Exception {
        JsonNode event = objectMapper.readTree(EVENT_JSON);

        EventCompactor.strip(List.of(event));

        assertThat(event.has("uuid")).isTrue();
        assertThat(event.path("message").has("usage")).isTrue();
    }

    @Test
    void stripReturnsNullForNullInput() {
        assertThat(EventCompactor.strip(null)).isNull();
    }
}
