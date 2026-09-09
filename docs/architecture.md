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

## Containers and Components

```plantuml
@startuml aims_container_component
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Component.puml

title C4 Containers and Components — AIMS

LAYOUT_TOP_DOWN()

Person(employee, "Сотрудник MIB", "Пользователь веб-приложения")
System_Ext(monitoring, "Внешняя система мониторинга", "Источник событий инопланетной активности")

System_Boundary(aims, "AIMS") {
  Container(frontend, "Клиентское приложение", "HTML, CSS, JavaScript", "Статический пользовательский интерфейс; маршруты /public/**")

  Container_Boundary(backend, "Spring Boot backend") {
    Component(api, "HTTP/API-слой", "OpenAPI, Spring MVC, Spring Security", "Контроллеры, JWT и Integration API key, раздача SPA")
    Component(services, "Сервисный слой", "Spring Services", "Бизнес-логика, включая процессы инцидентов и очистки, переходы статусов, файлы, пользователей, уведомления и аутентификацию")
    Component(dbqueue, "Асинхронная обработка", "db-queue", "Producers, consumers, процессоры")
    Component(persistence, "Слой данных", "Spring Data JPA, Liquibase", "Repositories, entities")
  }

  ContainerDb(database, "PostgreSQL", "PostgreSQL", "Прикладные таблицы, метаданные вложений и queue_tasks")
  Container(storage, "Локальное файловое хранилище", "Файловая система", "Содержимое вложений в ~/.aims/storage")
}

Rel(employee, frontend, "Использует", "Веб-браузер")
Rel(frontend, api, "Вызывает", "REST /api/v1/**, JSON, JWT")
Rel(monitoring, api, "Передаёт события", "POST /api/v1/integration/monitoring/events, X-Integration-Api-Key")

Rel(api, frontend, "Раздаёт SPA", "GET /public/**")
Rel(api, services, "Вызывает use cases")
Rel(services, dbqueue, "Транзакционно ставит задачи")
Rel(dbqueue, services, "Асинхронно создаёт in-app уведомления")
Rel(services, persistence, "Читает и изменяет данные")
Rel(persistence, database, "Работает с данными", "JDBC")
Rel(dbqueue, database, "Читает, блокирует и обновляет queue_tasks", "JDBC")
Rel(services, storage, "Читает и сохраняет вложения", "FileStorage, File I/O")

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
