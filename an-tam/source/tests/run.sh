#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/test-classes
java -jar "${ECJ_JAR:-ecj.jar}" -source 8 -target 8 -d build/test-classes src/vn/phonecare/SessionTimer.java src/vn/phonecare/ForegroundState.java src/vn/phonecare/ForegroundChoice.java tests/vn/phonecare/*.java
java -cp build/test-classes vn.phonecare.SessionTimerTest
java -cp build/test-classes vn.phonecare.ForegroundStateTest
java -cp build/test-classes vn.phonecare.ForegroundChoiceTest
