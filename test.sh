#!/usr/bin/env bash
# Compiles the project and runs the automated tests twice:
#   1. JUnit 5 (lib/junit-platform-console-standalone) - one JUnit test per table row
#   2. The table runner, which saves the report's test table to docs/evidence/test-results.txt
set -euo pipefail
cd "$(dirname "$0")"
JUNIT=lib/junit-platform-console-standalone-1.11.4.jar
mkdir -p out docs/evidence
javac -encoding UTF-8 -Xlint:all -cp "$JUNIT" -d out src/freshmart/*.java test/freshmart/*.java
java -jar "$JUNIT" execute --class-path out --select-class freshmart.EUK00361749_JUnitTests --details=tree --disable-banner
java -cp out freshmart.EUK00361749_Tests > docs/evidence/test-results.txt
echo
echo "Test table saved to docs/evidence/test-results.txt"
tail -1 docs/evidence/test-results.txt
