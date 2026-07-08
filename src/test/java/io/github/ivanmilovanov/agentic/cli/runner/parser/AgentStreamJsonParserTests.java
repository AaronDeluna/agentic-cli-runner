package io.github.ivanmilovanov.agentic.cli.runner.parser;

import io.github.ivanmilovanov.agentic.cli.runner.model.AgentLogDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AgentStreamJsonParserTests {

    private final AgentStreamJsonParser parser = new AgentStreamJsonParser();

    private static final String STREAM_JSON = """
            {"type":"system","subtype":"init","cwd":"/tmp","session_id":"abc"}
            {"type":"assistant","message":{"role":"assistant","content":[{"type":"text","text":"Analysing..."}]}}
            {"type":"tool_use","tool_name":"bash","tool_input":{"command":"ls"}}
            {"type":"result","subtype":"success","result":"Done","session_id":"abc","cost_usd":0.001}
            """;

    @Test
    void parsesAllEventsFromStreamJson() throws Exception {
        AgentLogDto result = parser.parse(STREAM_JSON);

        assertThat(result.getEvents()).hasSize(4);
        assertThat(result.getEvents().get(0).path("type").asText()).isEqualTo("system");
        assertThat(result.getEvents().get(1).path("type").asText()).isEqualTo("assistant");
        assertThat(result.getEvents().get(2).path("type").asText()).isEqualTo("tool_use");
        assertThat(result.getEvents().get(3).path("type").asText()).isEqualTo("result");
    }

    @Test
    void extractsFinalResultFromLastResultEvent() throws Exception {
        AgentLogDto result = parser.parse(STREAM_JSON);

        assertThat(result.getFinalResult()).isEqualTo("Done");
    }

    @Test
    void eventsJsonIsValidPrettyPrintedArray() throws Exception {
        AgentLogDto result = parser.parse(STREAM_JSON);

        assertThat(result.getEventsJson()).contains("\"type\" : \"system\"");
        assertThat(result.getEventsJson()).startsWith("[");
        assertThat(result.getEventsJson().trim()).endsWith("]");
    }

    @Test
    void returnsNullFinalResultWhenNoResultEvent() throws Exception {
        String noResult = """
                {"type":"system","subtype":"init","cwd":"/tmp","session_id":"abc"}
                {"type":"assistant","message":{"role":"assistant","content":[{"type":"text","text":"Hi"}]}}
                """;

        AgentLogDto result = parser.parse(noResult);

        assertThat(result.getEvents()).hasSize(2);
        assertThat(result.getFinalResult()).isNull();
    }

    @Test
    void doesNotThrowOnEmptyInput() {
        assertThatCode(() -> parser.parse("")).doesNotThrowAnyException();
        assertThatCode(() -> parser.parse(null)).doesNotThrowAnyException();
        assertThatCode(() -> parser.parse("   ")).doesNotThrowAnyException();
    }

    @Test
    void emptyInputProducesEmptyEventsAndNullFinalResult() throws Exception {
        AgentLogDto result = parser.parse(null);

        assertThat(result.getEvents()).isEmpty();
        assertThat(result.getEventsJson()).isEqualTo("[]");
        assertThat(result.getFinalResult()).isNull();
    }
}
