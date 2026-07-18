# Changelog

Все значимые изменения проекта фиксируются здесь.

## [1.1.1] — 2026-07-18

### Исправлено
- Публикация в Central Portal падала на этапе ожидания статуса (`waitUntil=published`):
  Sonatype добавил в JSON-ответ поле `warnings`, а модель `DeploymentApiResponse` в
  `central-publishing-maven-plugin:0.7.0` не игнорировала неизвестные поля и бросала
  `UnrecognizedPropertyException`. Загрузка бандла при этом фактически проходила, но
  сборка завершалась `BUILD FAILURE`. Плагин обновлён `0.7.0` → `0.11.0`.

## [1.1.0] — 2026-07-13

### Добавлено
- Конструктор `AgentRunnerService()` без аргументов: запускает агента прямо в текущей
  рабочей директории (cwd). Удобно, когда отдельная рабочая область не нужна —
  `new AgentRunnerService().execute(prompt)`. Конструктор с явным `workspace` сохранён.

## [1.0.1] — 2026-07-13

### Изменено
- `AgentRunContext` больше не выводит `runId` и путь логов из раскладки `<uuid>/sorce/` +
  `logs/`. Теперь библиотека сама генерирует `runId` (UUID), а `workspace` — это просто
  рабочая директория (cwd) запуска CLI; про её внутреннюю структуру SDK ничего не предполагает.
- Лог запуска пишется в `<buildDir>/agentic-cli-runner/<uuid>.json`, где `buildDir`
  определяется автоматически: `pom.xml` → `target` (Maven), `build.gradle[.kts]` → `build`
  (Gradle), иначе по умолчанию `target`.

### Удалено
- Метод `AgentRunContext.getRunDir()`. Вместо него — `AgentRunContext.getLogFile()`,
  возвращающий полный путь к файлу лога запуска.
- Предположение о структуре `<uuid>/sorce/` + `logs/`: подготовка рабочей области (копирование
  скилов и т.п.) — теперь целиком забота вызывающего кода, SDK её не навязывает.

## [1.0.0] — 2026-07-09

### Изменено
- Первый стабильный релиз, публикуемый в Maven Central под namespace `io.github.aarondeluna`.

## [0.0.9-alpha] — 2026-07-08

### Добавлено
- Живой лог процесса агента в рантайме: `parser.StreamJsonLineFormatter` превращает поток
  stream-json в читаемые строки (мысли, вызовы инструментов, итог). Форматтер подключается
  в `ApacheCommandExecutor` (по умолчанию — в `AgentRunnerFactory.defaultFactory`).

### Удалено
- Флаг `agent.cli.<name>.openai-logging` и метод `AgentRunnerProperties.isOpenaiLogging` —
  флаги `--openai-logging`/`--openai-logging-dir` были qwen-специфичными, но хардкодились в
  «generic» фабрике. `CommandFactory.buildCommand` больше не принимает `logDir`.

## [0.0.8-alpha] — 2026-07-08

### Изменено
- Полностью config-driven: имя CLI, аргументы, fallback-пути и префикс берутся из конфига
  по неймспейсу `agent.cli.<name>.*`. Библиотека больше не хранит флаги CLI в коде — любой
  CLI (в т.ч. закрытый) описывается только в properties.
- Вернулись ключи `agent.cli.<name>.args` и `agent.cli.<name>.openai-logging`;
  openai-логи теперь опциональны (по умолчанию выключены).

### Удалено
- Enum `AgentCli` — параметры CLI больше не зашиты в код, они в конфиге.

## [0.0.7-alpha] — 2026-07-08

### Изменено
- Неизвестное значение `agent.cli` больше не считается ошибкой: CLI запускается как
  stream-json-совместимый с параметрами по умолчанию (`AgentCli.DEFAULT_STREAM_JSON_ARGS`),
  имя бинаря = значение `agent.cli`. Известные CLI (`AgentCli`) по-прежнему берут свои флаги.
- `StreamJsonCommandFactory` снова принимает набор параметров (имя бинаря, аргументы,
  признак логирования) вместо `AgentCli` — так фабрика обслуживает и известные, и
  произвольные CLI.

### Удалено
- `UnsupportedAgentCliException` — произвольное имя CLI теперь допустимо (generic-запуск).

## [0.0.6-alpha] — 2026-07-08

### Добавлено
- Enum `AgentCli` с параметрами на каждый известный CLI: имя исполняемого файла,
  обязательные аргументы запуска и признак поддержки openai-логов. `agent.cli` выбирает
  элемент enum, а фабрика берёт из него флаги — так под каждый CLI используются свои
  аргументы (у qwen одни, у codex другие).
- Заведены элементы `QWEN` (поддерживается), `CODEX`, `CLAUDE`
  (экспериментально — команда собирается, формат вывода ещё не выверен).

### Изменено
- `StreamJsonCommandFactory` берёт параметры запуска из выбранного CLI (раньше набор
  аргументов был захардкожен как единый контракт для всех CLI, что было неверно —
  codex использует другие флаги).

## [0.0.5-alpha] — 2026-07-08

### Удалено
- `QwenSettingsUpdater` и `NotFoundSaveModelNameException` вынесены из библиотеки —
  это qwen-специфичная правка `.qwen/settings.json` (переключение модели), которой
  не место в generic-раннере. Перенесены в потребителя (модуль `skills`).
- Пакет `cli.qwen` удалён целиком — в библиотеке не осталось кода, привязанного к Qwen.

## [0.0.4-alpha] — 2026-07-08

Крупная ломающая переработка: библиотека перестала быть завязанной на Qwen — теперь
любой совместимый CLI со stream-json выводом поддерживается без отдельной реализации.

### Изменено
- **API `AgentRunner` переименован:** `executeUserPrompt` → `execute`,
  `executeSkillPrompt` → `executeSkill`.
- **`agent.cli` теперь и есть имя исполняемого файла.** `agent.cli=qwen` → ищем бинарь
  `qwen`. Механика запуска общая для всех совместимых CLI.
- **Ключи настроек стали плоскими:** `agent.cli.fallback.<os>`, `agent.cli.prefix.windows`
  (без повторения имени CLI). В одном файле — конфиг одного активного CLI.
- `QwenCommandFactoryImpl` → `StreamJsonCommandFactory` (принимает имя исполняемого файла),
  переехал в пакет `cli`.
- `QwenAgentRunner` → `AgentRunnerImpl`, переехал в пакет `runner`.
- `OsAwareCommandResolver` переехал из `cli.qwen` в `cli`.
- `AgentRunnerFactory.createCommandFactory(...)` больше не принимает `AgentCli` — читает
  имя CLI из настроек.

### Удалено
- Enum `AgentCli` и `UnsupportedAgentCliException` — выбор CLI больше не ограничен
  фиксированным списком, `agent.cli` может быть любым именем бинаря.

## [0.0.3-alpha] — 2026-07-08

### Изменено
- Обязательные аргументы запуска Qwen CLI (`--output-format stream-json`,
  `--approval-mode yolo`) зашиты в `QwenCommandFactoryImpl` как контракт библиотеки
  и больше не настраиваются через конфиг. Причина: парсер разбирает именно stream-json,
  а headless-запуск не должен зависать на подтверждениях — менять эти аргументы = ломать
  работу библиотеки.

### Удалено
- Ключ `agent.cli.<name>.args` и метод `AgentRunnerProperties.getBaseArgs(...)`.
  Пользователю остаются только среда-зависимые настройки: пути (`fallback.<os>`) и
  префикс команды для Windows (`prefix.windows`).

## [0.0.2-alpha] — 2026-07-08

### Изменено
- Ключи настроек CLI больше не завязаны на `qwen`: имя CLI-сегмента выводится из
  значения `agent.cli`. Ключи строятся как `agent.cli.<name>.args`,
  `agent.cli.<name>.fallback.<os>`, `agent.cli.<name>.prefix.windows`. Это позволяет
  держать конфиги нескольких CLI в одном файле и переключаться одной строкой `agent.cli`.
  Существующие файлы формата `agent.cli.qwen.*` продолжают работать без изменений.
- Сигнатуры `AgentRunnerProperties.getBaseArgs/getFallbackPaths/getPrefix` теперь
  принимают имя CLI (`cliName`).

### Удалено
- Убраны `JudgeRunner` и `QwenJudgeRunner` — понятие «судьи» относится к доменной
  логике оценки, а не к запуску CLI. Потребитель может получить финальный ответ
  через `AgentRunner` / примитивы (`CommandFactory` + `CommandExecutor` + `AgentStreamJsonParser`).

## [0.0.1-alpha] — 2026-07-07

### Добавлено
- Первая alpha-версия библиотеки запуска агентских CLI из Java.
- Запуск Qwen Code CLI: поиск исполняемого файла под текущую ОС
  (`OsAwareCommandResolver`: `<COMMAND>_PATH` → `PATH` → fallback-пути),
  сборка команды (`QwenCommandFactoryImpl`), запуск процесса
  (`ApacheCommandExecutor`), разбор stream-json вывода (`AgentStreamJsonParser`).
- Запись лога каждого запуска на диск (`RunnerLogWriter`).
- Конфигурация через `agent-runner.properties` с подстановкой переменных
  (`${env.X}`, `${user.home}`, `$HOME`, `$USERPROFILE`).
- Точка входа `AgentRunnerService` и фабрика `AgentRunnerFactory`.
