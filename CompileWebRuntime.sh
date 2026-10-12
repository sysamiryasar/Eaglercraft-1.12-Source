#!/bin/bash
# Builds the browser version of Sythix Client into build/web.
#
#   ./CompileWebRuntime.sh            build it
#   ./CompileWebRuntime.sh serve      build it and serve it on http://localhost:8080
#
# The browser version runs the normal client with CheerpJ (Java in WebAssembly). Upload the
# build/web folder to any web host (GitHub Pages, Netlify...) or serve it locally.
set -e
cd "$(dirname "$0")"

./gradlew build --no-daemon -q

python3 - <<'PY'
import json, os, shutil, zipfile

out = "build/web"
# empty the folder but keep it, so a running "serve" keeps working after a rebuild
os.makedirs(out, exist_ok=True)
for name in os.listdir(out):
    path = os.path.join(out, name)
    shutil.rmtree(path) if os.path.isdir(path) else os.remove(path)
shutil.copytree("web", out, dirs_exist_ok=True, ignore=shutil.ignore_patterns("proxy", "__pycache__"))
os.makedirs(out + "/lib", exist_ok=True)

# The game jar without music/sounds (the browser version has no sound) and only English text,
# which makes it about 30 MB instead of 150 MB.
# The built-in texture packs go into their own jars of at most ~20 MB, because static hosts such as
# Cloudflare allow 25 MiB per file.
src = zipfile.ZipFile("build/libs/EaglerGradle-1.0.0.jar")
dst = zipfile.ZipFile(out + "/sythix-client.jar", "w", zipfile.ZIP_DEFLATED)
pack_jars, pack_jar, pack_size = [], None, 0
for item in src.infolist():
    n = item.filename
    if n.startswith("assets/minecraft/sounds/") and n.endswith(".ogg"):
        continue
    if n.startswith("assets/minecraft/lang/") and not n.endswith("en_us.lang"):
        continue
    if n.startswith("sythix/texturepacks/") and not n.endswith("/"):
        if pack_jar is None or pack_size + item.compress_size > 20 * 1024 * 1024:
            if pack_jar:
                pack_jar.close()
            pack_jars.append("sythix-packs-%d.jar" % (len(pack_jars) + 1))
            pack_jar = zipfile.ZipFile(out + "/" + pack_jars[-1], "w", zipfile.ZIP_DEFLATED)
            pack_size = 0
        pack_jar.writestr(item, src.read(n))
        pack_size += item.compress_size
        continue
    dst.writestr(item, src.read(n))
dst.close()
if pack_jar:
    pack_jar.close()

# The same libraries as the desktop runtime, except LWJGL (the browser needs 2.9.3, matching its
# WebAssembly natives) and libraries that only wrap desktop native code.
skip = ("lwjgl",)
classpath = ["sythix-client.jar"] + pack_jars + ["lwjgl/lwjgl-2.9.3.jar", "lwjgl/lwjgl_util-2.9.3.jar"]
for root, _, files in os.walk("lib/libraries"):
    for f in sorted(files):
        if f.endswith(".jar") and not f.startswith(skip) and "natives" not in f and "-sources" not in f:
            shutil.copy(os.path.join(root, f), out + "/lib/" + f)
            classpath.append("lib/" + f)

with open(out + "/files.json", "w") as fh:
    # version: changes with every build, so returning players' saved files are refreshed (sw.js)
    import hashlib, time
    version = hashlib.sha1(("".join(classpath) + str(time.time())).encode()).hexdigest()[:12]
    json.dump({"classpath": classpath, "version": version}, fh, indent=1)

size = sum(os.path.getsize(os.path.join(r, f)) for r, _, fs in os.walk(out) for f in fs)
print(f"browser build written to {out} ({size / 1e6:.0f} MB, {len(classpath)} jars)")
PY

if [ "$1" = "serve" ]; then
    echo "Open http://localhost:8080 in your browser (Ctrl+C stops the server)"
    cd build/web && python3 serve.py 8080
fi
