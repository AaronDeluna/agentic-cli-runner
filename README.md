# agentic-cli-runner

Java-библиотека для запуска агентских CLI из вашего кода. Она сама находит исполняемый файл CLI, собирает команду, запускает процесс, разбирает его stream-json вывод и сохраняет лог запуска на диск. Никакого REST, никакого Spring — просто Java-классы, которые можно встроить куда угодно: в CI-скрипт, тестовый фреймворк или бэкенд-сервис.

Библиотека не привязана к одному CLI и не хранит их флаги в коде: имя бинаря и аргументы запуска задаются в конфиге по неймспейсу `agent.cli.<name>.*`. Так поддерживается любой stream-json-совместимый CLI (например, [Qwen Code](https://github.com/QwenLM/qwen-code) и его форки) без правок кода.

## Требования

- Java 17+
- Maven 3.6+
- Установленный CLI-агент (например, [Qwen CLI](https://github.com/QwenLM/qwen-code)): бинарь должен быть в `PATH`, либо путь к нему указывается через переменную окружения или fallback-пути в настройках — см. ниже.

## Quick Start

```java
Path workspace = Path.of("/path/to/workspace"); // здесь может лежать .qwen/ со скилами
AgentRunner runner = new AgentRunnerService(workspace);

AgentResultDto result = runner.execute("Объясни, что делает этот код");
System.out.println(result.getFinalResult());
```

`AgentRunnerService` сам прочитает `agent-runner.properties`, определит CLI и соберёт для него команду. Если нужен явный скил:

```java
runner.executeSkill("review", "Проверь этот PR на баги");
```

## Конфигурация: agent-runner.properties

Всё, что нужно для запуска CLI, задаётся в конфиге по неймспейсу `agent.cli.<name>.*` —
библиотека не хранит флаги CLI в коде. Файл ищется сначала в classpath, затем в текущей
директории. Пример для Qwen:

```properties
agent.cli=qwen
agent.cli.qwen.args=--output-format,stream-json,--approval-mode,yolo
agent.cli.qwen.prefix.windows=cmd,/c
agent.cli.qwen.fallback.mac=${env.HOME}/.local/bin
agent.cli.qwen.fallback.linux=${env.HOME}/.local/bin
```

Что означают ключи:

| Ключ | Описание |
|---|---|
| `agent.cli` | Имя активного CLI = имя его исполняемого файла (`qwen`, `codex`, …). Один активный CLI на файл. |
| `agent.cli.<name>.args` | Обязательные аргументы запуска, через запятую (`prompt` добавляется автоматически в конце) |
| `agent.cli.<name>.fallback.<os>` | Запасные пути поиска бинаря для ОС (`mac`/`linux`/`windows`), через `;` |
| `agent.cli.<name>.prefix.windows` | Префикс команды для Windows (например, `cmd,/c`) |

Пути и значения поддерживают подстановку переменных: `${env.HOME}`, `${user.home}`, `$HOME`, `$USERPROFILE`.

Поиск бинаря идёт в порядке: переменная окружения `<NAME>_PATH` (например, `QWEN_PATH`) → системный `PATH` → fallback-пути из конфига.

## Примеры конфигурации под разные CLI

Библиотека запускает `[prefix] <бинарь> <args...> <prompt>` и разбирает stream-json вывод.
Под конкретный CLI меняются `agent.cli` и его `.args`:

| CLI | `agent.cli` | `agent.cli.<name>.args` |
|---|---|---|
| Qwen Code | `qwen` | `--output-format,stream-json,--approval-mode,yolo` |
| Codex (OpenAI) | `codex` | `exec,--json,--dangerously-bypass-approvals-and-sandbox` |
| Claude Code | `claude` | `-p,--output-format,stream-json,--dangerously-skip-permissions` |

`codex`/`claude` — экспериментально: команда собирается корректно, но структура их событий
отличается от qwen, поэтому финальный ответ может не извлекаться общим `AgentStreamJsonParser`.
Полноценная поддержка (свой разбор вывода) — в работе.

## Подключение

**Maven** (`pom.xml`):

```xml
<dependency>
    <groupId>io.github.aarondeluna</groupId>
    <artifactId>agentic-cli-runner</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Gradle** (Groovy DSL, `build.gradle`):

```groovy
implementation 'io.github.aarondeluna:agentic-cli-runner:1.0.0'
```

**Gradle** (Kotlin DSL, `build.gradle.kts`):

```kotlin
implementation("io.github.aarondeluna:agentic-cli-runner:1.0.0")
```

Убедитесь, что в сборке подключён репозиторий `mavenCentral()` (Maven Central подключён по умолчанию).
