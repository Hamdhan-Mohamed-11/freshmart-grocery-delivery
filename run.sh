#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p out
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -Xlint:all -d out src/freshmart/*.java
java -cp out freshmart.EUK00361749_Main "$@"
