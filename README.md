# agentic-cli-runner

Java-библиотека для запуска агентских CLI из кода: находит исполняемый файл CLI, собирает команду, запускает процесс, разбирает stream-json вывод и сохраняет лог запуска на диск. Без внешних зависимостей от REST или Spring — только Java-классы для встраивания в CI, тесты или бэкенд.

Библиотека не привязана к конкретному CLI и не хранит их флаги в коде: имя бинаря и аргументы запуска задаются в конфиге по неймспейсу `agent.cli.<name>.*`. Поддерживается любой stream-json-совместимый CLI (например, [Qwen Code](https://github.com/QwenLM/qwen-code) и его форки) без правок кода.

## Требования

- Java 17+
- Maven 3.6+
- Установленный CLI-агент (например, [Qwen CLI](https://github.com/QwenLM/qwen-code)): бинарь должен быть в `PATH`, либо путь к нему указывается через переменную окружения или fallback-пути в настройках — см. ниже.

## Quick Start

```java
// Самый короткий вариант — запуск прямо в текущей директории (cwd):
AgentResultDto result = new AgentRunnerService().execute("Объясни, что делает этот код");
System.out.println(result.getFinalResult());

// Либо с явной рабочей областью (в ней может лежать .qwen/ со скилами):
Path workspace = Path.of("/path/to/workspace");
AgentRunner runner = new AgentRunnerService(workspace);
```

`AgentRunnerService` сам прочитает `agent-runner.properties`, определит CLI и соберёт для него команду. Если нужен явный скил:

```java
runner.executeSkill("review", "Проверь этот PR на баги");
```

`workspace` — это просто рабочая директория (cwd), из которой запускается CLI; библиотека ничего не предполагает про её внутреннюю структуру. На каждый запуск генерируется свой `runId` (UUID), а лог пишется в `<buildDir>/agentic-cli-runner/<uuid>.json`, где `buildDir` определяется автоматически: `target` для Maven (`pom.xml` в cwd) и `build` для Gradle (`build.gradle[.kts]` в cwd).

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

# Таймаут выполнения агента в минутах (необязательно; по умолчанию 15).
agent.timeout=15
```

Что означают ключи:

| Ключ | Описание |
|---|---|
| `agent.cli` | Имя активного CLI = имя его исполняемого файла (`qwen`, `codex`, …). Один активный CLI на файл. |
| `agent.cli.<name>.args` | Обязательные аргументы запуска, через запятую (`prompt` добавляется автоматически в конце) |
| `agent.cli.<name>.fallback.<os>` | Запасные пути поиска бинаря для ОС (`mac`/`linux`/`windows`), через `;` |
| `agent.cli.<name>.prefix.windows` | Префикс команды для Windows (например, `cmd,/c`) |
| `agent.timeout` | Таймаут выполнения агента в минутах (необязательно). Если не задан — 15 минут. |
| `agent.sandbox` | Режим песочницы (`true`/`false`, по умолчанию `false`). См. раздел ниже. |
| `agent.sandbox.os-enforcement` | Жёсткий запрет записи средствами ОС (по умолчанию `true`). |
| `agent.sandbox.exclude` | Каталоги, которые не копировать в песочницу (через запятую). |

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

## Песочница (sandbox)

По умолчанию агент запускается прямо в рабочей директории и пишет в реальный проект.
Это удобно, но небезопасно: агенты запускаются в auto-approve режиме (например, qwen с
`--approval-mode yolo`) и сами себе всё разрешают. Песочница защищает проект от нежелательных
изменений.

Включается одной строкой:

```properties
agent.sandbox=true
```

Что происходит при `agent.sandbox=true` на каждый запуск:

1. **Копия проекта.** Библиотека копирует проект во временную директорию
   `<tmp>/agentic-sandbox-<runId>/` — «1 в 1», чтобы копия оставалась запускаемой.
   Каталоги из `agent.sandbox.exclude` не копируются; каталог конфига активного CLI
   (`.<agent.cli>`, например `.qwen`) всегда попадает в корень копии и **никогда** не
   исключается.
2. **Запуск в копии.** Агент стартует с рабочей директорией = временная папка. Читать он
   может всё, но **писать — только внутрь песочницы**.
3. **Изменения файлов.** Перед удалением снимается разница между оригиналом и копией
   (что агент изменил) и пишется прямо в лог запуска — в поле `fileChanges`:
   `<buildDir>/agentic-cli-runner/<runId>.json`. Это массив объектов
   `{path, changeType (added|modified|deleted), diff}` (diff в unified-формате). Отдельный
   `.diff`-файл не создаётся — весь результат работы агента лежит в одном JSON. Тот же список
   доступен программно через `AgentResultDto.getFileChanges()` (без песочницы — пустой список).
4. **Уборка.** Временная папка удаляется — даже при ошибке или таймауте.

### Два слоя защиты

- **Изоляция через копию** (всегда) — реальный проект не является рабочей директорией агента,
  поэтому обычная работа с относительными путями его не затрагивает.
- **Жёсткий запрет записи средствами ОС** (`agent.sandbox.os-enforcement=true`, по умолчанию
  включён) — процесс оборачивается в ОС-песочницу, которая физически блокирует запись за
  пределы временной папки:
  - **macOS** — `sandbox-exec`;
  - **Linux** — `bwrap` (bubblewrap) или `firejail`, если установлены;
  - **Windows / прочее** — чистого аналога нет: слой отключается с предупреждением в логе,
    остаётся изоляция через копию.

  Если нужный инструмент не найден в `PATH`, ОС-слой тоже отключается с предупреждением.
  Отключить его вручную можно так:

  ```properties
  agent.sandbox.os-enforcement=false
  ```

### Что не копировать

По умолчанию исключаются история и артефакты сборки:
`.git,.idea,node_modules,target,build,dist,.gradle`. Список переопределяется целиком:

```properties
agent.sandbox.exclude=.git,.idea,target,build
```

Пустое значение (`agent.sandbox.exclude=`) означает «копировать всё».

Каталог конфига активного CLI (`.<agent.cli>`, например `.qwen`) едет в копию из самой рабочей
области и в исключения не попадает. Всё, что агент создаёт/меняет внутри него, тоже отражается
в `fileChanges`.

## Подключение

**Maven** (`pom.xml`):

```xml
<dependency>
    <groupId>io.github.aarondeluna</groupId>
    <artifactId>agentic-cli-runner</artifactId>
    <version>1.3.0</version>
</dependency>
```

**Gradle** (Groovy DSL, `build.gradle`):

```groovy
implementation 'io.github.aarondeluna:agentic-cli-runner:1.3.0'
```

**Gradle** (Kotlin DSL, `build.gradle.kts`):

```kotlin
implementation("io.github.aarondeluna:agentic-cli-runner:1.3.0")
```

Убедитесь, что в сборке подключён репозиторий `mavenCentral()` (Maven Central подключён по умолчанию).
