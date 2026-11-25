#!/usr/bin/env bash
set -euo pipefail

test_dir=$(cd "$(dirname "$0")/.." && pwd)
exec bash "$test_dir/scripts/run-maven.sh" e2e e2e-test test "$@"
