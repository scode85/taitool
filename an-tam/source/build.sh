#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
SDK_PATH="${ANDROID_SDK_ROOT:-/workspace/android-sdk}"
BUILD_TOOLS="${BUILD_TOOLS_PATH:-$SDK_PATH/build-tools/android-15}"
ANDROID_JAR="$SDK_PATH/platforms/android-35/android.jar"
rm -rf build/classes build/dex build/generated
mkdir -p build/classes build/dex build/generated
"$BUILD_TOOLS/aapt2" compile --dir res -o build/resources.zip
"$BUILD_TOOLS/aapt2" link -I "$ANDROID_JAR" --manifest AndroidManifest.xml --version-code 3 --version-name 0.2.1 --java build/generated -o build/base.apk build/resources.zip
java -jar ecj.jar -encoding UTF-8 -source 8 -target 8 -classpath "$ANDROID_JAR" -d build/classes src/vn/phonecare/*.java build/generated/vn/phonecare/R.java
rm -f build/classes.jar
(cd build/classes && zip -qr ../classes.jar .)
"$BUILD_TOOLS/d8" --lib "$ANDROID_JAR" --min-api 26 --output build/dex build/classes.jar
cp build/base.apk build/unsigned.apk
(cd build/dex && zip -q ../unsigned.apk classes.dex)
"$BUILD_TOOLS/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk
if [ ! -f build/local-test.jks ]; then
keytool -genkeypair -keystore build/local-test.jks -storepass android -keypass android -alias phonecare -dname 'CN=Phone Care Local Test' -keyalg RSA -keysize 2048 -validity 3650 -noprompt
fi
"$BUILD_TOOLS/apksigner" sign --ks build/local-test.jks --ks-key-alias phonecare --ks-pass pass:android --key-pass pass:android --out build/AnTam.apk build/aligned.apk
"$BUILD_TOOLS/apksigner" verify --verbose build/AnTam.apk
