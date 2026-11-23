#!/usr/bin/env bash
set -euo pipefail

CONTAINER_ENGINE="${CONTAINER_ENGINE:-docker}"

cd "$(dirname "$0")"

TEST_UID=$(id -u)
TEST_GID=$(id -g)
TEST_COMMIT=$(git rev-parse HEAD)
COMPOSE_PROJECT_NAME=aims-db-tests-$$
export TEST_UID TEST_GID TEST_COMMIT COMPOSE_PROJECT_NAME

output=../../../target/custom-tests/db-test
mkdir -p "$output"
mkdir "$output/.run.lock"
trap '
  status=$?
  "$CONTAINER_ENGINE" compose down --volumes --remove-orphans || status=$?
  rmdir "$output/.run.lock"
  exit "$status"
' EXIT

rm -rf "$output/results" "$output/report"
mkdir -p "$output/results/logs" "$output/history"

"$CONTAINER_ENGINE" compose build db tests
"$CONTAINER_ENGINE" compose up -d --wait --wait-timeout 60 db
"$CONTAINER_ENGINE" compose run --rm migrations 2>&1 | tee "$output/results/logs/migrations.log"
"$CONTAINER_ENGINE" compose run --rm tests psql -X -v ON_ERROR_STOP=1 -f support/setup.sql \
  2>&1 | tee "$output/results/logs/setup.log"

status=0
"$CONTAINER_ENGINE" compose run --rm tests || status=$?
"$CONTAINER_ENGINE" compose run --rm report

exit "$status"
