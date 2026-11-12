#!/usr/bin/env bash
# Compiles and runs the regression tests; results are also saved to docs/evidence/test-results.txt
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p out docs/evidence
javac -encoding UTF-8 -Xlint:all -d out src/freshmart/*.java test/freshmart/*.java
java -cp out freshmart.EUK00361749_Tests | tee docs/evidence/test-results.txt
