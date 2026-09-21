<!-- markdownlint-disable MD013 MD041 -->

```plantuml
@startuml aims_uc1_cooperative_use_case_view
title UC1. Регистрация инцидента — представление прецедента
scale max 3800 width
top to bottom direction

skinparam shadowing false
skinparam linetype ortho
skinparam nodesep 70
skinparam ranksep 110
skinparam actor {
  BackgroundColor #F7F9FC
  BorderColor #334155
  FontColor #0F172A
}
skinparam component {
  BackgroundColor #F7F9FC
  BorderColor #334155
  FontColor #0F172A
}

actor "Внешняя система\nмониторинга" as ExternalMonitoring
actor "Оператор мониторинга" as Operator

rectangle "Alien Incident Management System" as AIMS {
  component "Диспетчер\nуведомлений" as Alerts
  component "Рабочее место\nоператора" as Workplace
  component "Координатор\nрегистрации" as Registration
  component "Контролёр\nданных" as Validation
  component "Координатор заявок\nна оборудование" as Equipment
  component "Регистратор\nинцидентов" as Incidents
  component "Координатор\nстатусов" as Status
}

ExternalMonitoring --> Alerts : 1. Передать уведомление\nо подозрительной активности
Alerts --> ExternalMonitoring : 1.1. Подтвердить приём

Operator --> Workplace : 2. Выбрать уведомление [при наличии]\n3. Ввести сведения и добавить материалы\n4. Указать потребность в оборудовании\n[при необходимости]\n5. Сохранить инцидент\n6. Передать инцидент на анализ

Workplace --> Alerts : 2.1. Запросить сведения\nвыбранного уведомления
Alerts --> Workplace : 2.2. Передать сведения уведомления

Workplace --> Registration : 2.3. Передать сведения\nиз выбранного уведомления\n3.1. Передать сведения и материалы\n4.1. Передать потребность в оборудовании\n5.1. Запросить сохранение инцидента

Registration --> Validation : 3.2. Проверить сведения и материалы
Validation --> Registration : 3.3. Передать результат проверки

Registration --> Equipment : 4.2. Создать заявку\n[при необходимости]

Registration --> Incidents : 5.2. Создать инцидент\nв статусе «Черновик»
Incidents --> Registration : 5.3. Подтвердить создание

Workplace --> Status : 6.1. Запросить статус\n«Готов к анализу»
Status --> Incidents : 6.2. Сохранить новый статус
Incidents --> Status : 6.3. Подтвердить сохранение
@enduml
```

## Генерация диаграммы

```bash
curl -L https://github.com/plantuml/plantuml/releases/latest/download/plantuml.jar -o /tmp/plantuml.jar
mkdir -p /tmp/aims-uc1-cooperative-render

# Проверка синтаксиса
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -checkonly docs/cooperative/UC1.md

# SVG
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -tsvg -o /tmp/aims-uc1-cooperative-render docs/cooperative/UC1.md

# PNG
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -tpng -o /tmp/aims-uc1-cooperative-render docs/cooperative/UC1.md
```
