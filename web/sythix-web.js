// Starts Sythix Client in the browser: CheerpJ (a Java virtual machine in WebAssembly) runs the
// same client the desktop build uses. LWJGL's native code and gl4es (OpenGL -> WebGL) are
// WebAssembly modules from the lwjgl/ folder.

const statusEl = document.getElementById('status');
const play = document.getElementById('play');
const eula = document.getElementById('eula');
const bar = document.getElementById('bar');

// CheerpJ sees this web server under /app/; the build can live in any folder of it
const base = '/app' + location.pathname.replace(/[^/]*$/, '');

function status(text) {
	statusEl.classList.remove('error');
	statusEl.textContent = text;
}

/** Shows a problem with what to do about it (and keeps it in the console for bug reports). */
function problem(title, details) {
	console.error('[Sythix] ' + title + (details ? ': ' + details : ''));
	statusEl.classList.add('error');
	statusEl.textContent = '';
	const b = document.createElement('b');
	b.textContent = title;
	statusEl.appendChild(b);

	if (details) {
		const d = document.createElement('div');
		d.textContent = details;
		statusEl.appendChild(d);
	}
}

// ---- Checks before starting (clear messages instead of a stuck loading screen) ----------------
function browserProblems() {
	const problems = [];

	if (typeof WebAssembly !== 'object') {
		problems.push('This browser has no WebAssembly. Use a current Chrome, Edge, Firefox or Safari.');
	}

	const gl = document.createElement('canvas').getContext('webgl');

	if (!gl) {
		problems.push('WebGL is not available (graphics acceleration may be off in the browser settings, or the graphics driver is blocked).');
	}

	if (!window.indexedDB) {
		problems.push('This browser blocks site storage (IndexedDB), which the game needs for worlds and settings. Private windows often do this.');
	}

	if (typeof cheerpjInit !== 'function') {
		problems.push('The Java engine (CheerpJ, from cjrtnc.leaningtech.com) did not load. Check the internet connection, or an ad/script blocker.');
	}

	return problems;
}

/** Waits until sw.js controls the page (it answers CheerpJ's piece requests on hosts that can't). */
async function serviceWorkerReady() {
	if (!('serviceWorker' in navigator) || location.protocol === 'file:') {
		return false;
	}

	if (navigator.serviceWorker.controller) {
		return true;
	}

	await Promise.race([
		new Promise((resolve) => navigator.serviceWorker.addEventListener('controllerchange', resolve, { once: true })),
		new Promise((resolve) => setTimeout(resolve, 15000))
	]);
	return !!navigator.serviceWorker.controller;
}

/** Every file the game needs, checked before Java starts. */
async function missingFiles(files) {
	const needed = files.classpath.concat(['lwjgl/libraries/lwjgl.js', 'lwjgl/libraries/liblwjgl.so', 'lwjgl/libraries/gl4es.wasm']);
	const results = await Promise.all(needed.map((f) => fetch(f, { method: 'HEAD', cache: 'no-cache' }).then((r) => (r.ok ? null : f + ' (HTTP ' + r.status + ')')).catch(() => f + ' (network error)')));
	return results.filter((r) => r);
}

/** CheerpJ needs files in pieces (HTTP Range); the server or sw.js must provide that. */
async function rangeWorks() {
	try {
		const r = await fetch('sythix-client.jar', { headers: { Range: 'bytes=0-1' } });
		const ok = r.status === 206;
		await r.arrayBuffer();
		return ok;
	} catch (e) {
		return false;
	}
}

try {
	eula.checked = localStorage.getItem('sythix-eula') === 'yes';
} catch (e) {
	// storage blocked: the box just starts unticked
}

play.disabled = !eula.checked;
eula.addEventListener('change', () => {
	play.disabled = !eula.checked;

	try {
		localStorage.setItem('sythix-eula', eula.checked ? 'yes' : 'no');
	} catch (e) {
		// ignore
	}
});

play.addEventListener('click', start);

// ---- Options: multiplayer proxy, a server to join, resolution --------------------------------
// The proxy comes from ?proxy=, else what the player typed last time, else config.json (set by
// whoever hosts the site: {"proxy": "wss://proxy.example.com"}).
const params = new URLSearchParams(location.search);
const proxyInput = document.getElementById('proxy');
const serverInput = document.getElementById('server');
const scaleInput = document.getElementById('scale');

function stored(key) {
	try {
		return localStorage.getItem(key) || '';
	} catch (e) {
		return '';
	}
}

function store(key, value) {
	try {
		localStorage.setItem(key, value);
	} catch (e) {
		// storage blocked
	}
}

proxyInput.value = params.get('proxy') || stored('sythix-proxy');
serverInput.value = params.get('server') || '';
scaleInput.value = params.get('scale') || stored('sythix-scale') || '1';

if (!proxyInput.value) {
	fetch('config.json').then((r) => (r.ok ? r.json() : {})).then((c) => {
		if (!proxyInput.value && c.proxy) {
			proxyInput.value = c.proxy;
		}
	}).catch(() => {});
}

if (params.get('server')) {
	document.getElementById('options').open = true;
}

// ---- WebSockets for the game (Java native methods, see WebSocketBridge.java) ---------------
const sockets = new Map();
let nextSocket = 1;

function openSocket(url) {
	const id = nextSocket++;
	const s = { queue: [], offset: 0, pending: [], state: 0, reason: '' };
	sockets.set(id, s);

	try {
		const ws = new WebSocket(url);
		ws.binaryType = 'arraybuffer';
		s.ws = ws;
		ws.onopen = () => {
			s.state = 1;

			for (const data of s.pending) {
				ws.send(data);
			}

			s.pending = [];
		};
		ws.onmessage = (e) => {
			if (typeof e.data === 'string') {
				s.reason = e.data;
			} else {
				s.queue.push(new Int8Array(e.data));
			}
		};
		ws.onerror = () => {
			s.reason = s.reason || "Can't reach the multiplayer proxy (" + url.split('/').slice(0, 3).join('/') + ')';
		};
		ws.onclose = (e) => {
			s.state = 2;
			s.reason = s.reason || e.reason || 'Connection closed';
		};
	} catch (e) {
		s.state = 2;
		s.reason = 'Bad proxy address: ' + e.message;
	}

	return id;
}

// ---- Keys the browser layer drops (see WebInput.java) -----------------------------------------
// DOM key code -> [LWJGL key code, character]
const KEYS = {
	Enter: [28, 13], NumpadEnter: [156, 13], Backspace: [14, 8], Tab: [15, 9], Delete: [211, 127], Insert: [210, 0],
	ArrowUp: [200, 0], ArrowDown: [208, 0], ArrowLeft: [203, 0], ArrowRight: [205, 0],
	Home: [199, 0], End: [207, 0], PageUp: [201, 0], PageDown: [209, 0],
	ShiftLeft: [42, 0], ShiftRight: [54, 0], ControlLeft: [29, 0], ControlRight: [157, 0], AltLeft: [56, 0], AltRight: [184, 0],
	MetaLeft: [219, 0], MetaRight: [220, 0], CapsLock: [58, 0],
	F1: [59, 0], F2: [60, 0], F3: [61, 0], F4: [62, 0], F5: [63, 0], F6: [64, 0], F7: [65, 0], F8: [66, 0], F9: [67, 0], F10: [68, 0], F11: [87, 0], F12: [88, 0]
};
const keyQueue = [];
let gameRunning = false;

function onKey(e, down) {
	const k = KEYS[e.code];

	if (!gameRunning || !k) {
		return;
	}

	keyQueue.push(k[0], down ? k[1] : 0, down ? 1 : 0, e.repeat ? 1 : 0);

	// keep the browser from acting on them (Tab moving focus, F5 reloading, Backspace going back...)
	if (e.code !== 'F11' && e.code !== 'F12') {
		e.preventDefault();
	}
}

document.addEventListener('keydown', (e) => onKey(e, true), true);
document.addEventListener('keyup', (e) => onKey(e, false), true);

const natives = {
	async Java_net_sythix_client_web_WebInput_poll(lib, out) {
		const n = Math.min(keyQueue.length, out.length);
		out.set(keyQueue.splice(0, n - (n % 4)));
		return n >> 2;
	},
	async Java_net_sythix_client_web_WebSocketBridge_open(lib, url) {
		return openSocket(url);
	},
	async Java_net_sythix_client_web_WebSocketBridge_state(lib, id) {
		const s = sockets.get(id);
		return s ? s.state : 2;
	},
	async Java_net_sythix_client_web_WebSocketBridge_read(lib, id, buf) {
		const s = sockets.get(id);

		if (!s) {
			return -1;
		}

		let n = 0;

		while (s.queue.length && n < buf.length) {
			const chunk = s.queue[0];
			const take = Math.min(chunk.length - s.offset, buf.length - n);
			buf.set(chunk.subarray(s.offset, s.offset + take), n);
			n += take;
			s.offset += take;

			if (s.offset >= chunk.length) {
				s.queue.shift();
				s.offset = 0;
			}
		}

		return n === 0 && s.state === 2 ? -1 : n;
	},
	async Java_net_sythix_client_web_WebSocketBridge_send(lib, id, data, len) {
		const s = sockets.get(id);

		if (!s || s.state === 2) {
			return;
		}

		// copy: the Java array is shared and may be reused
		const copy = data.slice(0, len).buffer;

		if (s.state === 1) {
			s.ws.send(copy);
		} else {
			s.pending.push(copy);
		}
	},
	async Java_net_sythix_client_web_WebSocketBridge_close(lib, id) {
		const s = sockets.get(id);

		if (s && s.ws && s.state !== 2) {
			s.ws.close();
		}
	},
	async Java_net_sythix_client_web_WebSocketBridge_closeReason(lib, id) {
		const s = sockets.get(id);
		return s ? s.reason : '';
	},
	async Java_net_sythix_client_web_WebSocketBridge_proxyUrl(lib) {
		return proxyInput.value.trim();
	}
};

// ---- Speed: saved game files and a preloaded Java runtime ------------------------------------
// sw.js keeps the game files after the first visit. It is registered as soon as the page opens
// (with this build's version), so even the first game start fills the cache.
const filesPromise = fetch('files.json').then((r) => r.json());

if ('serviceWorker' in navigator && location.protocol !== 'file:') {
	filesPromise.then((files) => navigator.serviceWorker.register('sw.js?v=' + (files.version || 'dev'))).catch(() => {});
}

// preload.json: the Java runtime pieces the game needs, fetched in parallel instead of one by one
// (made with cjGetRuntimeResources() after a full start; see README)
const preloadPromise = fetch('preload.json').then((r) => (r.ok ? r.json() : null)).catch(() => null);

// ---- Resolution: mouse positions ----------------------------------------------------------------
// With a lower resolution the game picture is stretched to the window, but CheerpJ reads mouse
// positions as if it were not. Real mouse events over the game are caught first and sent again with
// positions converted to the game's size. (Mouse look in game uses movement, which needs no change.)
let mouseScale = 1;
const MOUSE_EVENTS = ['mousedown', 'mouseup', 'mousemove', 'click', 'dblclick', 'contextmenu', 'wheel', 'pointerdown', 'pointerup', 'pointermove'];

function rescale(e) {
	const display = document.getElementById('display');

	if (mouseScale === 1 || !e.isTrusted || document.pointerLockElement || !display.contains(e.target)) {
		return;
	}

	e.stopImmediatePropagation();
	const rect = display.getBoundingClientRect();
	const x = rect.left + (e.clientX - rect.left) * mouseScale;
	const y = rect.top + (e.clientY - rect.top) * mouseScale;
	const init = {
		bubbles: e.bubbles, cancelable: e.cancelable, composed: true, view: window, detail: e.detail,
		clientX: x, clientY: y, screenX: x, screenY: y, movementX: e.movementX, movementY: e.movementY,
		button: e.button, buttons: e.buttons, ctrlKey: e.ctrlKey, shiftKey: e.shiftKey, altKey: e.altKey, metaKey: e.metaKey
	};
	let copy;

	if (e.type === 'wheel') {
		copy = new WheelEvent(e.type, { ...init, deltaX: e.deltaX, deltaY: e.deltaY, deltaZ: e.deltaZ, deltaMode: e.deltaMode });
	} else if (e.type.startsWith('pointer')) {
		copy = new PointerEvent(e.type, { ...init, pointerId: e.pointerId, pointerType: e.pointerType, isPrimary: e.isPrimary, pressure: e.pressure });
	} else {
		copy = new MouseEvent(e.type, init);
	}

	if (!e.target.dispatchEvent(copy)) {
		e.preventDefault();
	}
}

for (const type of MOUSE_EVENTS) {
	window.addEventListener(type, rescale, { capture: true, passive: false });
}

async function start() {
	play.disabled = true;
	bar.style.display = 'block';

	try {
		store('sythix-proxy', proxyInput.value.trim());
		store('sythix-scale', scaleInput.value);

		if (document.getElementById('fullscreen').checked && document.documentElement.requestFullscreen) {
			// must happen right after the click; the game fills whatever size the window has
			await document.documentElement.requestFullscreen().catch(() => {});
		}

		status('Checking the browser...');
		const browser = browserProblems();

		if (browser.length) {
			problem("Sythix can't run in this browser", browser.join(' '));
			bar.style.display = 'none';
			play.disabled = false;
			return;
		}

		let files;

		try {
			files = await filesPromise;
		} catch (e) {
			problem('The game files list (files.json) is missing', 'The site was not built or uploaded completely. Build it with cloudflare/build.sh or CompileWebRuntime.sh and publish all of build/web.');
			bar.style.display = 'none';
			play.disabled = false;
			return;
		}

		status('Checking the game files...');
		const swReady = await serviceWorkerReady();
		const missing = await missingFiles(files);

		if (missing.length) {
			problem('Some game files are missing from the website', missing.join(', ') + '. Publish the whole build/web folder.');
			bar.style.display = 'none';
			play.disabled = false;
			return;
		}

		if (!(await rangeWorks())) {
			problem("The game files can't be read in pieces", swReady
				? 'The web server does not support HTTP Range requests and the service worker could not help. Try reloading the page.'
				: 'This web server does not support HTTP Range requests, and this browser blocked the service worker that works around it (private windows often do). Use a normal window, or host the site where Range requests work.');
			bar.style.display = 'none';
			play.disabled = false;
			return;
		}

		status('Loading Java (CheerpJ)...');
		const preload = await preloadPromise;
		await cheerpjInit({
			...(preload ? { preloadResources: preload, preloadProgress: (done, total) => status('Loading Java (CheerpJ)... ' + Math.round(100 * done / total) + '%') } : {}),
			version: 8,
			javaProperties: ['java.library.path=' + base + 'lwjgl/libraries/', 'sythix.web=true', 'sythix.webdebug=' + (location.hash === '#debug'), 'sythix.join=' + serverInput.value.trim()],
			libraries: { 'libGL.so.1': base + 'lwjgl/libraries/gl4es.wasm' },
			enableX11: true,
			// Minecraft 1.12 cannot use input methods, and CheerpJ's would swallow the Enter key
			enableInputMethods: false,
			natives,
			status: 'none'
		});

		status('Starting Sythix Client...');
		const display = document.getElementById('display');
		display.style.display = 'block';
		// Resolution: the game draws fewer pixels and the browser stretches the picture to the window
		const scale = Math.min(1, Math.max(0.25, parseFloat(scaleInput.value) || 1));
		const width = Math.round(window.innerWidth * scale);
		const height = Math.round(window.innerHeight * scale);

		mouseScale = scale;

		if (scale < 1) {
			display.style.width = width + 'px';
			display.style.height = height + 'px';
			display.style.transformOrigin = '0 0';
			display.style.transform = 'scale(' + (1 / scale) + ')';
			display.style.imageRendering = 'auto';
		}

		await cheerpjCreateDisplay(scale < 1 ? width : -1, scale < 1 ? height : -1, display);
		document.getElementById('start').style.display = 'none';
		gameRunning = true;

		const classpath = files.classpath.map((f) => base + f).join(':');
		const exit = await cheerpjRunMain('Start', classpath, '--gameDir', '/files/sythix', '--width', String(width), '--height', String(height));
		gameRunning = false;
		document.getElementById('start').style.display = 'flex';
		display.style.display = 'none';
		bar.style.display = 'none';
		if (exit === 0) {
			status('The game closed. Press Play to start again.');
		} else {
			problem('The game stopped with an error (exit code ' + exit + ')', 'Details are in the browser console (F12). Press Play to start again.');
		}

		play.disabled = false;
	} catch (e) {
		console.error(e);
		bar.style.display = 'none';
		problem('The game could not start', String(e && e.message ? e.message : e) + ' (details in the browser console, F12)');
		play.disabled = false;
	}
}
