#!/usr/bin/env bash
# Compiles and starts FreshMart
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 -Xlint:all -d out src/freshmart/*.java
java -cp out freshmart.EUK00361749_Main "$@"
