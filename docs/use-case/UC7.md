# UC7. Создание записи в базе знаний

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
title Use Case Diagram — UC7. Создание записи в базе знаний
left to right direction

actor "Аналитик-ксенобиолог" as Analyst

rectangle "Alien Incident Management System" {

(Создание записи в базе знаний) as UC11

(Сохранение записи в базе знаний) as UC114
(Ввод данных о виде инопланетянина) as UC111

}

Analyst -- UC11

UC11 --> UC111 : <<include>>
UC11 --> UC114 : <<include>>

@enduml
```

## 8. Interface example (Пример интерефейса)

TBD
