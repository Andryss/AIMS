# Создание отчёта об очистке

## Диаграмма прецедента

```plantuml
@startuml
title UC4. Создание отчёта об очистке
left to right direction

actor "Специалист по прикрытию" as Cleaner

rectangle "Alien Incident Management System" {

(Создание отчёта об очистке) as UC11

(Изменение статуса очистки) as UC115
(Сохранение отчета об очистке) as UC114
(Прикрепление фото и видео материалов) as UC113
(Ввод сведений о выполненной очистке) as UC112
(Просмотр карточки инцидента) as UC111

}

Cleaner -- UC11

UC11 --> UC111 : <<include>>
UC11 --> UC112 : <<include>>
UC11 --> UC113 : <<include>>
UC11 --> UC114 : <<include>>
UC11 --> UC115 : <<include>>

@enduml
```

## Пример интерефейса

```plantuml
@startsalt
{+ 
    Инцидент №612
    {
        Статус | ^ Выполняется ^
        Дата и время: | "2026-05-11 10:30"
        {
            .
            <иные поля>
            .
        }
        Статус очистки: | ^Выполняется^
        Отчет об очистке: | [ Открыть ]
    }
}
@endsalt
```

```plantuml
@startsalt
{+ 
    Отчет об очистке
    {
        Инцидент: | "№612"
        Статус: | ^Выполняется^
        Описание выполнения: {+
            .
            .
            "                  "
        }
        Материалы: | {
            [Изображение 1]
            [Изображение 2]
            [Изображение 3]
            [ Добавить ]
        }
    }
    [ Сохранить ]
}
@endsalt
```

## Генерация диаграмм

```bash
curl -L https://github.com/plantuml/plantuml/releases/latest/download/plantuml.jar -o /tmp/plantuml.jar
mkdir -p /tmp/aims-uc4-use-case-render

# Проверка синтаксиса
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -checkonly docs/use-case/UC4.md

# SVG
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -tsvg -o /tmp/aims-uc4-use-case-render docs/use-case/UC4.md

# PNG
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -tpng -o /tmp/aims-uc4-use-case-render docs/use-case/UC4.md
```
