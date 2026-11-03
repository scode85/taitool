#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/test-classes
java -jar "${ECJ_JAR:-ecj.jar}" -source 8 -target 8 -classpath tests/lib/json.jar -d build/test-classes src/vn/chamanh/ImageMatcher.java src/vn/chamanh/StepSpec.java src/vn/chamanh/RunClock.java src/vn/chamanh/TrialPlan.java src/vn/chamanh/LoopPolicy.java tests/vn/chamanh/*.java
java -cp build/test-classes vn.chamanh.ImageMatcherTest
java -cp build/test-classes:tests/lib/json.jar vn.chamanh.StepSpecTest
java -cp build/test-classes:tests/lib/json.jar vn.chamanh.TrialPlanTest
java -cp build/test-classes:tests/lib/json.jar vn.chamanh.LoopPolicyTest
