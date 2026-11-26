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

## Тестирование бизнес-цикла

Установите JDK 17, Node.js 20+ с npm и Allure CLI 2.46.1. Добавьте `allure` в `PATH`.

```bash
# Запуск всех E2E-тестов и формирование отчёта
./src/test/e2e/run.sh

# Запуск одного класса
./src/test/e2e/run.sh -Dtest=IncidentRegistrationE2ETest
./src/test/e2e/run.sh -Dtest=IncidentClassificationE2ETest

# Повторное формирование отчёта по последнему запуску
bash src/test/scripts/report.sh target/custom-tests/e2e-test

# Просмотр отчёта
allure open target/custom-tests/e2e-test/report
```

Чтобы добавить сценарий:

1. Создайте класс `*E2ETest`, наследующий `BaseE2ETest`.
2. Добавьте `@ParameterizedTest` и `@MethodSource("browsers")`. Перед действиями в браузере вызовите `startBrowser(browser)`.
3. Укажите действия участников в `@Description`, а требования — через `Allure.parameter("Связанные требования", ...)`.
4. Получайте общие данные через `fixtures.account(...)` и `fixtures.alien(...)`. Для предусловий используйте `fixtures.storeFile(...)` и `fixtures.createIncident(...)`, задавая нужный статус, данные и вложения.
5. Оформляйте действия самого прецедента через `uiStep("Название действия", ...)`, чтобы прикладывать скриншот результата к шагу Allure. Выполняйте авторизацию при подготовке через `LoginPage.signIn(account)`, если она не является предметом прецедента.
6. Используйте существующие Page Objects или добавьте новый класс в `src/test/java/gov/mib/aims/e2e/pages`.

Чтобы добавить браузер, реализуйте его создание в `E2EBrowser` и добавьте его в список `BROWSERS` в `BaseE2ETest`.

## Формирование и просмотр функционального отчёта

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
