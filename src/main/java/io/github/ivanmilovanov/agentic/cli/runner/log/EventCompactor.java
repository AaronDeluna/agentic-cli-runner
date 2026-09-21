package io.github.ivanmilovanov.agentic.cli.runner.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Урезание служебных полей событий stream-json для compact-лога: рекурсивно вырезает
 * идентификаторы и прочий шум, ничего не говорящий модели-судье
 * ({@code uuid}, {@code session_id}, {@code usage}, {@code id}, {@code model},
 * {@code tool_use_id}, {@code parent_tool_use_id}). Имя модели выносится в шапку лога
 * один раз (см. {@link #extractModel(List)}), поэтому в самих событиях оно избыточно.
 * Исходные события не мутируются.
 */
public final class EventCompactor {

    private static final Set<String> STRIPPED_FIELDS = Set.of(
            "uuid", "session_id", "usage", "id", "model", "tool_use_id", "parent_tool_use_id");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private EventCompactor() {
    }

    /**
     * Находит имя модели в событиях (поле {@code model} на любом уровне). Так как во всём запуске
     * модель одна и та же, её достаточно вынести в шапку лога один раз, а из событий вырезать.
     *
     * @param events исходные события (может быть {@code null})
     * @return имя модели либо {@code null}, если поле нигде не встретилось
     */
    public static String extractModel(List<JsonNode> events) {
        if (events == null) {
            return null;
        }
        for (JsonNode event : events) {
            JsonNode model = event.findValue("model");
            if (model != null && model.isTextual() && !model.asText().isBlank()) {
                return model.asText();
            }
        }
        return null;
    }

    /**
     * Возвращает копии событий без служебных полей.
     *
     * @param events исходные события (может быть {@code null})
     * @return урезанные копии либо {@code null}, если на вход пришёл {@code null}
     */
    public static List<JsonNode> strip(List<JsonNode> events) {
        if (events == null) {
            return null;
        }
        return events.stream()
                .map(EventCompactor::stripEvent)
                .collect(Collectors.toList());
    }

    /**
     * Сериализует урезанные события в pretty-JSON (тот же формат, что и {@code eventsJson} парсера).
     *
     * @param events исходные события
     * @return JSON без служебных полей
     */
    public static String stripToJson(List<JsonNode> events) throws JsonProcessingException {
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(strip(events));
    }

    private static JsonNode stripEvent(JsonNode event) {
        JsonNode copy = event.deepCopy();
        prune(copy);
        return copy;
    }

    private static void prune(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            objectNode.remove(STRIPPED_FIELDS);
            objectNode.forEach(EventCompactor::prune);
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(EventCompactor::prune);
        }
    }
}
