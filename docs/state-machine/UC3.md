<!-- markdownlint-disable MD013 MD041 -->

```plantuml
@startuml aims_uc3_state_machine_use_case_view
title UC3. Назначение ответственных за инцидент — представление прецедента
scale max 3800 width

skinparam shadowing false
skinparam state {
  BackgroundColor #F7F9FC
  BorderColor #334155
  StartColor #166534
  EndColor #991B1B
}

state "Готов к выполнению" as Ready
state "Подготовка к выполнению" as Preparation
state "Подготовлен к выполнению" as Prepared
state "Требуется уточнение" as Clarification
state "Требуется повторный анализ" as Reanalysis

Ready : entry / Уведомить оперативных агентов
Ready : do / Просмотр карточки
Ready : do / Назначение ответственного
Ready : do / Добавление исполнителей
Preparation : do / Назначение ответственного
Preparation : do / Добавление исполнителей
Preparation : do / Создание заявки на оборудование
Prepared : entry / Уведомить о готовности инцидента
Clarification : entry / Уведомить оператора
Reanalysis : entry / Уведомить аналитиков

[*] --> Ready : Аналитик завершает классификацию
Ready --> Preparation : Агент начинает подготовку
Preparation --> Prepared : Агент завершает подготовку\n[есть ответственный и хотя бы один исполнитель]

Ready --> Clarification : Запрос уточнения
Ready --> Reanalysis : Запрос повторного анализа
Preparation --> Clarification : Запрос уточнения
Preparation --> Reanalysis : Запрос повторного анализа

Prepared --> [*]
Clarification --> [*]
Reanalysis --> [*]
@enduml
```

## Генерация диаграммы

```bash
curl -L https://github.com/plantuml/plantuml/releases/latest/download/plantuml.jar -o /tmp/plantuml.jar
mkdir -p /tmp/aims-uc3-state-machine-render

# Проверка синтаксиса
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -checkonly docs/state-machine/UC3.md

# SVG
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -tsvg -o /tmp/aims-uc3-state-machine-render docs/state-machine/UC3.md

# PNG
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -tpng -o /tmp/aims-uc3-state-machine-render docs/state-machine/UC3.md
```
