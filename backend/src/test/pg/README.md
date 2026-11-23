# Тестирование БД

```bash
# Запуск БД, миграций, тестов и генерации отчёта из корня репозитория (Docker)
./backend/src/test/pg/run.sh

# Запуск через Podman (нужен установленный Compose-провайдер)
CONTAINER_ENGINE=podman ./backend/src/test/pg/run.sh

# Просмотр отчёта
python3 -m http.server 8877 --bind 127.0.0.1 \
  --directory backend/target/custom-tests/db-test/report

# Открыть в браузере: http://127.0.0.1:8877/
```

```text
backend/src/test/pg/
├── README.md                 # Команды запуска и состав каталога
├── run.sh                    # БД → миграции → тесты → отчёт
├── compose.yaml              # Тестовый стенд Docker Compose
├── docker/
│   ├── postgres.Dockerfile   # PostgreSQL с pgTAP
│   └── runner.Dockerfile     # psql, pg_prove, pytest и Allure
├── support/
│   ├── setup.sql             # Установка pgTAP и общих функций
│   ├── fixtures.sql          # Создание тестовых объектов
│   └── reporting.sql         # SQL-проверки и данные для отчёта
├── tests/tables/
│   ├── app_user/             # SQL-тесты таблицы app_user
│   └── stored_file/          # SQL-тесты таблицы stored_file
└── tools/
    ├── conftest.py           # Окружение и временные отметки прогона
    ├── test_database.py      # Запуск SQL-сценариев через pg_prove
    ├── reporting.py          # Шаги и вложения Allure
    └── report.sh             # Генерация HTML и сохранение истории

backend/target/custom-tests/db-test/
├── results/                  # Результаты Allure, вложения и журналы
├── report/                   # HTML-отчёт
└── history/                  # История прогонов
```
