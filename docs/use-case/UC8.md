# UC8. Редактирование записи в базе знаний

## 1.Use-Case Name (Название прецедента)

TBD

## 2.Actors (Акторы)

TBD

## 2. Brief Description (Краткое описание)

TBD

## 3. Flow of Events (Последовательность событий)

### 3.1 Basic Flow (Главная последовательность)

TBD

### 3.2 Alternative Flows (Альтернативные последовательности)

TBD

## 4. Preconditions (Предусловия)

TBD

## 5. Postconditions (Постусловия)

TBD

## 6. Extension Points (Точки расширения)

TBD

## 7. Use-case diagram (Диаграмма прецедента)

```plantuml
@startuml
title Use Case Diagram — UC8. Редактирование записи в базе знаний
left to right direction

actor "Аналитик-ксенобиолог" as Analyst

rectangle "Alien Incident Management System" {

(Редактирование записи в базе знаний) as UC11

(Сохранение изменений записи) as UC115
(Изменение данных записи базы знаний) as UC113
(Просмотр выбранной записи) as UC112
(Поиск записи в базе знаний) as UC111

}

Analyst -- UC11

UC11 --> UC111 : <<include>>
UC11 --> UC112 : <<include>>
UC11 --> UC113 : <<include>>
UC11 --> UC115 : <<include>>

@enduml
```

## 8. Interface example (Пример интерефейса)

TBD
