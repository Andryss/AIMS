#!/usr/bin/env bash
set -euo pipefail

profile=${1:?Укажите Maven-профиль}
output_name=${2:?Укажите имя каталога результатов}
phase=${3:?Укажите фазу Maven}
shift 3
backend_dir=$(cd "$(dirname "$0")/../../.." && pwd)
output="$backend_dir/target/custom-tests/$output_name"

if [[ -z "${JAVA_HOME:-}" && -x /usr/libexec/java_home ]]; then
    JAVA_HOME=$(/usr/libexec/java_home -v 17)
    export JAVA_HOME
fi

cd "$backend_dir"
mkdir -p "$output/logs"
mkdir "$output/.run.lock"
trap 'task_exit=$?; rmdir "$output/.run.lock"; exit "$task_exit"' EXIT

revision=$(git rev-parse HEAD)
set +e
./mvnw -B -P"$profile" -Dfunctional.revision="$revision" "$@" "$phase" \
    2>&1 | tee "$output/logs/maven.log"
test_exit=("${PIPESTATUS[@]}")
bash "$backend_dir/src/test/scripts/report.sh" "$output" \
    2>&1 | tee "$output/logs/report.log"
report_exit=("${PIPESTATUS[@]}")
set -e

for task_exit in "${test_exit[@]}" "${report_exit[@]}"; do
    if [[ "$task_exit" -ne 0 ]]; then
        exit "$task_exit"
    fi
done
