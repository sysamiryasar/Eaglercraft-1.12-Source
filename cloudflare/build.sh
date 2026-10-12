#!/bin/bash
# Cloudflare build: compiles Sythix Client and writes the browser version to build/web.
#
# Cloudflare's build machine has Python but no Java, and the game needs JDK 17 to compile (the
# Gradle 8.0 wrapper does not run on newer Java). If no JDK 17 is found, this downloads Eclipse
# Temurin 17 (GPLv2 + Classpath Exception) into .cache/jdk17 for the build only; it is not
# published. Then it runs the normal web build (CompileWebRuntime.sh).
set -euo pipefail
cd "$(dirname "$0")/.."

java_major() {
    "$1/bin/java" -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -n 1
}

# SYTHIX_DOWNLOAD_JDK=1 forces the download (to test the path Cloudflare takes)
if [ "${SYTHIX_DOWNLOAD_JDK:-}" != "1" ] && [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/javac" ] && [ "$(java_major "$JAVA_HOME")" = "17" ]; then
    echo "Using JDK 17 at $JAVA_HOME"
elif [ "${SYTHIX_DOWNLOAD_JDK:-}" != "1" ] && [ -x /usr/lib/jvm/java-17-openjdk-amd64/bin/javac ]; then
    export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
    echo "Using JDK 17 at $JAVA_HOME"
else
    JDK=.cache/jdk17
    if [ ! -x "$JDK/bin/javac" ]; then
        echo "Downloading JDK 17 (Eclipse Temurin) for the build..."
        rm -rf "$JDK" && mkdir -p "$JDK"
        curl -fsSL --retry 3 "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse" | tar -xz -C "$JDK" --strip-components=1
    fi
    export JAVA_HOME="$PWD/$JDK"
    echo "Using downloaded JDK at $JAVA_HOME ($("$JAVA_HOME/bin/java" -version 2>&1 | head -n 1))"
fi

export PATH="$JAVA_HOME/bin:$PATH"
chmod +x gradlew CompileWebRuntime.sh
./CompileWebRuntime.sh

# the site needs these; stop the build (instead of publishing a broken site) if one is missing
for f in index.html sythix-web.js sw.js files.json preload.json sythix-client.jar lwjgl/libraries/gl4es.wasm lwjgl/libraries/liblwjgl.so lwjgl/libraries/lwjgl.js lwjgl/lwjgl-2.9.3.jar; do
    if [ ! -s "build/web/$f" ]; then
        echo "ERROR: build/web/$f is missing" >&2
        exit 1
    fi
done

python3 - <<'PY'
import json, os, sys
files = json.load(open("build/web/files.json"))["classpath"]
missing = [f for f in files if not os.path.isfile("build/web/" + f)]
big = [(f, os.path.getsize(os.path.join(r, f))) for r, _, fs in os.walk("build/web") for f in fs if os.path.getsize(os.path.join(r, f)) > 25 * 1024 * 1024]
if missing:
    sys.exit("ERROR: files.json lists missing files: %s" % missing)
if big:
    sys.exit("ERROR: Cloudflare allows 25 MiB per file, too big: %s" % big)
print("Site ready in build/web (%d jars on the classpath, all present)" % len(files))
PY
