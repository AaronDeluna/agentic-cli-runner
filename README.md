# agentic-cli-runner

Java-библиотека для запуска агентских CLI из вашего кода. Она сама находит исполняемый файл CLI, собирает команду, запускает процесс, разбирает его stream-json вывод и сохраняет лог запуска на диск. Никакого REST, никакого Spring — просто Java-классы, которые можно встроить куда угодно: в CI-скрипт, тестовый фреймворк или бэкенд-сервис.

Библиотека не привязана к одному CLI: значение `agent.cli` — это имя исполняемого файла, а механика запуска общая для всех совместимых CLI со stream-json выводом ([Qwen Code](https://github.com/QwenLM/qwen-code), GigaCode и их форки). Добавить такой CLI = поменять одну строку в конфиге, кода писать не нужно.

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

Файл ищется сначала в classpath, затем в текущей директории. Минимальный пример:

```properties
agent.cli=qwen
agent.cli.fallback.mac=${env.HOME}/.local/bin
agent.cli.fallback.linux=${env.HOME}/.local/bin
agent.cli.prefix.windows=cmd,/c
```

Что означают ключи:

| Ключ | Описание |
|---|---|
| `agent.cli` | Имя активного CLI = имя его исполняемого файла (`qwen`, `gigacode`, …). Настройки одного активного CLI на файл. |
| `agent.cli.fallback.<os>` | Запасные пути поиска исполняемого файла для конкретной ОС (`mac`/`linux`/`windows`), через `;` |
| `agent.cli.prefix.windows` | Префикс команды для Windows (например, `cmd,/c`) |

Пути и значения поддерживают подстановку переменных: `${env.HOME}`, `${user.home}`, `$HOME`, `$USERPROFILE`.

Поиск исполняемого файла CLI идёт в таком порядке: переменная окружения `<COMMAND>_PATH` (например, `QWEN_PATH`) → системный `PATH` → fallback-пути из конфига.

Аргументы `--output-format stream-json` и `--approval-mode yolo` — контракт библиотеки (парсер разбирает именно stream-json, а headless-запуск не должен зависать на подтверждениях), поэтому они зашиты в коде и не настраиваются.

## Как добавить новый CLI

- **CLI-форк со stream-json выводом** (GigaCode и подобные) — кода писать не нужно. Просто укажите его имя и пути:

  ```properties
  agent.cli=gigacode
  agent.cli.fallback.mac=${env.HOME}/.local/bin
  ```

- **CLI с другим форматом вывода** — реализуйте `CommandFactory` (сборка команды) и/или свой парсер вместо `AgentStreamJsonParser`, и передайте их в `AgentRunnerImpl`. Точку расширения под парсер добавим, когда появится реальный кейс.

## Подключение

**Maven** (`pom.xml`):

```xml
<dependency>
    <groupId>io.github.ivanmilovanov</groupId>
    <artifactId>agentic-cli-runner</artifactId>
    <version>0.0.5-alpha</version>
</dependency>
```

**Gradle** (Groovy DSL, `build.gradle`):

```groovy
implementation 'io.github.ivanmilovanov:agentic-cli-runner:0.0.5-alpha'
```

**Gradle** (Kotlin DSL, `build.gradle.kts`):

```kotlin
implementation("io.github.ivanmilovanov:agentic-cli-runner:0.0.5-alpha")
```

Убедитесь, что в сборке подключён репозиторий `mavenCentral()` (Maven Central подключён по умолчанию).
