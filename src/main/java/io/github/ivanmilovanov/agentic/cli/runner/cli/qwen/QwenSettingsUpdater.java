package io.github.ivanmilovanov.agentic.cli.runner.cli.qwen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.ivanmilovanov.agentic.cli.runner.context.AgentRunContext;
import io.github.ivanmilovanov.agentic.cli.runner.exception.NotFoundSaveModelNameException;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Читает и правит имя модели в {@code .qwen/settings.json} рабочей области запуска,
 * с возможностью откатить изменение обратно.
 */
@Slf4j
public class QwenSettingsUpdater {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DEFAULT_MODEL_NAME = "default-model";

    private final boolean createSettingsIfMissing;
    private String previousModelName = null;
    private final AgentRunContext agentRunContext;

    /**
     * Создаёт апдейтер, который требует существующий settings.json (без автосоздания).
     */
    public QwenSettingsUpdater(AgentRunContext agentRunContext) {
        this.agentRunContext = agentRunContext;
        this.createSettingsIfMissing = false;
    }

    private QwenSettingsUpdater(Builder builder) {
        this.agentRunContext = builder.agentRunContext;
        this.createSettingsIfMissing = builder.createSettingsIfMissing;
    }

    /** Возвращает builder для настройки создаваемого экземпляра. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder для {@link QwenSettingsUpdater}.
     */
    public static class Builder {
        private AgentRunContext agentRunContext;
        private boolean createSettingsIfMissing = false;

        /** Задаёт контекст запуска, из которого берётся путь к settings.json. */
        public Builder agentRunContext(AgentRunContext agentRunContext) {
            this.agentRunContext = agentRunContext;
            return this;
        }

        /** Разрешает создать минимальный settings.json, если файл отсутствует. */
        public Builder createSettingsIfMissing(boolean createSettingsIfMissing) {
            this.createSettingsIfMissing = createSettingsIfMissing;
            return this;
        }

        /**
         * @throws IllegalArgumentException если не задан agentRunContext
         */
        public QwenSettingsUpdater build() {
            if (agentRunContext == null) {
                throw new IllegalArgumentException("agentRunContext must not be null");
            }
            return new QwenSettingsUpdater(this);
        }
    }

    /**
     * Проверяет наличие settings.json. Если файла нет и {@code createSettingsIfMissing} = true,
     * создаёт директорию .qwen и записывает минимальный валидный JSON.
     *
     * @param settingsPath путь к settings.json
     * @return файл settings.json
     * @throws IOException если файл не найден, а createSettingsIfMissing = false
     */
    private File ensureSettingsFile(Path settingsPath) throws IOException {
        File settingsFile = settingsPath.toFile();
        if (settingsFile.exists()) {
            return settingsFile;
        }
        if (!createSettingsIfMissing) {
            log.error("Файл settings.json не найден по пути: {}", settingsPath);
            throw new FileNotFoundException("Файл settings.json не найден по пути: " + settingsPath);
        }
        log.info("Файл settings.json не найден, создаём минимальный: {}", settingsPath);
        Files.createDirectories(settingsPath.getParent());
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        ObjectNode modelNode = root.putObject("model");
        modelNode.put("name", DEFAULT_MODEL_NAME);
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(settingsFile, root);
        log.info("Создан минимальный settings.json с моделью '{}'", DEFAULT_MODEL_NAME);
        return settingsFile;
    }

    /**
     * Обновляет имя модели в settings.json и сохраняет файл.
     * Перед изменением сохраняет текущее значение как "предыдущее".
     *
     * @param newModelName новое имя модели
     */
    public void updateModelNameAndSave(String newModelName) throws Exception {
        log.debug("Начало обновления имени модели на: {}", newModelName);
        Path workDir = agentRunContext.getWorkspace();
        Path settingsPath = workDir.resolve(".qwen").resolve("settings.json");
        File settingsFile = ensureSettingsFile(settingsPath);

        try {
            ObjectNode root = (ObjectNode) OBJECT_MAPPER.readTree(settingsFile);
            String currentName = root.get("model").get("name").asText();
            if (currentName.equals(newModelName)) {
                log.info("Имена сохраненной и новой моделей идентичны '{}', изменение модели не производится", currentName);
            } else {
                previousModelName = currentName;
                log.info("Текущее имя модели '{}' сохранено как предыдущее", currentName);

                ((ObjectNode) root.get("model")).put("name", newModelName);

                String updatedJson = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
                log.info("Обновленная конфигурация 'settings.json' для выполнения тестов:\n{}", updatedJson);

                OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(settingsFile, root);
                log.info("Имя модели успешно обновлено с '{}' на '{}'", currentName, newModelName);
            }
        } catch (Exception e) {
            log.error("Ошибка при обновлении имени модели: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Восстанавливает исходное имя модели (которое было до последнего вызова updateModelNameAndSave).
     * Если предыдущее значение отсутствует (ни разу не вызывали update), то метод ничего не делает.
     *
     * @throws NotFoundSaveModelNameException если предыдущее имя не сохранено
     */
    public void restoreOriginalModelName() throws Exception {
        log.debug("Начало восстановления исходного имени модели");
        if (previousModelName == null) {
            log.warn("Попытка восстановления, но предыдущее имя модели не сохранено");
            throw new NotFoundSaveModelNameException();
        }

        Path workDir = agentRunContext.getWorkspace();
        Path settingsPath = workDir.resolve(".qwen").resolve("settings.json");
        File settingsFile = ensureSettingsFile(settingsPath);

        try {
            ObjectNode root = (ObjectNode) OBJECT_MAPPER.readTree(settingsFile);
            ((ObjectNode) root.get("model")).put("name", previousModelName);
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(settingsFile, root);
            log.info("Имя модели восстановлено до '{}'", previousModelName);
        } catch (Exception e) {
            log.error("Ошибка при восстановлении имени модели: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Возвращает текущее имя модели из файла settings.json.
     */
    public String getCurrentModelName() throws Exception {
        log.debug("Получение текущего имени модели");
        Path workDir = agentRunContext.getWorkspace();
        Path settingsPath = workDir.resolve(".qwen").resolve("settings.json");
        try {
            File settingsFile = ensureSettingsFile(settingsPath);
            ObjectNode root = (ObjectNode) OBJECT_MAPPER.readTree(settingsFile);
            String modelName = root.get("model").get("name").asText();
            log.debug("Текущее имя модели: {}", modelName);
            return modelName;
        } catch (Exception e) {
            log.error("Не удалось получить текущее имя модели: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Возвращает сохранённое предыдущее имя модели.
     */
    public String getPreviousModelName() {
        log.debug("Запрошено предыдущее имя модели: {}", previousModelName);
        return previousModelName;
    }
}
