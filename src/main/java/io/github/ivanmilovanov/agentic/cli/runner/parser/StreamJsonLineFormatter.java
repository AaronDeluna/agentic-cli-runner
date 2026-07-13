package io.github.ivanmilovanov.agentic.cli.runner.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Форматирование строки stream-json в короткую читаемую строку для живого лога.
 * <p>
 * Нераспознанная строка (не событие или не JSON) возвращается без изменений.
 */
public class StreamJsonLineFormatter {

    private final ObjectMapper objectMapper;

    public StreamJsonLineFormatter() {
        this(new ObjectMapper());
    }

    public StreamJsonLineFormatter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Форматирует строку stream-json; при неудаче разбора возвращает исходную строку.
     *
     * @param line строка вывода CLI
     * @return читаемая строка для живого лога
     */
    public String format(String line) {
        if (line == null || line.isBlank()) {
            return line;
        }
        JsonNode event;
        try {
            event = objectMapper.readTree(line);
        } catch (Exception e) {
            return line;
        }
        if (event == null || !event.isObject()) {
            return line;
        }

        String type = event.path("type").asText("");
        return switch (type) {
            case "system" -> formatSystem(event);
            case "assistant" -> formatMessage(event, "[ASSISTANT]");
            case "user" -> formatMessage(event, "[USER]");
            case "result" -> formatResult(event);
            default -> line;
        };
    }

    private String formatSystem(JsonNode event) {
        if ("init".equals(event.path("subtype").asText())) {
            String model = event.path("model").asText("");
            String cwd = event.path("cwd").asText("");
            StringBuilder sb = new StringBuilder("[START] сессия");
            if (!model.isEmpty()) {
                sb.append(" | model=").append(model);
            }
            if (!cwd.isEmpty()) {
                sb.append(" | cwd=").append(cwd);
            }
            return sb.toString();
        }
        return "[SYSTEM] " + event.path("subtype").asText("system");
    }

    private String formatMessage(JsonNode event, String tag) {
        JsonNode content = event.path("message").path("content");
        if (!content.isArray() || content.isEmpty()) {
            return tag + " ...";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : content) {
            String partType = part.path("type").asText("");
            String rendered = switch (partType) {
                case "text" -> tag + " " + clean(part.path("text").asText().strip());
                case "thinking" -> "[THINK] " + clean(part.path("thinking").asText().strip());
                case "tool_use" -> "[TOOL] " + part.path("name").asText("tool") + "(" + shortInput(part.path("input")) + ")";
                case "tool_result" -> tag + " результат: " + clean(toolResultText(part));
                default -> "";
            };
            if (!rendered.isBlank()) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(rendered);
            }
        }
        return sb.length() > 0 ? sb.toString() : tag + " ...";
    }

    private String formatResult(JsonNode event) {
        StringBuilder sb = new StringBuilder("[DONE]");
        if (event.hasNonNull("subtype")) {
            sb.append(" (").append(event.path("subtype").asText()).append(")");
        }
        if (event.hasNonNull("duration_ms")) {
            sb.append(" | ").append(event.path("duration_ms").asLong()).append(" мс");
        }
        if (event.hasNonNull("total_cost_usd")) {
            sb.append(" | $").append(event.path("total_cost_usd").asText());
        }
        return sb.toString();
    }

    private String shortInput(JsonNode input) {
        if (input == null || input.isMissingNode() || input.isNull()) {
            return "";
        }
        String raw = input.isTextual() ? input.asText() : input.toString();
        return clean(raw.replace('\n', ' '));
    }

    private String toolResultText(JsonNode part) {
        JsonNode content = part.path("content");
        if (content.isTextual()) {
            return content.asText();
        }
        if (content.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode c : content) {
                if (c.path("type").asText().equals("text")) {
                    sb.append(c.path("text").asText());
                }
            }
            return sb.toString();
        }
        return content.toString();
    }

    private static String clean(String text) {
        if (text == null) {
            return "";
        }
        return text.strip();
    }
}
