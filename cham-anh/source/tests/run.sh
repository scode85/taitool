#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/test-classes
java -jar "${ECJ_JAR:-ecj.jar}" -source 8 -target 8 -d build/test-classes src/vn/chamanh/ImageMatcher.java tests/vn/chamanh/ImageMatcherTest.java
java -cp build/test-classes vn.chamanh.ImageMatcherTest
