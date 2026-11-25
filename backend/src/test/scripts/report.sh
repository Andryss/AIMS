#!/usr/bin/env bash
set -euo pipefail

output=${1:?Укажите каталог результатов прогона}

if ! compgen -G "$output/results/*-result.json" > /dev/null; then
    echo "Нет результатов JUnit для формирования Allure-отчёта: $output/results" >&2
    exit 1
fi

mkdir -p "$output/history"
if [[ ! -d "$output/results/history" ]]; then
    cp -R "$output/history" "$output/results/history"
fi
allure generate "$output/results" --clean -o "$output/report"
cp -R "$output/report/history/." "$output/history/"
