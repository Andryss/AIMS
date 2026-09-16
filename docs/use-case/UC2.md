# Классификация инопланетянина

## Диаграмма прецедента

```plantuml
@startuml
title UC2. Классификация инопланетянина
left to right direction

actor "Аналитик-ксенобиолог" as Analyst

rectangle "Alien Incident Management System" {

(Получение уведомления о готовом к анализу инциденте) as UC01

(Классификация инопланетянина) as UC11

(Изменение статуса инцидента) as UC115
(Привязывание записи базы знаний к инциденту) as UC114
(Создание новой записи в базе знаний) as UC113
(Поиск записи в базе знаний инопланетян) as UC112
(Выполнение автоматической классификации инопланетянина) as UC111

}

Analyst -- UC01

Analyst -- UC11

UC11 <-- UC111 : <<extend>>
UC11 --> UC112 : <<include>>
UC11 <-- UC113 : <<extend>>
UC11 --> UC114 : <<include>>
UC11 --> UC115 : <<include>>

@enduml
```

## Пример интерефейса

```plantuml
@startsalt
{ 
    Поиск по базе знаний
    {
        "слиз"
        Найдено 2 совпадения:
        {+
            Тип: | "Слизень"
            Описание: | {+
               Жидкая зеленая субстанция
               .
               "                              "
            }
            Уровень угрозы: | "3 (Низкий)"
        }
        {+
            Тип: | "Слизистый червь"
            Описание: | {+
               Длинный толстый червь-людоед
               .
               "                              "
            }
            Уровень угрозы: | "6 (Средний)"
        }
    }
}
@endsalt
```

```plantuml
@startsalt
{+ 
    Инцидент №612
    {
        Статус | ^ Готов к анализу ^
        Дата и время: | "2026-05-11 10:30"
        Место: | "Центральный вокзал"
        Тип события: | ^Подозрение на контакт^
        Описание: | {+
           Замечен неизвестный субъект
        }
        Материалы: | {
            [Изображение 1]
            [Изображение 2]
            [Изображение 3]
        }
        Тип инопланетянина: | [ Выбрать ]
    }
}
@endsalt
```

```plantuml
@startsalt
{ 
    Статус | ^Готов к анализу       ^^ Готов к выполнению ^^ Требуется уточнение ^
}
@endsalt
```

## Генерация диаграмм

```bash
curl -L https://github.com/plantuml/plantuml/releases/latest/download/plantuml.jar -o /tmp/plantuml.jar
mkdir -p /tmp/aims-uc2-use-case-render

# Проверка синтаксиса
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -checkonly docs/use-case/UC2.md

# SVG
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -tsvg -o /tmp/aims-uc2-use-case-render docs/use-case/UC2.md

# PNG
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -tpng -o /tmp/aims-uc2-use-case-render docs/use-case/UC2.md
```
