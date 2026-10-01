<!-- markdownlint-disable MD013 MD041 -->

```plantuml
@startuml aims_uc1_use_case_view
!pragma layout smetana
title Class Diagram — UC1. Регистрация инцидента — Use Case View

skinparam classAttributeIconSize 0
skinparam linetype ortho
skinparam shadowing false
left to right direction

class "Оператор мониторинга" as Operator <<actor>>
class "Медиа" as Media <<entity>>
class "Регистрация инцидента" as Registration <<control>>
class "Инцидент" as Incident <<entity>>
class "Уведомление мониторинга" as MonitoringNotification <<entity>>
class "Заявка на оборудование" as EquipmentRequest <<entity>>

enum "Статус инцидента" as IncidentStatus <<entity>>

Operator --> Registration
Registration ..> Media
Registration --> Incident : создает
Registration --> EquipmentRequest : создает
Incident -- Media
Incident -- IncidentStatus
MonitoringNotification -- Incident : является основанием
Incident -- EquipmentRequest

@enduml
```

```plantuml
@startuml aims_uc1_logical_view
!pragma layout smetana
title Class Diagram — UC1. Регистрация инцидента — Logical View

skinparam classAttributeIconSize 0
skinparam linetype ortho
skinparam shadowing false
top to bottom direction

class "Клиент регистрации" as RegistrationClient <<boundary>>

package "Система" {
  interface "Регистрация инцидентов" as Registration {
    register(request: RegistrationRequest): IncidentState
    get(incidentId: Id): IncidentState
  }

  interface "Приём медиа" as MediaReception {
    accept(media: Media): Id
    exist(mediaIds: Id[]): Bool
  }

  interface "Мониторинг" as Monitoring {
    get(notificationId: Id): Notification
    link(notificationId: Id, incidentId: Id): Bool
  }

  interface "Управление статусом" as StatusManagement {
    change(request: StatusChangeRequest): IncidentState
  }

  interface "Работа с заявками" as EquipmentRequests {
    create(request: EquipmentRequest): Id
  }
}

package "Сообщения" {
  class RegistrationRequest <<request>> {
    eventType: EventType
    location: String
    detectedAt: Timestamp
    description: String
    mediaIds: Id[]
    notificationId: Id?
  }

  class StatusChangeRequest <<request>> {
    incidentId: Id
    targetStatus: IncidentStatus
  }

  class EquipmentRequest <<request>> {
    incidentId: Id
    needs: String
  }

  class IncidentState <<response>> {
    incidentId: Id
    status: IncidentStatus
  }

  class Notification <<response>> {
    notificationId: Id
    description: String
  }

}

RegistrationClient ..> MediaReception
RegistrationClient ..> Monitoring
RegistrationClient ..> Registration
RegistrationClient ..> StatusManagement
RegistrationClient ..> EquipmentRequests

Registration ..> MediaReception
Registration ..> Monitoring
StatusManagement ..> Registration
EquipmentRequests ..> Registration

MediaReception -[hidden]down-> RegistrationRequest
RegistrationRequest -[hidden]right-> StatusChangeRequest
StatusChangeRequest -[hidden]right-> EquipmentRequest
RegistrationRequest -[hidden]down-> Notification
StatusChangeRequest -[hidden]down-> IncidentState

@enduml
```

```plantuml
@startuml aims_uc1_implementation_view
!pragma layout smetana
title Class Diagram — UC1. Регистрация инцидента — Implementation View

skinparam classAttributeIconSize 0
skinparam linetype ortho
skinparam shadowing false
top to bottom direction

package "frontend" {
  class CreateIncidentModal <<React component>> {
    -eventType: IncidentEventType
    -location: string
    -detectedAt: string
    -description: string
    -monitoringAlertId: number | undefined
    -attachmentFiles: File[]
    -handleSubmit(event: FormEvent): Promise<void>
  }

  class ApiClient <<TypeScript module>> {
    +uploadFile(token: string, file: File): Promise<FileUploadResponse>
    +uploadFiles(token: string, files: File[]): Promise<FileUploadResponse[]>
    +createIncident(token: string, payload: CreateIncidentRequest): Promise<IncidentResponse>
    +changeIncidentStatus(token: string, id: number, status: IncidentStatus,%n()\
    \tcomment?: string): Promise<IncidentResponse>
  }
}

package "controller" {
  class FilesApiImpl {
    -fileService: FileService
    +uploadFile(file: MultipartFile): FileUploadResponse
  }

  class IncidentsApiImpl {
    -incidentService: IncidentService
    +createIncident(createIncidentRequest: CreateIncidentRequest): IncidentResponse
    +changeIncidentStatus(id: Long,%n()\
    \tchangeIncidentStatusRequest: ChangeIncidentStatusRequest): IncidentResponse
  }
}

package "service" {
  interface IncidentService {
    +create(request: CreateIncidentRequest): IncidentResponse
    +changeStatus(id: Long, request: ChangeIncidentStatusRequest): IncidentResponse
  }

  class IncidentServiceImpl {
    -incidentRepository: IncidentRepository
    -monitoringAlertRepository: MonitoringAlertRepository
    -incidentStatusWorkflow: IncidentStatusWorkflow
    +create(request: CreateIncidentRequest): IncidentResponse
    -resolveMonitoringAlertForCreate(monitoringAlertId: Long): MonitoringAlertEntity
    +changeStatus(id: Long, request: ChangeIncidentStatusRequest): IncidentResponse
  }

  class FileService {
    -fileStorage: FileStorage
    -storedFileRepository: StoredFileRepository
    +upload(file: MultipartFile): FileUploadResponse
  }

  interface FileStorage {
    +store(originalFileName: String, contentType: String,%n()\
    \tcontent: InputStream, sizeBytes: long): FileDescriptor
  }

  class IncidentStatusWorkflow {
    -incidentRepository: IncidentRepository
    +changeStatus(incident: IncidentEntity, target: IncidentStatus): IncidentEntity
  }
}

package "persistence" {
  interface IncidentRepository <<Spring Data>> {
    +save(entity: IncidentEntity): IncidentEntity
    +findById(id: Long): Optional<IncidentEntity>
  }

  interface StoredFileRepository <<Spring Data>> {
    +save(entity: StoredFileEntity): StoredFileEntity
    +existsById(id: Long): boolean
  }

  interface MonitoringAlertRepository <<Spring Data>> {
    +save(entity: MonitoringAlertEntity): MonitoringAlertEntity
    +findById(id: Long): Optional<MonitoringAlertEntity>
  }

  class IncidentEntity <<JPA entity>> {
    -id: Long
    -status: IncidentStatus
    -eventType: IncidentEventType
    -location: String
    -detectedAt: LocalDateTime
    -description: String
    -attachmentFileIds: List<Long>
    -monitoringAlertId: Long
  }

  class StoredFileEntity <<JPA entity>> {
    -id: Long
    -storageId: String
    -fileName: String
    -contentType: String
    -fileSize: Long
  }

  class MonitoringAlertEntity <<JPA entity>> {
    -id: Long
    -status: MonitoringAlertStatus
    -incidentId: Long
  }
}

CreateIncidentModal ..> ApiClient
FilesApiImpl ..> FileService
IncidentsApiImpl ..> IncidentService
IncidentServiceImpl -[dotted]up-|> IncidentService
IncidentServiceImpl ..> IncidentRepository
IncidentServiceImpl ..> MonitoringAlertRepository
IncidentServiceImpl ..> IncidentStatusWorkflow
IncidentStatusWorkflow ..> IncidentRepository
FileService ..> FileStorage
FileService ..> StoredFileRepository

IncidentEntity o-- StoredFileEntity : attachmentFileIds
MonitoringAlertEntity -- IncidentEntity : monitoringAlertId / incidentId

ApiClient -[hidden]down-> IncidentsApiImpl
IncidentsApiImpl -[hidden]right-> FilesApiImpl
FileStorage -[hidden]down-> MonitoringAlertRepository
IncidentStatusWorkflow -[hidden]down-> MonitoringAlertRepository
IncidentRepository -[hidden]down-> IncidentEntity
StoredFileRepository -[hidden]down-> StoredFileEntity
MonitoringAlertRepository -[hidden]down-> MonitoringAlertEntity

@enduml
```

```bash
curl -L https://github.com/plantuml/plantuml/releases/latest/download/plantuml.jar -o /tmp/plantuml.jar
mkdir -p /tmp/aims-uc1-classes-render

# Syntax check
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -checkonly docs/classes/UC1.md

# SVG
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -tsvg -o /tmp/aims-uc1-classes-render docs/classes/UC1.md

# PNG
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -tpng -o /tmp/aims-uc1-classes-render docs/classes/UC1.md
```
