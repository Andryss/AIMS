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
  participant "Карточка инцидента" as IncidentCard
  participant "Координатор очистки" as Cleanup
  participant "Редактор отчёта" as Report
  participant "Загрузка материалов" as Materials
end box

Cleaner -> IncidentCard: Открыть карточку инцидента
IncidentCard -> Cleanup: Получить состояние очистки
Cleanup --> IncidentCard: Текущий статус и наличие отчёта
IncidentCard --> Cleaner: Показать сведения об инциденте

Cleaner -> IncidentCard: Начать подготовку к очистке
IncidentCard -> Cleanup: Установить статус «Подготовка»
Cleanup -> Cleanup: Проверить допустимость перехода
Cleanup --> IncidentCard: Статус изменён
IncidentCard --> Cleaner: Показать статус «Подготовка»

Cleaner -> IncidentCard: Начать выполнение очистки
IncidentCard -> Cleanup: Установить статус «Выполнение»
Cleanup -> Cleanup: Проверить допустимость перехода
Cleanup --> IncidentCard: Статус изменён
IncidentCard --> Cleaner: Показать статус «Выполнение»

Cleaner -> IncidentCard: Открыть создание отчёта
IncidentCard -> Report: Начать формирование отчёта
Report --> IncidentCard: Данные нового отчёта
IncidentCard --> Cleaner: Показать форму отчёта

Cleaner -> IncidentCard: Ввести описание выполненной очистки
IncidentCard -> Report: Добавить описание
Report -> Report: Проверить, что описание заполнено

Cleaner -> IncidentCard: Прикрепить фото и видеоматериалы
IncidentCard -> Materials: Добавить материалы к отчёту
Materials -> Materials: Проверить наличие хотя бы одного материала
Materials --> IncidentCard: Материалы приняты

Cleaner -> IncidentCard: Сохранить отчёт об очистке
IncidentCard -> Report: Сохранить описание и материалы
Report -> Report: Проверить полноту отчёта
Report -> Materials: Получить приложенные материалы
Materials --> Report: Передать материалы
Report -> Cleanup: Зарегистрировать сохранённый отчёт
Cleanup --> IncidentCard: Отчёт сохранён
IncidentCard --> Cleaner: Подтвердить сохранение отчёта

Cleaner -> IncidentCard: Завершить очистку
IncidentCard -> Cleanup: Установить статус «Завершена»
Cleanup -> Cleanup: Проверить допустимость перехода
Cleanup -> Report: Проверить наличие сохранённого отчёта
Report --> Cleanup: Отчёт существует
Cleanup --> IncidentCard: Статус изменён
IncidentCard --> Cleaner: Показать статус «Завершена»
@enduml
```

## Генерация диаграммы

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
