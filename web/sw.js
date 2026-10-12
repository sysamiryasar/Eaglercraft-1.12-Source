// Keeps the game files (jars, WebAssembly, textures) in the browser after the first visit, so the
// next start loads them from disk instead of the network. CheerpJ reads jars in pieces (HTTP Range
// requests); those are answered from the saved copy too. A new build (new version in the URL this
// worker is registered with) starts a fresh cache and deletes the old one.
const VERSION = new URL(self.location).searchParams.get('v') || 'dev';
const CACHE = 'sythix-' + VERSION;
const SAVE = /\.(jar|wasm|so|js|png|json)$/;

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

function slice(response, range) {
	return response.arrayBuffer().then((buffer) => {
		const size = buffer.byteLength;
		const m = /bytes=(\d*)-(\d*)/.exec(range);
		let start = m && m[1] !== '' ? parseInt(m[1], 10) : 0;
		let end = m && m[2] !== '' ? parseInt(m[2], 10) : size - 1;

		if (m && m[1] === '' && m[2] !== '') {
			start = Math.max(0, size - parseInt(m[2], 10));
			end = size - 1;
		}

		end = Math.min(end, size - 1);
		return new Response(buffer.slice(start, end + 1), {
			status: 206,
			headers: {
				'Content-Type': response.headers.get('Content-Type') || 'application/octet-stream',
				'Content-Range': 'bytes ' + start + '-' + end + '/' + size,
				'Content-Length': String(end - start + 1),
				'Accept-Ranges': 'bytes'
			}
		});
	});
}

self.addEventListener('fetch', (event) => {
	const request = event.request;
	const url = new URL(request.url);

	// only this site's game files; never the page itself (so updates show) or other sites
	if (request.method !== 'GET' || url.origin !== self.location.origin || !SAVE.test(url.pathname) || url.pathname.endsWith('/sw.js') || url.pathname.endsWith('files.json') || url.pathname.endsWith('sythix-web.js')) {
		return;
	}

	event.respondWith((async () => {
		const cache = await caches.open(CACHE);
		const key = url.origin + url.pathname;
		const saved = await cache.match(key);
		const range = request.headers.get('Range');

		if (saved) {
			return range ? slice(saved, range) : saved;
		}

		// not saved yet: answer from the network now, and save the whole file in the background
		event.waitUntil(fetch(key).then((full) => (full.ok && full.status === 200 ? cache.put(key, full) : null)).catch(() => {}));
		return fetch(request);
	})());
});
