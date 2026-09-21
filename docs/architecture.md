# Architecture Diagrams

## System Context

```plantuml
@startuml aims_system_context
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Context.puml

title C4 System Context — AIMS

LAYOUT_LEFT_RIGHT()

Person(employee, "Сотрудник MIB", "Работает с инцидентами, алертами, очисткой и уведомлениями")
System(aims, "AIMS", "Alien Incident Management System")
System_Ext(monitoring, "Внешняя система мониторинга", "Передаёт события инопланетной активности")

Rel(employee, aims, "Использует веб-интерфейс", "Веб-браузер")
Rel(monitoring, aims, "Передаёт события", "POST /api/v1/integration/monitoring/events, X-Integration-Api-Key")

@enduml
```

## Container View

```plantuml
@startuml aims_container_view
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Container.puml

title C4 Container View — AIMS

LAYOUT_TOP_DOWN()

Person(employee, "Сотрудник MIB", "Пользователь веб-приложения")
System_Ext(monitoring, "Внешняя система мониторинга", "Источник событий инопланетной активности")

System_Boundary(aims, "AIMS") {
  Container(staticContent, "Static Content", "Directory", "HTML, CSS и JavaScript из classpath:/static")
  Container(frontend, "Клиентское приложение", "HTML, CSS, JavaScript", "Интерфейс, выполняемый в веб-браузере")
  Container(backend, "Backend", "Java 17, Spring Boot", "REST API, бизнес-логика и асинхронная обработка")
  ContainerDb(database, "PostgreSQL", "PostgreSQL", "Прикладные таблицы, метаданные вложений и queue_tasks")
  Container(storage, "Локальное файловое хранилище", "Файловая система", "Содержимое вложений в ~/.aims/storage")
}

Rel(employee, staticContent, "Загружает UI", "HTTP")
Rel(employee, frontend, "Использует", "Веб-браузер")
Rel(staticContent, frontend, "Предоставляет")
Rel(frontend, backend, "Вызывает API", "JSON/HTTP, JWT")
Rel(monitoring, backend, "Передаёт события", "JSON/HTTP, X-Integration-Api-Key")
Rel(backend, staticContent, "Читает и раздаёт", "Spring ResourceHandler, /public/**")
Rel(backend, database, "Читает и изменяет данные и задачи", "JDBC")
Rel(backend, storage, "Читает и сохраняет вложения", "File I/O")

@enduml
```

## Component View

```plantuml
@startuml aims_component_view
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Component.puml

title C4 Component View — AIMS

LAYOUT_TOP_DOWN()

System_Boundary(aims, "AIMS") {
  Container_Boundary(frontend, "Клиентское приложение") {
    Component(sessionUi, "Навигация и сессия", "React", "Вход, профиль и навигация по приложению")
    Component(incidentUi, "Инциденты и очистка", "React", "Работа с инцидентами, очисткой и вложениями")
    Component(monitoringUi, "Мониторинг и уведомления", "React", "Просмотр алертов и пользовательских уведомлений")
    Component(apiClient, "API-клиент", "TypeScript, Fetch API", "Обращается к REST API и передаёт JWT")
  }

  Container_Boundary(backend, "Backend") {
    Component(api, "HTTP API и безопасность", "OpenAPI, Spring MVC, Spring Security", "Принимает REST-запросы и проверяет JWT")
    Component(incidents, "Инциденты и очистка", "Spring Services", "Управляет инцидентами, очисткой, статусами и историей")
    Component(monitoring, "Мониторинг и уведомления", "Spring Services", "Обрабатывает алерты и пользовательские уведомления")
    Component(files, "Работа с файлами", "Spring Services", "Метаданные и содержимое вложений")
    Component(dbqueue, "Асинхронная обработка", "db-queue", "Ставит и выполняет фоновые задачи")
    Component(persistence, "Доступ к данным", "Spring Data JPA, Liquibase", "Repositories, entities и миграции")
  }

  ContainerDb(database, "PostgreSQL", "PostgreSQL", "Прикладные таблицы и queue_tasks")
  Container(storage, "Локальное файловое хранилище", "Файловая система", "Содержимое вложений")
}

Lay_R(sessionUi, incidentUi)
Lay_R(incidentUi, monitoringUi)
Lay_D(incidentUi, apiClient)
Lay_D(apiClient, api)
Lay_D(api, incidents)
Lay_R(incidents, monitoring)
Lay_R(monitoring, files)
Lay_D(incidents, dbqueue)
Lay_R(dbqueue, persistence)
Lay_D(persistence, database)
Lay_D(files, storage)

Rel_D(sessionUi, apiClient, "Вход и профиль")
Rel_D(incidentUi, apiClient, "Инциденты, очистка и вложения")
Rel_D(monitoringUi, apiClient, "Алерты и уведомления")
Rel_D(apiClient, api, "Вызывает", "JSON/HTTP, JWT")

Rel_D(api, incidents, "Передаёт команды и запросы")
Rel_D(api, monitoring, "Передаёт алерты и запросы")
Rel_D(api, files, "Передаёт файлы")
Rel_D(incidents, dbqueue, "Ставит фоновые задачи")
Rel_D(monitoring, dbqueue, "Ставит задачи уведомлений")
Rel_D(incidents, persistence, "Сохраняет предметные данные")
Rel_D(monitoring, persistence, "Сохраняет алерты и уведомления")
Rel_D(files, persistence, "Сохраняет метаданные")
Rel_D(files, storage, "Хранит содержимое", "File I/O")
Rel_D(dbqueue, database, "Обрабатывает queue_tasks", "JDBC")
Rel_D(persistence, database, "Читает и изменяет данные", "JDBC")

@enduml
```

## Генерация диаграмм

```bash
curl -L https://github.com/plantuml/plantuml/releases/latest/download/plantuml.jar -o /tmp/plantuml.jar
mkdir -p /tmp/aims-architecture-render

# Проверка синтаксиса
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -checkonly docs/architecture.md

# SVG
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -tsvg -o /tmp/aims-architecture-render docs/architecture.md

# PNG
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -tpng -o /tmp/aims-architecture-render docs/architecture.md
```
