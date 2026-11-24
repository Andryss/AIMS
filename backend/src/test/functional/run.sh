#!/usr/bin/env bash
set -euo pipefail

test_dir=$(cd "$(dirname "$0")/.." && pwd)
exec bash "$test_dir/scripts/run-maven.sh" functional-tests functional-test test "$@"
