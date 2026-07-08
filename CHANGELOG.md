# Changelog

Все значимые изменения проекта фиксируются здесь.

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
