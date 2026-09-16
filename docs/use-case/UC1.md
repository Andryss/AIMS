# Регистрация инцидента

## Диаграмма прецедента

```plantuml
@startuml
title UC1. Регистрация инцидента
left to right direction

actor "Оператор мониторинга" as Operator
actor "Внешняя система мониторинга" as ExternalMonitoring

rectangle "Alien Incident Management System" {

(Отправка уведомления о подозрительной активности) as UC01
(Получение уведомления о подозрительной активности) as UC02

(Создание инцидента) as UC11

(Изменение статуса инцидента) as UC116
(Создание заявки на оборудование) as UC115
(Сохранение инцидента) as UC114
(Проверка корректности данных) as UC113
(Добавление медиа материалов к инциденту) as UC112
(Ввод первичных данных инцидента) as UC111

}

UC01 -- ExternalMonitoring
Operator -- UC02

Operator -- UC11

UC11 --> UC111 : <<include>>
UC11 --> UC112 : <<include>>
UC11 --> UC113 : <<include>>
UC11 --> UC114 : <<include>>
UC11 <-- UC115 : <<extend>>
UC11 --> UC116 : <<include>>

@enduml
```

## Пример интерефейса

```plantuml
@startsalt
{+ 
    Форма регистрации инцидента
    {
        Дата и время: | "2026-05-11 10:30"
        Место: | "Центральный вокзал"
        Тип события: | ^Подозрение на контакт^
        Описание: | {+
           Замечен неизвестный субъект
           .
           "                            "
        }
        Материалы: | [Добавить файл]
        
        [ Сохранить ] | [ Отмена ]
    }
}
@endsalt
```

```plantuml
@startsalt
{+ 
    Инцидент №612
    {
        Статус | ^ Черновик ^
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
    }
}
@endsalt
```

```plantuml
@startsalt
{ 
    Статус | ^Черновик       ^^ Готов к анализу ^
}
@endsalt
```

## Генерация диаграмм

```bash
curl -L https://github.com/plantuml/plantuml/releases/latest/download/plantuml.jar -o /tmp/plantuml.jar
mkdir -p /tmp/aims-uc1-use-case-render

# Проверка синтаксиса
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -checkonly docs/use-case/UC1.md

# SVG
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -tsvg -o /tmp/aims-uc1-use-case-render docs/use-case/UC1.md

# PNG
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -tpng -o /tmp/aims-uc1-use-case-render docs/use-case/UC1.md
```
