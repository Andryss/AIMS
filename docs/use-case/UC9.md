# UC9. Просмотр истории изменений инцидента

## 1.Use-Case Name (Название прецедента)

UC9. Просмотр истории изменений инцидента.

Связанные требования: 3.1.1.4.

## 2.Actors (Акторы)

Основное действующее лицо: Сотрудник.

## 2. Brief Description (Краткое описание)

Сотрудник открывает карточку инцидента и просматривает историю его изменений, включая время изменения и сведения о
пользователе, внесшем изменение, для анализа хода обработки инцидента.

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
title Use Case Diagram — UC9. Просмотр истории изменений инцидента
left to right direction

actor "Сотрудник" as Employee

rectangle "Alien Incident Employee System" {

(Просмотр истории изменений инцидента) as UC11

(Просмотр автора изменения) as UC113
(Просмотр карточки инцидента) as UC111

}

Employee -- UC11

UC11 --> UC111 : <<include>>
UC11 --> UC113 : <<include>>

@enduml
```

## 8. Interface example (Пример интерефейса)

TBD
