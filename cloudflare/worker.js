// Cloudflare Worker for Sythix Client: serves the browser version (the files in build/web) and is
// its multiplayer proxy, so joining servers works without renting a separate proxy server.
//
//   browser (Sythix) --WebSocket wss://YOUR-SITE/proxy/host:port--> this Worker --TCP--> Minecraft server
//
// Same rules as web/proxy/sythix_proxy.py: only Minecraft ports, never private addresses, SRV records
// looked up like the normal game does, and only pages from this site may use it.
import { connect } from 'cloudflare:sockets';

const PORTS = { min: 25565, max: 25575 };
const CONNECT_TIMEOUT_MS = 10000;

export default {
	async fetch(request, env) {
		const url = new URL(request.url);

		if (url.pathname === '/proxy' || url.pathname.startsWith('/proxy/')) {
			return proxy(request, url, env);
		}

		return env.ASSETS.fetch(request);
	}
};

function proxy(request, url, env) {
	if ((request.headers.get('Upgrade') || '').toLowerCase() !== 'websocket') {
		return new Response('Sythix multiplayer proxy is running.\n', { headers: { 'Content-Type': 'text/plain', 'Cache-Control': 'no-store' } });
	}

	// only the game page on this same site may use the proxy
	const origin = request.headers.get('Origin');

	if (origin) {
		try {
			if (new URL(origin).host !== url.host) {
				return new Response('Forbidden', { status: 403 });
			}
		} catch (e) {
			return new Response('Forbidden', { status: 403 });
		}
	}

	const target = decodeURIComponent(url.pathname.slice('/proxy/'.length));
	const pair = new WebSocketPair();
	const [client, server] = Object.values(pair);
	server.accept();
	// the game sends its first packets as soon as the WebSocket opens, before the server
	// connection is ready, so keep them until relay() can pass them on
	const early = [];
	const keep = (event) => early.push(event.data);
	server.addEventListener('message', keep);
	relay(server, target, env.ALLOW_PRIVATE === '1', early, keep).catch((e) => refuse(server, 'Proxy error: ' + (e && e.message ? e.message : e)));
	return new Response(null, { status: 101, webSocket: client });
}

/** Tells the game why it can't join (it shows this text) and closes the connection. */
function refuse(ws, reason) {
	try {
		ws.send(reason.slice(0, 120));
		// a close reason may be at most 123 bytes
		ws.close(1008, reason.slice(0, 120));
	} catch (e) {
		// already closed
	}
}

function isIp(host) {
	return /^\d{1,3}(\.\d{1,3}){3}$/.test(host) || host.includes(':');
}

function isPrivate(host) {
	if (host === 'localhost' || host.endsWith('.localhost') || host.endsWith('.local') || host.endsWith('.internal')) {
		return true;
	}

	const v4 = host.match(/^(\d+)\.(\d+)\.(\d+)\.(\d+)$/);

	if (v4) {
		const [a, b] = [Number(v4[1]), Number(v4[2])];
		return a === 0 || a === 10 || a === 127 || (a === 100 && b >= 64 && b <= 127) || (a === 169 && b === 254) || (a === 172 && b >= 16 && b <= 31) || (a === 192 && b === 168) || a >= 224;
	}

	if (host.includes(':')) {
		const h = host.toLowerCase();
		return h === '::1' || h === '::' || h.startsWith('fc') || h.startsWith('fd') || h.startsWith('fe80') || h.startsWith('::ffff:');
	}

	return false;
}

/** The server behind _minecraft._tcp.host (like the normal game), or null. */
async function srv(host) {
	try {
		const r = await fetch('https://cloudflare-dns.com/dns-query?type=SRV&name=_minecraft._tcp.' + encodeURIComponent(host), { headers: { Accept: 'application/dns-json' } });
		const data = await r.json();
		const answers = (data.Answer || []).filter((a) => a.type === 33).map((a) => a.data.split(' '));

		if (!answers.length) {
			return null;
		}

		answers.sort((x, y) => Number(x[0]) - Number(y[0]));
		const [, , port, name] = answers[0];
		return { host: name.replace(/\.$/, ''), port: Number(port) };
	} catch (e) {
		return null;
	}
}

async function resolve(target, allowPrivate) {
	let host = target;
	let port = 25565;
	const colon = target.lastIndexOf(':');

	if (colon > 0 && !target.endsWith(']') && /^\d+$/.test(target.slice(colon + 1)) && target.indexOf(':') === colon) {
		host = target.slice(0, colon);
		port = Number(target.slice(colon + 1));
	}

	host = host.replace(/^\[|\]$/g, '').trim().toLowerCase();

	if (!host || host.length > 253) {
		throw new Error('Unknown server');
	}

	if (port === 25565 && !isIp(host)) {
		const found = await srv(host);

		if (found) {
			host = found.host;
			port = found.port;
		}
	}

	if (port < PORTS.min || port > PORTS.max) {
		throw new Error('Port ' + port + ' is not allowed by this proxy');
	}

	// ALLOW_PRIVATE=1 is only for testing on your own computer (wrangler dev --var ALLOW_PRIVATE:1)
	if (!allowPrivate && isPrivate(host)) {
		throw new Error("That address is private and can't be joined through this proxy");
	}

	return { host, port };
}

async function relay(ws, target, allowPrivate, early, keep) {
	let address;

	try {
		address = await resolve(target, allowPrivate);
	} catch (e) {
		refuse(ws, e.message);
		return;
	}

	const socket = connect({ hostname: address.host, port: address.port }, { allowHalfOpen: false });
	const timeout = new Promise((_, reject) => setTimeout(() => reject(new Error('timeout')), CONNECT_TIMEOUT_MS));

	try {
		await Promise.race([socket.opened, timeout]);
	} catch (e) {
		// Cloudflare doesn't let Workers connect to servers on its own network (Hypixel, for one)
		refuse(ws, "Can't reach " + target.replace(/:\d+$/, '') + ' (servers protected by Cloudflare need the separate Sythix proxy)');

		try {
			socket.close();
		} catch (ignored) {
			// not open
		}

		return;
	}

	const writer = socket.writable.getWriter();
	let closed = false;

	const shutdown = () => {
		if (closed) {
			return;
		}

		closed = true;

		try {
			ws.close(1000, 'Disconnected');
		} catch (e) {
			// already closed
		}

		try {
			socket.close();
		} catch (e) {
			// already closed
		}
	};

	// game -> server: binary messages are raw Minecraft protocol bytes
	const send = (data) => {
		if (typeof data !== 'string') {
			writer.write(new Uint8Array(data)).catch(shutdown);
		}
	};

	ws.removeEventListener('message', keep);
	early.forEach(send);
	early.length = 0;
	ws.addEventListener('message', (event) => send(event.data));
	ws.addEventListener('close', shutdown);
	ws.addEventListener('error', shutdown);

	// server -> game
	const reader = socket.readable.getReader();

	try {
		while (true) {
			const { value, done } = await reader.read();

			if (done) {
				break;
			}

			ws.send(value);
		}
	} catch (e) {
		// the server closed the connection
	}

	shutdown();
}
