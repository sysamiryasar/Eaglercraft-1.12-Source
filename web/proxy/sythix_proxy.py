#!/usr/bin/env python3
"""
Sythix multiplayer proxy: lets the browser version of Sythix Client join Minecraft servers.

Browsers cannot open game (TCP) connections, only WebSockets. The game opens a WebSocket to
    wss://YOUR-PROXY/<server address>
and this proxy connects to that Minecraft server and passes the bytes both ways. Only Python 3.8+
is needed (no packages).

  python3 sythix_proxy.py                       # ws:// on port 8765 (put HTTPS in front, see README)
  python3 sythix_proxy.py --cert c.pem --key k.pem --port 443   # wss:// by itself

Safety: it only connects to Minecraft ports (25565-25575 unless you change --ports), never to
private or internal addresses (so nobody can use it to reach your own network), limits connections
per visitor, and can be limited to certain servers (--allow) or websites (--origin).
"""
import argparse
import asyncio
import base64
import hashlib
import ipaddress
import logging
import os
import random
import socket
import ssl
import struct
import time
from urllib.parse import unquote

GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
log = logging.getLogger("sythix-proxy")


def parse_args():
    p = argparse.ArgumentParser(description="WebSocket to Minecraft proxy for the Sythix browser client")
    p.add_argument("--host", default="0.0.0.0", help="address to listen on (default 0.0.0.0)")
    p.add_argument("--port", type=int, default=int(os.environ.get("PORT", "8765")), help="port to listen on (default 8765 or $PORT)")
    p.add_argument("--cert", help="TLS certificate (PEM) for wss:// without a reverse proxy")
    p.add_argument("--key", help="TLS private key (PEM)")
    p.add_argument("--ports", default="25565-25575", help="Minecraft ports that may be joined, e.g. 25565-25575,19132")
    p.add_argument("--allow", default="", help="only these servers, comma separated (wildcards like *.example.com work); default: any public server")
    p.add_argument("--origin", default="", help="only pages from these sites may use the proxy, comma separated (e.g. https://you.github.io)")
    p.add_argument("--per-ip", type=int, default=4, help="connections per visitor (default 4)")
    p.add_argument("--max", type=int, default=300, help="connections in total (default 300)")
    p.add_argument("--allow-private", action="store_true", help="also allow private/local addresses (only for testing on your own computer)")
    p.add_argument("--behind-proxy", action="store_true", help="trust X-Forwarded-For (when Caddy/nginx/Cloudflare is in front)")
    return p.parse_args()


def port_set(spec):
    ports = set()
    for part in spec.split(","):
        part = part.strip()
        if "-" in part:
            a, b = part.split("-", 1)
            ports.update(range(int(a), int(b) + 1))
        elif part:
            ports.add(int(part))
    return ports


def matches(host, patterns):
    host = host.lower().rstrip(".")
    for p in patterns:
        if p.startswith("*.") and (host.endswith(p[1:]) or host == p[2:]):
            return True
        if host == p:
            return True
    return False


# ---- DNS SRV (_minecraft._tcp), so "play.example.com" works like in the normal game ------------

def nameserver():
    try:
        with open("/etc/resolv.conf") as f:
            for line in f:
                parts = line.split()
                if len(parts) >= 2 and parts[0] == "nameserver":
                    return parts[1]
    except OSError:
        pass
    return "1.1.1.1"


def read_name(data, pos):
    labels = []
    jumped = False
    end = pos
    for _ in range(64):
        length = data[pos]
        if length & 0xC0 == 0xC0:
            if not jumped:
                end = pos + 2
            pos = ((length & 0x3F) << 8) | data[pos + 1]
            jumped = True
            continue
        if length == 0:
            if not jumped:
                end = pos + 1
            break
        labels.append(data[pos + 1:pos + 1 + length].decode("ascii", "replace"))
        pos += 1 + length
    return ".".join(labels), end


def srv_lookup(host):
    """(target, port) from _minecraft._tcp.host, or None."""
    qid = random.randint(0, 65535)
    name = b"".join(bytes([len(l)]) + l.encode("idna") for l in ("_minecraft._tcp." + host).split(".") if l) + b"\0"
    packet = struct.pack(">HHHHHH", qid, 0x0100, 1, 0, 0, 0) + name + struct.pack(">HH", 33, 1)
    s = socket.socket(socket.AF_INET6 if ":" in nameserver() else socket.AF_INET, socket.SOCK_DGRAM)
    s.settimeout(2.0)
    try:
        s.sendto(packet, (nameserver(), 53))
        data = s.recv(4096)
    except OSError:
        return None
    finally:
        s.close()
    return parse_srv(data, qid)


def parse_srv(data, qid):
    """The best (target, port) in a DNS answer, or None."""
    rid, flags, qd, an = struct.unpack(">HHHH", data[:8])
    if rid != qid or flags & 0x000F or an == 0:
        return None
    pos = 12
    for _ in range(qd):
        _, pos = read_name(data, pos)
        pos += 4
    best = None
    for _ in range(an):
        _, pos = read_name(data, pos)
        rtype, _, _, rdlen = struct.unpack(">HHIH", data[pos:pos + 10])
        pos += 10
        if rtype == 33:
            prio, weight, port = struct.unpack(">HHH", data[pos:pos + 6])
            target, _ = read_name(data, pos + 6)
            if best is None or prio < best[0]:
                best = (prio, target, port)
        pos += rdlen
    return (best[1], best[2]) if best else None


# ---- WebSocket framing ------------------------------------------------------------------------

def frame(opcode, payload=b""):
    n = len(payload)
    if n < 126:
        head = struct.pack(">BB", 0x80 | opcode, n)
    elif n < 65536:
        head = struct.pack(">BBH", 0x80 | opcode, 126, n)
    else:
        head = struct.pack(">BBQ", 0x80 | opcode, 127, n)
    return head + payload


async def read_frame(reader):
    b1, b2 = await reader.readexactly(2)
    fin, opcode, masked, n = b1 & 0x80, b1 & 0x0F, b2 & 0x80, b2 & 0x7F
    if n == 126:
        n = struct.unpack(">H", await reader.readexactly(2))[0]
    elif n == 127:
        n = struct.unpack(">Q", await reader.readexactly(8))[0]
    if n > 4 * 1024 * 1024:
        raise ValueError("frame too big")
    mask = await reader.readexactly(4) if masked else None
    data = await reader.readexactly(n)
    if mask:
        data = xor_mask(data, mask)
    return fin, opcode, data


def xor_mask(data, mask):
    # XOR the whole payload with the repeated 4-byte mask in one go
    n = len(data)
    whole = int.from_bytes((mask * (n // 4 + 1))[:n], "big")
    return (int.from_bytes(data, "big") ^ whole).to_bytes(n, "big")


class Proxy:
    def __init__(self, args):
        self.args = args
        self.ports = port_set(args.ports)
        self.allow = [h.strip().lower() for h in args.allow.split(",") if h.strip()]
        self.origins = [o.strip().rstrip("/") for o in args.origin.split(",") if o.strip()]
        self.per_ip = {}
        self.total = 0

    async def handle(self, reader, writer):
        peer = writer.get_extra_info("peername")
        ip = peer[0] if peer else "?"
        try:
            request = await asyncio.wait_for(reader.readuntil(b"\r\n\r\n"), 10)
        except Exception:
            writer.close()
            return
        lines = request.decode("latin-1").split("\r\n")
        try:
            method, path, _ = lines[0].split(" ", 2)
        except ValueError:
            writer.close()
            return
        headers = {}
        for line in lines[1:]:
            if ":" in line:
                k, v = line.split(":", 1)
                headers[k.strip().lower()] = v.strip()
        if self.args.behind_proxy and "x-forwarded-for" in headers:
            ip = headers["x-forwarded-for"].split(",")[0].strip()

        if headers.get("upgrade", "").lower() != "websocket" or "sec-websocket-key" not in headers:
            body = b"Sythix multiplayer proxy is running.\n"
            writer.write(b"HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\nAccess-Control-Allow-Origin: *\r\n\r\n" % len(body) + body)
            await writer.drain()
            writer.close()
            return

        origin = headers.get("origin", "").rstrip("/")
        if self.origins and origin not in self.origins:
            writer.write(b"HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\n\r\n")
            writer.close()
            log.info("%s refused: origin %s", ip, origin)
            return

        accept = base64.b64encode(hashlib.sha1((headers["sec-websocket-key"] + GUID).encode()).digest()).decode()
        writer.write(("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: %s\r\n\r\n" % accept).encode())
        await writer.drain()

        if self.per_ip.get(ip, 0) >= self.args.per_ip or self.total >= self.args.max:
            await self.refuse(writer, "Too many connections from you right now, try again in a moment")
            return

        self.per_ip[ip] = self.per_ip.get(ip, 0) + 1
        self.total += 1
        try:
            await self.relay(reader, writer, unquote(path.lstrip("/")), ip)
        finally:
            self.per_ip[ip] -= 1
            if self.per_ip[ip] <= 0:
                del self.per_ip[ip]
            self.total -= 1

    async def refuse(self, writer, reason):
        try:
            writer.write(frame(0x1, reason.encode()[:120]))
            writer.write(frame(0x8, struct.pack(">H", 1008) + reason.encode()[:120]))
            await writer.drain()
        except Exception:
            pass
        writer.close()

    async def resolve(self, target):
        """Checks the address and returns (ip, port, name) or raises ValueError with a message for the player."""
        host, _, port = target.rpartition(":")
        if not host:
            host, port = target, "25565"
        host = host.strip("[]").strip().lower()
        try:
            port = int(port)
        except ValueError:
            raise ValueError("Bad server address")
        if not host or len(host) > 253:
            raise ValueError("Bad server address")
        if self.allow and not matches(host, self.allow):
            raise ValueError("This proxy only allows certain servers")
        name = host
        loop = asyncio.get_running_loop()
        if port == 25565 and not self.is_ip(host):
            srv = await loop.run_in_executor(None, srv_lookup, host)
            if srv:
                name, port = srv[0].rstrip("."), srv[1]
        if port not in self.ports:
            raise ValueError("Port %d is not allowed by this proxy" % port)
        try:
            infos = await loop.getaddrinfo(name, port, type=socket.SOCK_STREAM)
        except socket.gaierror:
            raise ValueError("Unknown server: %s" % host)
        for info in infos:
            addr = ipaddress.ip_address(info[4][0])
            if self.args.allow_private or addr.is_global:
                return str(addr), port, name
        raise ValueError("That address is private and can't be joined through this proxy")

    @staticmethod
    def is_ip(host):
        try:
            ipaddress.ip_address(host)
            return True
        except ValueError:
            return False

    async def relay(self, ws_reader, ws_writer, target, ip):
        try:
            addr, port, name = await self.resolve(target)
            mc_reader, mc_writer = await asyncio.wait_for(asyncio.open_connection(addr, port), 10)
        except ValueError as e:
            await self.refuse(ws_writer, str(e))
            log.info("%s -> %s refused: %s", ip, target, e)
            return
        except Exception:
            await self.refuse(ws_writer, "Can't reach %s" % target.rsplit(":", 1)[0])
            log.info("%s -> %s unreachable", ip, target)
            return
        sock = mc_writer.get_extra_info("socket")
        if sock is not None:
            sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        log.info("%s -> %s (%s:%d)", ip, target, addr, port)
        started = time.time()

        async def browser_to_server():
            message = b""
            while True:
                fin, opcode, data = await read_frame(ws_reader)
                if opcode == 0x8:
                    return
                if opcode == 0x9:
                    ws_writer.write(frame(0xA, data))
                    continue
                if opcode in (0x0, 0x2):
                    message += data
                    if fin:
                        mc_writer.write(message)
                        await mc_writer.drain()
                        message = b""

        async def server_to_browser():
            while True:
                data = await mc_reader.read(65536)
                if not data:
                    return
                ws_writer.write(frame(0x2, data))
                await ws_writer.drain()

        tasks = [asyncio.ensure_future(browser_to_server()), asyncio.ensure_future(server_to_browser())]
        try:
            await asyncio.wait(tasks, return_when=asyncio.FIRST_COMPLETED)
        finally:
            for t in tasks:
                t.cancel()
            for w in (mc_writer, ws_writer):
                try:
                    w.close()
                except Exception:
                    pass
            log.info("%s -> %s closed after %ds", ip, target, time.time() - started)


async def main():
    args = parse_args()
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(message)s")
    context = None
    if args.cert:
        context = ssl.create_default_context(ssl.Purpose.CLIENT_AUTH)
        context.load_cert_chain(args.cert, args.key)
    proxy = Proxy(args)
    server = await asyncio.start_server(proxy.handle, args.host, args.port, ssl=context)
    log.info("Sythix proxy on %s://%s:%d (ports %s%s)", "wss" if context else "ws", args.host, args.port, args.ports, ", allow " + args.allow if args.allow else "")
    async with server:
        await server.serve_forever()


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        pass
