package io.github.ivanmilovanov.agentic.cli.runner.parser;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StreamJsonLineFormatterTests {

    private final StreamJsonLineFormatter formatter = new StreamJsonLineFormatter();

    @Test
    void formatsInitSystemEvent() {
        String out = formatter.format(
                "{\"type\":\"system\",\"subtype\":\"init\",\"model\":\"claude-opus\",\"cwd\":\"/tmp\"}");

        assertThat(out).contains("[START]", "model=claude-opus", "cwd=/tmp");
    }

    @Test
    void formatsAssistantText() {
        String out = formatter.format(
                "{\"type\":\"assistant\",\"message\":{\"content\":[{\"type\":\"text\",\"text\":\"Разбираюсь\"}]}}");

        assertThat(out).isEqualTo("[ASSISTANT] Разбираюсь");
    }

    @Test
    void formatsToolUseWithInput() {
        String out = formatter.format(
                "{\"type\":\"assistant\",\"message\":{\"content\":"
                        + "[{\"type\":\"tool_use\",\"name\":\"Bash\",\"input\":{\"command\":\"ls\"}}]}}");

        assertThat(out).startsWith("[TOOL] Bash(").contains("ls");
    }

    @Test
    void formatsResultEvent() {
        String out = formatter.format(
                "{\"type\":\"result\",\"subtype\":\"success\",\"duration_ms\":1200,\"total_cost_usd\":0.01}");

        assertThat(out).contains("[DONE]", "success", "1200 мс", "$0.01");
    }

    @Test
    void returnsOriginalLineWhenNotJson() {
        String raw = "просто текст, не json";

        assertThat(formatter.format(raw)).isEqualTo(raw);
    }

    @Test
    void returnsOriginalLineForUnknownEventType() {
        String raw = "{\"type\":\"whatever\",\"foo\":1}";

        assertThat(formatter.format(raw)).isEqualTo(raw);
    }

    @Test
    void keepsLongTextInFull() {
        String longText = "a".repeat(400);
        String out = formatter.format(
                "{\"type\":\"assistant\",\"message\":{\"content\":[{\"type\":\"text\",\"text\":\"" + longText + "\"}]}}");

        assertThat(out).isEqualTo("[ASSISTANT] " + longText).doesNotContain("... (+");
    }
}
