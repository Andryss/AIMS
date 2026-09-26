# Прецеденты использования

[Общая диаграмма прецедентов](common.md)

| Идентификатор | Название |
| --- | --- |
| UC1 | [Регистрация инцидента](UC1.md) |
| UC2 | [Классификация инопланетянина](UC2.md) |
| UC3 | [Назначение ответственных за инцидент](UC3.md) |
| UC4 | [Создание отчёта об очистке](UC4.md) |
| UC5 | [Выполнение инцидента](UC5.md) |
| UC6 | [Завершение инцидента](UC6.md) |
| UC7 | [Создание записи в базе знаний](UC7.md) |
| UC8 | [Редактирование записи в базе знаний](UC8.md) |
| UC9 | [Просмотр истории изменений инцидента](UC9.md) |
| UC10 | [Учёт оборудования](UC10.md) |
| UC11 | [Создание заявки на оборудование](UC11.md) |
| UC12 | [Обработка заявки на оборудование](UC12.md) |
| UC13 | [Создание тендера на закупку оборудования](UC13.md) |
| UC14 | [Добавление участника тендера](UC14.md) |
| UC15 | [Изменение тендера](UC15.md) |
| UC16 | [Выбор победителя тендера](UC16.md) |
| UC17 | [Создание заявки на тюремное заключение](UC17.md) |
| UC18 | [Обработка заявки на тюремное заключение](UC18.md) |
| UC19 | [Автоматическая классификация инопланетянина](UC19.md) |
| UC20 | [Автоматическое определение уровня угрозы инцидента](UC20.md) |
| UC21 | [Просмотр аналитики по инцидентам](UC21.md) |
| UC22 | [Просмотр аналитики по оборудованию](UC22.md) |
| UC23 | [Просмотр аналитики по тюремному заключению](UC23.md) |

## Генерация диаграмм

Подготовка:

```bash
curl -fL https://github.com/plantuml/plantuml/releases/latest/download/plantuml.jar -o /tmp/plantuml.jar
mkdir -p /tmp/aims-use-case-render
```

Проверка синтаксиса:

```bash
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -checkonly docs/use-case/common.md 'docs/use-case/UC*.md'
```

Все диаграммы в SVG:

```bash
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -tsvg -o /tmp/aims-use-case-render \
  docs/use-case/common.md 'docs/use-case/UC*.md'
```

Все диаграммы в PNG:

```bash
java -Djava.awt.headless=true -jar /tmp/plantuml.jar -tpng -o /tmp/aims-use-case-render \
  docs/use-case/common.md 'docs/use-case/UC*.md'
```

Результаты: `/tmp/aims-use-case-render`.
