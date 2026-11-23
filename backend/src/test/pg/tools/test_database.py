import re
import subprocess
from html import escape
from pathlib import Path

import allure
import pytest

from tools.reporting import read_phases, runner_error, show_phase


TESTS = Path("tests/tables")
SCENARIOS = sorted(TESTS.glob("*/*.sql"))


@pytest.mark.parametrize("sql_file", SCENARIOS, ids=lambda file: str(file.relative_to(TESTS)))
def test_database(sql_file):
    source = sql_file.read_text()
    metadata = dict(re.findall(r"^--\s*@(\w+)\s+(.+)$", source, re.MULTILINE))
    allure.dynamic.title(metadata["name"])
    allure.dynamic.parent_suite("Тестирование базы данных")
    allure.dynamic.suite("Тестирование таблицы " + sql_file.parent.name)
    allure.dynamic.description_html(
        f"<b>Объект</b> — <code>{escape(metadata['object'])}</code><br><br>"
        f"<b>Правило</b> — <code>{escape(metadata['rule'])}</code>"
    )
    allure.dynamic.parameter("sql_file", str(sql_file))

    result = subprocess.run(
        ["pg_prove", "--norc", "--verbose", "--set", "ON_ERROR_STOP=1", str(sql_file)],
        capture_output=True,
        text=True,
    )
    log = Path("/output/results/logs") / sql_file.relative_to(TESTS).with_suffix(".log")
    log.parent.mkdir(parents=True, exist_ok=True)
    log.write_text(result.stdout + "\n" + result.stderr)

    phases = read_phases(result.stdout)
    error = runner_error(result.returncode, result.stdout, result.stderr, phases)
    failed = False
    for name, facts in phases:
        try:
            show_phase(name, facts)
        except AssertionError:
            failed = True

    if error or failed:
        allure.attach.file(log, "Журнал SQL-проверок", allure.attachment_type.TEXT)
        allure.attach(source, "SQL-сценарий", allure.attachment_type.TEXT)
    if error:
        with allure.step("Ошибка выполнения SQL"):
            raise RuntimeError(error)
    if failed:
        pytest.fail("Результат SQL-проверки отличается от ожидаемого", pytrace=False)
