#!/usr/bin/env bash
set -euo pipefail

output=${1:?Укажите каталог результатов прогона}

mkdir -p "$output/history"
rm -rf "$output/results/history"
cp -R "$output/history" "$output/results/history"
allure generate "$output/results" --clean -o "$output/report"
cp -R "$output/report/history/." "$output/history/"
