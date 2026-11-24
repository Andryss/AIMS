# AIMS Backend

JDK 17, Allure CLI 2 (`allure` в `PATH`). Для тестов БД — Docker Compose или Podman с Compose-провайдером.

Все команды выполняются из `backend`.

## Запуск приложения

```bash
./mvnw spring-boot:run
```

## Сборка и полный набор тестов

```bash
./mvnw verify
```

## Функциональные тесты

```bash
# Запуск и формирование отчета
./src/test/functional/run.sh

# Запуск через Maven
./mvnw -Pfunctional-tests test

# Запуск одного класса
./mvnw -Pfunctional-tests -Dtest=IncidentServiceImplTest test
```

## Формирование и просмотр отчета

```bash
# Формирование отчёта по последнему запуску
bash src/test/scripts/report.sh target/custom-tests/functional-test

# Просмотр отчёта
allure open target/custom-tests/functional-test/report
```

## Тесты БД

```bash
# Docker
./src/test/pg/run.sh

# Podman
CONTAINER_ENGINE=podman ./src/test/pg/run.sh

# Просмотр отчёта
allure open target/custom-tests/db-test/report
```
