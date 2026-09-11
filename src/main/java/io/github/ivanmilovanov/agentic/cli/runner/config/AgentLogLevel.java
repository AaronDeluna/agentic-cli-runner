package io.github.ivanmilovanov.agentic.cli.runner.config;

/**
 * Уровень детализации лога запуска. Задаётся свойством {@code agent.log.level}.
 * По аналогии с уровнями логирования, но уровней всего два: {@link #FULL} и {@link #COMPACT}.
 */
public enum AgentLogLevel {

    /** Полный лог — все события stream-json как есть. */
    FULL,

    /** Сжатый лог (по умолчанию) — из событий вырезаются служебные поля {@code uuid}, {@code session_id}, {@code usage}. */
    COMPACT;

    /**
     * Разбирает значение свойства {@code agent.log.level} без учёта регистра.
     *
     * @param value        строковое значение (может быть {@code null} или пустым)
     * @param defaultLevel уровень по умолчанию, если значение не задано
     * @return распознанный уровень
     * @throws IllegalArgumentException если значение задано, но не соответствует ни одному уровню
     */
    public static AgentLogLevel from(String value, AgentLogLevel defaultLevel) {
        if (value == null || value.isBlank()) {
            return defaultLevel;
        }
        return AgentLogLevel.valueOf(value.trim().toUpperCase());
    }
}
