// Service worker for the browser version of Sythix Client.
//
// 1. CheerpJ reads jars in pieces (HTTP Range requests) and stops if the web server cannot do that.
//    Some free static hosts (Cloudflare's static assets among them) answer every request with the
//    whole file, so this worker downloads each game file once, keeps it, and answers the piece
//    requests itself. This works on any static host.
// 2. Kept files make later starts load from disk instead of downloading ~60 MB again. A new build
//    (new version in the URL this worker is registered with) starts a fresh cache.
const VERSION = new URL(self.location).searchParams.get('v') || 'dev';
const CACHE = 'sythix-' + VERSION;
const GAME_FILE = /\.(jar|zip|wasm|so|js|png|json)$/;
const memory = new Map(); // path -> Promise<{ buffer, type }> for this worker's lifetime

self.addEventListener('install', () => self.skipWaiting());

self.addEventListener('activate', (event) => {
	event.waitUntil((async () => {
		for (const key of await caches.keys()) {
			if (key.startsWith('sythix-') && key !== CACHE) {
				await caches.delete(key);
			}
		}

		await self.clients.claim();
	})());
});

/** The whole file, from memory, the cache or (once) the network. Parallel requests share one download. */
function whole(key) {
	if (!memory.has(key)) {
		const promise = (async () => {
			const cache = await caches.open(CACHE);
			let response = await cache.match(key);

			if (!response) {
				response = await fetch(key, { cache: 'no-cache' });

				if (!response.ok) {
					throw new Error('HTTP ' + response.status + ' for ' + key);
				}

				const copy = response.clone();
				await cache.put(key, copy).catch(() => {});
			}

			return { buffer: await response.arrayBuffer(), type: response.headers.get('Content-Type') || 'application/octet-stream' };
		})();
		promise.catch(() => memory.delete(key));
		memory.set(key, promise);
	}

	return memory.get(key);
}

function piece(file, range) {
	const size = file.buffer.byteLength;
	const m = /bytes=(\d*)-(\d*)/.exec(range || '');
	let start = 0;
	let end = size - 1;

	if (m && m[1] !== '') {
		start = parseInt(m[1], 10);
		end = m[2] !== '' ? Math.min(parseInt(m[2], 10), size - 1) : size - 1;
	} else if (m && m[2] !== '') {
		start = Math.max(0, size - parseInt(m[2], 10));
	}

	if (start >= size) {
		return new Response(null, { status: 416, headers: { 'Content-Range': 'bytes */' + size } });
	}

	return new Response(file.buffer.slice(start, end + 1), {
		status: 206,
		headers: { 'Content-Type': file.type, 'Content-Range': 'bytes ' + start + '-' + end + '/' + size, 'Content-Length': String(end - start + 1), 'Accept-Ranges': 'bytes' }
	});
}

self.addEventListener('fetch', (event) => {
	const request = event.request;
	const url = new URL(request.url);

	// only this site's game files; never the page, its loader or files.json (so updates show)
	if (request.method !== 'GET' || url.origin !== self.location.origin || !GAME_FILE.test(url.pathname) || /\/(sw\.js|sythix-web\.js|files\.json|config\.json)$/.test(url.pathname)) {
		return;
	}

	const key = url.origin + url.pathname;
	const range = request.headers.get('Range');
	event.respondWith(whole(key).then(
		(file) => (range ? piece(file, range) : new Response(file.buffer, { status: 200, headers: { 'Content-Type': file.type, 'Content-Length': String(file.buffer.byteLength), 'Accept-Ranges': 'bytes' } })),
		() => fetch(request)
	));
});
