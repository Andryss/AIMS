# UC6. Завершение инцидента

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
title Use Case Diagram — UC6. Завершение инцидента
left to right direction

actor "Оперативный агент" as Agent

rectangle "Alien Incident Management System" {

(Завершение инцидента) as UC11

(Сохранение изменений инцидента) as UC115
(Изменение статуса инцидента) as UC114
(Просмотр карточки инцидента) as UC111

}

Agent -- UC11

UC11 --> UC111 : <<include>>
UC11 --> UC114 : <<include>>
UC11 --> UC115 : <<include>>

@enduml
```

## 8. Interface example (Пример интерефейса)

TBD
