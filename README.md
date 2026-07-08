# agentic-cli-runner

Небольшая Java-библиотека для запуска агентских CLI (сейчас — [Qwen Code](https://github.com/QwenLM/qwen-code)) из вашего кода. Она сама находит исполняемый файл CLI, собирает команду, запускает процесс, разбирает его stream-json вывод и сохраняет лог запуска на диск. Никакого REST, никакого Spring — просто Java-классы, которые можно встроить куда угодно: в CI-скрипт, тестовый фреймворк или бэкенд-сервис.

## Требования

- Java 17+
- Maven 3.6+
- Установленный [Qwen CLI](https://github.com/QwenLM/qwen-code) (`qwen` должен быть в `PATH`, либо путь к нему нужно указать через переменную окружения или настройки — см. ниже)

## Quick Start

```java
Path workspace = Path.of("/path/to/workspace"); // здесь может лежать .qwen/ со скилами
AgentRunner runner = new AgentRunnerService(workspace);

AgentResultDto result = runner.executeUserPrompt("Объясни, что делает этот код");
System.out.println(result.getFinalResult());
```

`AgentRunnerService` сам прочитает `agent-runner.properties`, определит CLI и соберёт для него команду. Если нужен явный скил:

```java
runner.executeSkillPrompt("review", "Проверь этот PR на баги");
```

## Конфигурация: agent-runner.properties

Файл ищется сначала в classpath, затем в текущей директории (или в `./skills`). Минимальный пример:

```properties
agent.cli=QWEN
agent.cli.qwen.args=--output-format,stream-json,--approval-mode,yolo
agent.cli.qwen.fallback.mac=${env.HOME}/.local/bin
agent.cli.qwen.fallback.linux=${env.HOME}/.local/bin
agent.cli.qwen.prefix.windows=cmd,/c
```

Что означают ключи:

| Ключ | Описание |
|---|---|
| `agent.cli` | Какой CLI использовать (пока только `QWEN`) |
| `agent.cli.qwen.args` | Базовые аргументы, добавляемые к каждому запуску, через запятую |
| `agent.cli.qwen.fallback.<os>` | Запасные пути поиска исполняемого файла для конкретной ОС (`mac`/`linux`/`windows`), через `;` |
| `agent.cli.qwen.prefix.windows` | Префикс команды для Windows (например, `cmd,/c`) |

Пути и значения поддерживают подстановку переменных: `${env.HOME}`, `${user.home}`, `$HOME`, `$USERPROFILE`.

Поиск исполняемого файла CLI идёт в таком порядке: переменная окружения `<COMMAND>_PATH` (например, `QWEN_PATH`) → системный `PATH` → fallback-пути из конфига.

## Как добавить новый CLI

Библиотека не завязана на Qwen — это просто первая поддержанная реализация. Чтобы добавить новый CLI:

1. Реализуйте `CommandFactory` — он строит список аргументов команды под конкретный CLI (см. `QwenCommandFactoryImpl` как пример).
2. При необходимости реализуйте свой `AgentRunner` / `JudgeRunner`, если поведение запуска отличается от стандартного (см. `QwenAgentRunner`).
3. Добавьте новый элемент в `AgentCli` и соответствующие ветки `switch` в `AgentRunnerFactory.create(...)` и `AgentRunnerFactory.createCommandFactory(...)`.

После этого новый CLI выбирается тем же способом — через `agent.cli` в настройках.

## Подключение через Maven

```xml
<dependency>
    <groupId>io.github.ivanmilovanov</groupId>
    <artifactId>agentic-cli-runner</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```
