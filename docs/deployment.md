# Deployment Diagram

```plantuml
@startuml aims_deployment
!pragma layout smetana
title Deployment-диаграмма AIMS

skinparam linetype ortho
skinparam shadowing false
left to right direction

node "Рабочее место сотрудника" <<device>> as workstation {
  node "Google Chrome 146.0" <<executionEnvironment>> as browser {
    artifact "index.html" <<artifact>> as spa
    note as client_note
      Клиентское приложение:
      HTML, CSS и JavaScript
    end note
  }
}

node "Внешняя система мониторинга" <<external system>> as monitoring

node "Сервер заказчика" <<device>> as server {
  note as server_note
    Операционная система:
    FreeBSD 14.3
  end note

  node "OpenJDK 17 JVM" <<executionEnvironment>> as jvm {
    node "Embedded Tomcat" <<executionEnvironment>> as tomcat {
      artifact "backend-0.0.1-SNAPSHOT.jar" <<artifact>> as backend
      note as application_note
        AIMS JAR
        При запуске применяет
        миграции Liquibase
        HTTP-порт: 8080
        Health check: /actuator/health
      end note
    }
  }

  node "PostgreSQL 16" <<executionEnvironment>> as database {
    artifact "Файлы данных" <<file>> as database_files
    note as database_note
      База данных: aims
    end note
  }

  artifact "Файлы вложений" <<file>> as storage
  note top of storage
    <U+007E>/.aims/storage
  end note

  jvm -[hidden]right-> database
  database -[hidden]down-> storage
}

browser --> tomcat : HTTP\n/public/**\n/api/v1/** (JWT)
monitoring --> tomcat : HTTP POST\n/api/v1/integration/monitoring/events\nX-Integration-Api-Key
backend --> database : JDBC\nlocalhost:5432/aims
backend --> storage : File I/O

@enduml
```

## Генерация диаграммы

```bash
curl -L https://github.com/plantuml/plantuml/releases/latest/download/plantuml.jar -o /tmp/plantuml.jar
mkdir -p /tmp/aims-deployment-render

# SVG
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -tsvg -o /tmp/aims-deployment-render docs/deployment.md

# PNG
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -tpng -o /tmp/aims-deployment-render docs/deployment.md
```
