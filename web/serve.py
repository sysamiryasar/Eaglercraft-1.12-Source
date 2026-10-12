#!/usr/bin/env python3
"""
A small web server for testing the browser build locally: like "python3 -m http.server" but it
also answers HTTP Range requests (parts of a file), which CheerpJ needs.

  python3 serve.py [port]      (run it inside the build/web folder; default port 8080)
"""
import http.server
import os
import sys


class RangeHandler(http.server.SimpleHTTPRequestHandler):
    extensions_map = dict(http.server.SimpleHTTPRequestHandler.extensions_map, **{".wasm": "application/wasm", ".js": "text/javascript", ".mjs": "text/javascript"})

    def send_head(self):
        path = self.translate_path(self.path)
        rng = self.headers.get("Range")

        if rng is None or not os.path.isfile(path):
            return super().send_head()

        size = os.path.getsize(path)
        spec = rng.strip().split("=", 1)[1].split(",")[0]
        start, _, end = spec.partition("-")

        if start == "":
            start, end = max(0, size - int(end)), size - 1
        else:
            start, end = int(start), int(end) if end else size - 1

        end = min(end, size - 1)

        if start > end:
            self.send_error(416)
            return None

        f = open(path, "rb")
        f.seek(start)
        self.send_response(206)
        self.send_header("Content-Type", self.guess_type(path))
        self.send_header("Content-Range", f"bytes {start}-{end}/{size}")
        self.send_header("Content-Length", str(end - start + 1))
        self.send_header("Accept-Ranges", "bytes")
        self.end_headers()
        self._remaining = end - start + 1
        return f

    def copyfile(self, source, outputfile):
        remaining = getattr(self, "_remaining", None)

        if remaining is None:
            return super().copyfile(source, outputfile)

        while remaining > 0:
            chunk = source.read(min(65536, remaining))

            if not chunk:
                break

            outputfile.write(chunk)
            remaining -= len(chunk)

        self._remaining = None

    def end_headers(self):
        self.send_header("Accept-Ranges", "bytes")
        super().end_headers()


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
    print(f"Sythix web build on http://localhost:{port} (Ctrl+C stops it)")
    http.server.ThreadingHTTPServer(("", port), RangeHandler).serve_forever()
