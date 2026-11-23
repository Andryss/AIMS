import json
import os
import platform
import subprocess
import xml.etree.ElementTree as ET
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo


REPORT_TIMEZONE = ZoneInfo("Europe/Moscow")


def pytest_sessionstart(session):
    started_at = datetime.now(REPORT_TIMEZONE)
    session.db_tests_started_at = started_at
    version = ET.parse("/work/pom.xml").getroot().findtext("{*}version")
    database = subprocess.check_output(
        ["psql", "-XAt", "-c", """
            SELECT json_build_object(
                'PostgreSQL', version(),
                'pgTAP', (SELECT extversion FROM pg_extension WHERE extname = 'pgtap'),
                'Timezone', current_setting('TimeZone'),
                'Encoding', current_setting('server_encoding'));
        """],
        text=True,
    )
    environment = {
        "Application.version": version,
        "Git.commit": os.environ["TEST_COMMIT"],
        "Container": platform.platform(),
        **json.loads(database),
        "Run.started_at": started_at.isoformat(timespec="milliseconds"),
        "Run.timezone": REPORT_TIMEZONE.key,
    }
    output = Path("/output/results")
    (output / "environment.properties").write_text(
        "".join(f"{name}={value}\n" for name, value in environment.items())
    )
    (output / "executor.json").write_text(json.dumps({
        "name": "Docker Compose",
        "type": "local",
        "buildName": started_at.isoformat(timespec="milliseconds"),
        "buildOrder": int(started_at.timestamp() * 1000),
        "reportName": "Отчёт о тестировании базы данных",
    }, ensure_ascii=False))


def pytest_sessionfinish(session):
    finished_at = datetime.now(REPORT_TIMEZONE)
    duration = (finished_at - session.db_tests_started_at).total_seconds()
    with Path("/output/results/environment.properties").open("a") as output:
        output.write(f"Run.finished_at={finished_at.isoformat(timespec='milliseconds')}\n")
        output.write(f"Run.duration={duration:.3f} s\n")
