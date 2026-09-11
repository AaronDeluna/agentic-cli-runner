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
 * Урезание служебных полей событий stream-json для compact-лога:
 * рекурсивно вырезает {@code uuid}, {@code session_id} и {@code usage}. Исходные события не мутируются.
 */
public final class EventCompactor {

    private static final Set<String> STRIPPED_FIELDS = Set.of("uuid", "session_id", "usage");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private EventCompactor() {
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
