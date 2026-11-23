import json
import re

import allure


def read_phases(output):
    phases = [("Проверка", [])]
    prefix = "# AIMS_FACT "

    for line in output.splitlines():
        if not line.startswith(prefix):
            continue
        fact = json.loads(line[len(prefix):])
        if fact["kind"] == "phase":
            phases.append((fact["name"], []))
        else:
            phases[-1][1].append(fact)

    return [(name, facts) for name, facts in phases if facts]


def runner_error(returncode, output, stderr, phases):
    if re.search(r"(?:^|:\s)(?:ERROR|FATAL|PANIC):", stderr, re.MULTILINE):
        return "Неожиданная ошибка SQL. Подробности в журнале psql."
    if re.search(r"^Bail out!", output, re.MULTILINE | re.IGNORECASE):
        return "Раннер прервал выполнение SQL-сценария."
    if "Parse errors:" in output:
        return "Нарушен план или формат TAP. Подробности в журнале запуска."

    checks = [fact for _, facts in phases for fact in facts if fact["kind"] == "check"]
    summary = re.search(r"^Files=\d+,\s*Tests=(\d+)(?:,|$)", output, re.MULTILINE)
    if not checks or not summary or len(checks) != int(summary[1]):
        return "Для части проверок отсутствуют ожидаемые и фактические значения."
    if returncode and all(check["passed"] for check in checks):
        return "pg_prove завершился с ошибкой. Подробности в журнале запуска."
    return None


def show_phase(name, facts):
    with allure.step(name):
        failures = []
        for fact in facts:
            try:
                show_fact(fact)
            except AssertionError:
                failures.append(fact["name"])
        assert not failures, "Не пройдены проверки: " + ", ".join(failures)


def show_fact(fact):
    with allure.step(fact["name"]):
        if "sql" in fact:
            allure.attach(fact["sql"], "SQL", allure.attachment_type.TEXT)

        if fact["kind"] == "check":
            comparison = {"Ожидалось": fact["expected"], "Получено": fact["actual"]}
            if fact.get("message"):
                comparison["Сообщение PostgreSQL"] = fact["message"]
            allure.attach(
                json.dumps(comparison, ensure_ascii=False, indent=2),
                "Результат проверки",
                allure.attachment_type.JSON,
            )
            assert fact["passed"], json.dumps(comparison, ensure_ascii=False)
        else:
            allure.attach(
                json.dumps(fact["actual"], ensure_ascii=False, indent=2),
                "Состояние данных" if fact["kind"] == "snapshot" else "Изменено строк",
                allure.attachment_type.JSON,
            )
