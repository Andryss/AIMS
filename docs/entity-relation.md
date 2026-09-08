# Data Base Diagram

## Инфологическая модель

```plantuml
@startuml aims_infological_model
!pragma layout smetana
title Инфологическая модель базы данных

hide circle
hide methods
skinparam linetype ortho
skinparam classAttributeIconSize 0
skinparam shadowing false
top to bottom direction

  entity "Пользователь\n(app_user)" as user_i {
    id : integer <<PK>> — идентификатор пользователя
    --
    login : string — логин
    password_hash : string — хеш пароля
  }

  entity "Роль\n(role)" as role_i {
    id : integer <<PK>> — идентификатор роли
    --
    name : string — системное имя
    description : text — назначение роли
  }

  entity "Разрешение\n(permission)" as permission_i {
    id : integer <<PK>> — идентификатор разрешения
    --
    code : string — код действия
    description : text — описание действия
  }

  entity "Инцидент\n(incident)" as incident_i {
    id : integer <<PK>> — номер инцидента
    --
    status : string — статус
    event_type : string — тип события
    location : string — место обнаружения
    detected_at : datetime — время обнаружения
    description : text — описание
    attachment_file_ids : integer[] <<FK>> — вложения
    created_by_user_id : integer <<FK>> — автор
    responsible_user_id : integer <<FK>> — ответственный
    executor_user_ids : integer[] <<FK>> — исполнители
    created_at : datetime — время создания
    updated_at : datetime — время изменения
    alien_id : integer <<FK>> — вид инопланетянина
    cleanup_status : string — статус очистки
    cleanup_report_id : integer <<FK>> — отчёт об очистке
    monitoring_alert_id : integer <<FK>> — исходный алерт
  }

  entity "Инопланетянин\n(alien)" as alien_i {
    id : integer <<PK>> — идентификатор вида
    --
    name : string — название вида
    description : text — описание вида
    threat_level : integer — уровень угрозы
    created_at : datetime — время создания
  }

  entity "Комментарий к инциденту\n(incident_comment)" as comment_i {
    id : integer <<PK>> — идентификатор комментария
    --
    incident_id : integer <<FK>> — инцидент
    author_user_id : integer <<FK>> — автор
    text : text — текст комментария
    created_at : datetime — время создания
  }

  entity "Отчёт об очистке\n(cleanup_report)" as cleanup_i {
    id : integer <<PK>> — идентификатор отчёта
    --
    incident_id : integer <<FK>> — инцидент
    description : text — выполненные работы
    attachment_file_ids : integer[] <<FK>> — вложения
    created_by_user_id : integer <<FK>> — автор
    created_at : datetime — время создания
  }

  entity "Алерт мониторинга\n(monitoring_alert)" as alert_i {
    id : integer <<PK>> — идентификатор алерта
    --
    external_event_id : string — внешний идентификатор
    source_system : string — система-источник
    status : string — статус обработки
    event_type : string — тип события
    location : string — место обнаружения
    detected_at : datetime — время обнаружения
    description : text — описание
    media_urls : string[] — медиассылки
    raw_payload : json — исходное сообщение
    incident_id : integer <<FK>> — созданный инцидент
    received_at : datetime — время получения
    created_at : datetime — время создания
  }

  entity "Сохранённый файл\n(stored_file)" as file_i {
    id : integer <<PK>> — идентификатор файла
    --
    storage_id : string — ключ хранилища
    file_name : string — имя файла
    content_type : string — MIME-тип
    file_size : integer — размер в байтах
    created_at : datetime — время загрузки
    created_by_user_id : integer <<FK>> — загрузивший пользователь
  }

  entity "Уведомление\n(notification)" as notification_i {
    id : integer <<PK>> — идентификатор уведомления
    --
    recipient_user_id : integer <<FK>> — получатель
    message : text — текст уведомления
    related_entities : reference[] <<FK>> — связанные объекты
    read_at : datetime — время прочтения
    created_at : datetime — время создания
  }

  entity "Запись истории\n(entity_history)" as history_i {
    id : integer <<PK>> — идентификатор записи
    --
    entity_type : string — тип объекта
    entity_id : integer <<FK>> — изменённый объект
    snapshot : json — снимок состояния
    changed_by_user_id : integer <<FK>> — автор изменения
    changed_at : datetime — время изменения
  }

user_i }o--o{ role_i
role_i }o--o{ permission_i

user_i ||--o{ incident_i
user_i |o--o{ incident_i
user_i }o..o{ incident_i
alien_i |o--o{ incident_i

incident_i ||--o{ comment_i
user_i ||--o{ comment_i
incident_i |o--o| cleanup_i
user_i ||--o{ cleanup_i
incident_i |o--o| alert_i

user_i |o--o{ file_i
file_i }o..o{ incident_i
file_i }o..o{ cleanup_i

user_i ||--o{ notification_i
incident_i }o..o{ notification_i
alert_i }o..o{ notification_i

user_i ||--o{ history_i
incident_i |o..o{ history_i
alert_i |o..o{ history_i

permission_i -[hidden]right-> role_i
role_i -[hidden]right-> user_i

alien_i -[hidden]right-> incident_i
incident_i -[hidden]right-> alert_i

file_i -[hidden]right-> comment_i
comment_i -[hidden]right-> cleanup_i
cleanup_i -[hidden]right-> notification_i
notification_i -[hidden]right-> history_i

user_i -[hidden]down-> incident_i
incident_i -[hidden]down-> cleanup_i

@enduml
```

## Даталогическая модель

```plantuml
@startuml aims_datalogical_model
!pragma layout smetana
title Даталогическая модель базы данных

hide circle
hide methods
skinparam linetype ortho
skinparam classAttributeIconSize 0
skinparam shadowing false
top to bottom direction

  class "permission" as permission {
    id : BIGSERIAL <<PK>>
    --
    code : TEXT <<UQ>> <<NN>>
    description : TEXT
  }

  class "role" as role {
    id : BIGSERIAL <<PK>>
    --
    name : TEXT <<UQ>> <<NN>>
    description : TEXT
  }

  class "role_permission" as role_permission {
    role_id : BIGINT <<PK, FK>>
    permission_id : BIGINT <<PK, FK>>
  }

  class "app_user" as app_user {
    id : BIGSERIAL <<PK>>
    --
    login : TEXT <<UQ>> <<NN>>
    password_hash : TEXT <<NN>>
  }

  class "user_role" as user_role {
    user_id : BIGINT <<PK, FK>>
    role_id : BIGINT <<PK, FK>>
  }

  class "incident" as incident {
    id : BIGSERIAL <<PK>>
    --
    status : TEXT <<NN>>
    event_type : TEXT <<NN>>
    location : TEXT <<NN>>
    detected_at : TIMESTAMP <<NN>>
    description : TEXT <<NN>>
    attachment_file_ids : JSONB <<DEFAULT '[]'>> <<NN>>
    created_by_user_id : BIGINT <<FK>> <<NN>>
    responsible_user_id : BIGINT <<FK>>
    executor_user_ids : JSONB <<DEFAULT '[]'>> <<NN>>
    created_at : TIMESTAMP <<DEFAULT now()>> <<NN>>
    updated_at : TIMESTAMP <<DEFAULT now()>> <<NN>>
    alien_id : BIGINT <<FK>>
    cleanup_status : TEXT
    cleanup_report_id : BIGINT <<FK>>
    monitoring_alert_id : BIGINT <<FK, UQ partial>>
  }

  class "incident_comment" as incident_comment {
    id : BIGSERIAL <<PK>>
    --
    incident_id : BIGINT <<FK>> <<NN>>
    author_user_id : BIGINT <<FK>> <<NN>>
    text : TEXT <<NN>>
    created_at : TIMESTAMPTZ <<DEFAULT now()>> <<NN>>
  }

  class "alien" as alien {
    id : BIGSERIAL <<PK>>
    --
    name : TEXT <<NN>>
    description : TEXT <<NN>>
    threat_level : SMALLINT <<NN>>
    created_at : TIMESTAMP <<DEFAULT now()>> <<NN>>
  }

  class "cleanup_report" as cleanup_report {
    id : BIGSERIAL <<PK>>
    --
    incident_id : BIGINT <<FK, UQ>> <<NN>>
    description : TEXT <<NN>>
    attachment_file_ids : JSONB <<DEFAULT '[]'>> <<NN>>
    created_by_user_id : BIGINT <<FK>> <<NN>>
    created_at : TIMESTAMP <<DEFAULT now()>> <<NN>>
  }

  class "monitoring_alert" as monitoring_alert {
    id : BIGSERIAL <<PK>>
    --
    external_event_id : TEXT <<UQ>> <<NN>>
    source_system : TEXT <<NN>>
    status : TEXT <<NN>>
    event_type : TEXT <<NN>>
    location : TEXT <<NN>>
    detected_at : TIMESTAMP <<NN>>
    description : TEXT <<NN>>
    media_urls : JSONB <<DEFAULT '[]'>> <<NN>>
    raw_payload : JSONB <<NN>>
    incident_id : BIGINT <<FK>>
    received_at : TIMESTAMP <<DEFAULT now()>> <<NN>>
    created_at : TIMESTAMP <<DEFAULT now()>> <<NN>>
  }

  class "queue_tasks" as queue_tasks {
    id : BIGSERIAL <<PK>>
    --
    queue_name : TEXT <<NN>>
    payload : TEXT
    created_at : TIMESTAMP WITH TIME ZONE <<DEFAULT now()>>
    next_process_at : TIMESTAMP WITH TIME ZONE <<DEFAULT now()>>
    attempt : INTEGER <<DEFAULT 0>>
    reenqueue_attempt : INTEGER <<DEFAULT 0>>
    total_attempt : INTEGER <<DEFAULT 0>>
  }

  class "stored_file" as stored_file {
    id : BIGSERIAL <<PK>>
    --
    storage_id : TEXT <<UQ>> <<NN>>
    file_name : TEXT <<NN>>
    content_type : TEXT <<NN>>
    file_size : BIGINT <<NN>>
    created_at : TIMESTAMPTZ <<DEFAULT now()>> <<NN>>
    created_by_user_id : BIGINT <<FK>>
  }

  class "entity_history" as entity_history {
    id : BIGSERIAL <<PK>>
    --
    entity_type : TEXT <<NN>>
    entity_id : BIGINT <<NN>>
    snapshot : JSONB <<NN>>
    changed_by_user_id : BIGINT <<FK>> <<NN>>
    changed_at : TIMESTAMPTZ <<DEFAULT now()>> <<NN>>
  }

  class "notification" as notification {
    id : BIGSERIAL <<PK>>
    --
    recipient_user_id : BIGINT <<FK>> <<NN>>
    message : TEXT <<NN>>
    related_entities : JSONB <<DEFAULT '[]'>> <<NN>>
    read_at : TIMESTAMPTZ
    created_at : TIMESTAMPTZ <<DEFAULT now()>> <<NN>>
  }

role ||--o{ role_permission
permission ||--o{ role_permission
app_user ||--o{ user_role
role ||--o{ user_role

app_user |o--o{ stored_file
app_user ||--o{ entity_history
app_user ||--o{ notification

app_user ||--o{ incident
app_user |o--o{ incident
alien |o--o{ incident
cleanup_report |o--o{ incident
monitoring_alert |o--o| incident

incident ||--o{ incident_comment
app_user ||--o{ incident_comment
incident ||--o| cleanup_report
app_user ||--o{ cleanup_report
incident |o--o{ monitoring_alert

stored_file }o..o{ incident
stored_file }o..o{ cleanup_report
app_user }o..o{ incident
incident }o..o{ notification
monitoring_alert }o..o{ notification
incident |o..o{ entity_history
monitoring_alert |o..o{ entity_history

permission -[hidden]right-> role
role -[hidden]right-> app_user
app_user -[hidden]right-> queue_tasks

role_permission -[hidden]right-> user_role
user_role -[hidden]right-> incident
incident -[hidden]right-> monitoring_alert

alien -[hidden]right-> incident_comment
incident_comment -[hidden]right-> cleanup_report
cleanup_report -[hidden]right-> stored_file
stored_file -[hidden]right-> notification
notification -[hidden]right-> entity_history

app_user -[hidden]down-> incident
incident -[hidden]down-> cleanup_report

@enduml
```

## Генерация диаграмм

```bash
curl -L https://github.com/plantuml/plantuml/releases/latest/download/plantuml.jar -o /tmp/plantuml.jar
mkdir -p /tmp/aims-er-render

# SVG
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -tsvg -o /tmp/aims-er-render docs/entity-relation.md

# PNG
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -tpng -o /tmp/aims-er-render docs/entity-relation.md
```
