# Changelog

Все значимые изменения проекта фиксируются здесь.

## [0.0.5-alpha] — 2026-07-08

### Удалено
- `QwenSettingsUpdater` и `NotFoundSaveModelNameException` вынесены из библиотеки —
  это qwen-специфичная правка `.qwen/settings.json` (переключение модели), которой
  не место в generic-раннере. Перенесены в потребителя (модуль `skills`).
- Пакет `cli.qwen` удалён целиком — в библиотеке не осталось кода, привязанного к Qwen.

## [0.0.4-alpha] — 2026-07-08

Крупная ломающая переработка: библиотека перестала быть завязанной на Qwen — теперь
любой совместимый CLI со stream-json выводом (Qwen Code, GigaCode и форки) поддерживается
без отдельной реализации.

### Изменено
- **API `AgentRunner` переименован:** `executeUserPrompt` → `execute`,
  `executeSkillPrompt` → `executeSkill`.
- **`agent.cli` теперь и есть имя исполняемого файла.** `agent.cli=qwen` → ищем бинарь
  `qwen`, `agent.cli=gigacode` → `gigacode`. Механика запуска общая для всех.
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
