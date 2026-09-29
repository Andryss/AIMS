<!-- markdownlint-disable MD013 MD041 -->

```plantuml
@startuml aims_uc4_sequence_use_case_view
title UC4. Создание отчёта об очистке — представление прецедента
scale max 3800 width

skinparam shadowing false
skinparam sequence {
  ActorBorderColor #334155
  ActorFontColor #0F172A
  LifeLineBorderColor #64748B
  ParticipantBackgroundColor #F7F9FC
  ParticipantBorderColor #334155
  ParticipantFontColor #0F172A
  ArrowColor #334155
}

actor "Специалист по прикрытию" as Cleaner

box "Alien Incident Management System" #F8FAFC
  participant "Инцидент" as Incident
  participant "Очистка" as Cleanup
  participant "Отчёт об очистке" as Report
  participant "Материал отчёта" as Material
end box

Cleaner -> Incident: Просмотреть сведения об инциденте
Incident --> Cleaner: Сведения об инциденте,\nсостояние очистки и наличие отчёта

Cleaner -> Cleanup: Начать подготовку к очистке
Cleanup -> Cleanup: Изменить состояние на «Подготовка»
Cleanup --> Cleaner: Подготовка начата

Cleaner -> Cleanup: Начать выполнение очистки
Cleanup -> Cleanup: Изменить состояние на «Выполнение»
Cleanup --> Cleaner: Выполнение начато

Cleaner -> Report: Указать сведения о выполненной очистке

Cleaner -> Material: Добавить фото или видеоматериал
Material --> Report: Включить материал в отчёт

Cleaner -> Report: Сохранить отчёт об очистке
Report -> Incident: Связать отчёт с инцидентом
Incident --> Cleaner: Отчёт сохранён

Cleaner -> Cleanup: Завершить очистку
Cleanup -> Cleanup: Изменить состояние на «Завершена»
Cleanup --> Cleaner: Очистка завершена
@enduml
```

Logical View фиксирует успешный протокол между клиентом, сервером и базой данных. Клиент использует подтверждения предыдущих операций в последующих запросах: идентификаторы загруженных материалов передаются при создании отчёта, а завершение очистки запрашивается только после получения созданного отчёта. Диаграмма не определяет транспорт, программные модули и физическую схему хранения.

```plantuml
@startuml aims_uc4_sequence_logical_view
title UC4. Создание отчёта об очистке — Logical View
scale max 3800 width

skinparam shadowing false
skinparam sequence {
  LifeLineBorderColor #64748B
  ParticipantBackgroundColor #F7F9FC
  ParticipantBorderColor #334155
  ParticipantFontColor #0F172A
  ArrowColor #334155
  GroupBackgroundColor #F8FAFC
  GroupBorderColor #94A3B8
}

participant "Клиент" as Client
participant "Сервер" as Server
database "База данных" as Database

group Переход к подготовке очистки
  Client -> Server: Изменить состояние очистки\nДанные: идентификатор инцидента, «Подготовка»
  Server -> Database: Сохранить новое состояние очистки
  Database --> Server: Обновлённое состояние инцидента
  Server --> Client: Обновлённое состояние инцидента
end

group Переход к выполнению очистки
  Client -> Server: Изменить состояние очистки\nДанные: идентификатор инцидента, «Выполнение»
  Server -> Database: Сохранить новое состояние очистки
  Database --> Server: Обновлённое состояние инцидента
  Server --> Client: Обновлённое состояние инцидента
end

loop Для каждого фото или видеоматериала
  Client -> Server: Передать материал\nДанные: содержимое и сведения о материале
  Server -> Database: Сохранить материал
  Database --> Server: Идентификатор материала
  Server --> Client: Идентификатор материала
end

Client -> Server: Создать отчёт об очистке\nДанные: идентификатор инцидента, описание,\nидентификаторы материалов
Server -> Database: Сохранить отчёт и связать его с инцидентом
Database --> Server: Созданный отчёт с идентификатором
Server --> Client: Созданный отчёт с идентификатором

group Завершение после подтверждения создания отчёта
  Client -> Server: Изменить состояние очистки\nДанные: идентификатор инцидента, «Завершена»
  Server -> Database: Получить актуальное состояние инцидента\nи связанный отчёт
  Database --> Server: Состояние инцидента и отчёт
  Server -> Database: Сохранить состояние очистки «Завершена»
  Database --> Server: Итоговое состояние инцидента
  Server --> Client: Итоговое состояние инцидента
end
@enduml
```

Implementation View показывает основные вызовы успешного сценария в реализованном коде. Физическое файловое хранилище и репозитории объединены на одной линии жизни для компактности; конкретный получатель указан в подписи каждого вызова. Вспомогательные mapper-классы, запись истории, управление React-состоянием и постановка уведомления в очередь опущены.

```plantuml
@startuml aims_uc4_sequence_implementation_view
title UC4. Создание отчёта об очистке — Implementation View
scale max 3800 width

skinparam shadowing false
skinparam sequence {
  LifeLineBorderColor #64748B
  ParticipantBackgroundColor #F7F9FC
  ParticipantBorderColor #334155
  ParticipantFontColor #0F172A
  ArrowColor #334155
  GroupBackgroundColor #F8FAFC
  GroupBorderColor #94A3B8
}

box "frontend" #EFF6FF
  participant "CleanupStatusSelect.tsx" as StatusSelect
  participant "CleanupReportDrawer.tsx" as ReportDrawer
  participant "api/client.ts" as ApiClient
end box

box "backend" #F0FDF4
  participant "FilesApiImpl" as FilesApi
  participant "IncidentsApiImpl" as IncidentsApi
  participant "FileService" as FileService
  participant "CleanupServiceImpl" as CleanupService
  participant "CleanupStatusWorkflow" as StatusWorkflow
end box

database "FileStorage /\nRepositories" as Persistence

loop nextStatus = PREPARATION, EXECUTION
  StatusSelect -> StatusSelect: handleChange(nextRaw); nextStatus = nextRaw as CleanupStatus
  StatusSelect -> ApiClient: changeCleanupStatus(token, incident.id, nextStatus)
  ApiClient -> IncidentsApi: changeIncidentCleanupStatus(id, ChangeCleanupStatusRequest { status })
  IncidentsApi -> CleanupService: changeCleanupStatus(id, request)
  CleanupService -> Persistence: IncidentRepository.findById(incidentId)
  Persistence --> CleanupService: IncidentEntity incident
  CleanupService -> StatusWorkflow: changeStatus(incident, target)
  StatusWorkflow -> StatusWorkflow: transitionGraph.getTransition(current, target)
  StatusWorkflow -> StatusWorkflow: precondition.check(incident)
  StatusWorkflow -> Persistence: IncidentRepository.save(incident) { cleanupStatus = target }
  Persistence --> StatusWorkflow: IncidentEntity incident
  StatusWorkflow --> CleanupService: incident
  CleanupService --> IncidentsApi: IncidentResponse
  IncidentsApi --> ApiClient: IncidentResponse
  ApiClient --> StatusSelect: updated
  StatusSelect -> StatusSelect: onStatusChanged(updated)
end

ReportDrawer -> ReportDrawer: handleSubmit(event); trimmed = description.trim()
ReportDrawer -> ApiClient: uploadFiles(token, attachmentFiles)

loop file in attachmentFiles
  ApiClient -> ApiClient: uploadFile(token, file)
  ApiClient -> FilesApi: uploadFile(file: MultipartFile)
  FilesApi -> FileService: upload(file)
  FileService -> Persistence: FileStorage.store(fileName, contentType, file.getInputStream(), size)
  Persistence --> FileService: FileDescriptor descriptor
  FileService -> Persistence: StoredFileRepository.save(entity)
  Persistence --> FileService: StoredFileEntity entity
  FileService --> FilesApi: FileUploadResponse
  FilesApi --> ApiClient: FileUploadResponse
end

ApiClient --> ReportDrawer: FileUploadResponse[] uploaded
ReportDrawer -> ReportDrawer: fileIds = uploaded.map((f) => f.id)
ReportDrawer -> ApiClient: createCleanupReport(token, incidentId, trimmed, fileIds)
ApiClient -> IncidentsApi: createIncidentCleanupReport(id, CreateCleanupReportRequest { description, attachmentFileIds })
IncidentsApi -> CleanupService: createReport(incidentId, request)
CleanupService -> Persistence: IncidentRepository.findById(incidentId)
Persistence --> CleanupService: IncidentEntity incident
CleanupService -> Persistence: CleanupReportRepository.existsByIncidentId(incidentId)
Persistence --> CleanupService: false
CleanupService -> CleanupService: assertCleanupAllowed(incident)
CleanupService -> CleanupService: AttachmentValidator.assertAllExist(attachmentFileIds)
CleanupService -> Persistence: CleanupReportRepository.save(report)
Persistence --> CleanupService: CleanupReportEntity report
CleanupService -> Persistence: IncidentRepository.save(incident) { cleanupReportId = report.id }
Persistence --> CleanupService: IncidentEntity incident
CleanupService --> IncidentsApi: CleanupReportResponse
IncidentsApi --> ApiClient: CleanupReportResponse
ApiClient --> ReportDrawer: created
ReportDrawer -> ReportDrawer: onCreated(created)
ReportDrawer -> ReportDrawer: onClose()

group nextStatus = COMPLETED
  StatusSelect -> StatusSelect: handleChange(nextRaw)
  StatusSelect -> ApiClient: changeCleanupStatus(token, incident.id, COMPLETED)
  ApiClient -> IncidentsApi: changeIncidentCleanupStatus(id, ChangeCleanupStatusRequest { status = COMPLETED })
  IncidentsApi -> CleanupService: changeCleanupStatus(id, request)
  CleanupService -> Persistence: IncidentRepository.findById(incidentId)
  Persistence --> CleanupService: IncidentEntity incident
  CleanupService -> StatusWorkflow: changeStatus(incident, COMPLETED)
  StatusWorkflow -> StatusWorkflow: transitionGraph.getTransition(EXECUTION, COMPLETED)
  StatusWorkflow -> StatusWorkflow: CleanupReportExistsPrecondition.check(incident)
  StatusWorkflow -> Persistence: IncidentRepository.save(incident) { cleanupStatus = COMPLETED }
  Persistence --> StatusWorkflow: IncidentEntity incident
  StatusWorkflow --> CleanupService: incident
  CleanupService --> IncidentsApi: IncidentResponse
  IncidentsApi --> ApiClient: IncidentResponse
  ApiClient --> StatusSelect: updated
  StatusSelect -> StatusSelect: onStatusChanged(updated)
end
@enduml
```

## Генерация диаграмм

```bash
curl -L https://github.com/plantuml/plantuml/releases/latest/download/plantuml.jar -o /tmp/plantuml.jar
mkdir -p /tmp/aims-uc4-sequence-render

# Проверка синтаксиса
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -checkonly docs/sequence/UC4.md

# SVG
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -tsvg -o /tmp/aims-uc4-sequence-render docs/sequence/UC4.md

# PNG
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -tpng -o /tmp/aims-uc4-sequence-render docs/sequence/UC4.md
```
