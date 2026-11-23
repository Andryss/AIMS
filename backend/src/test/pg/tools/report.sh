#!/usr/bin/env bash
set -eu

cp -R /output/history /output/results/history
allure generate /output/results --clean -o /output/report
cp -R /output/report/history/. /output/history/
