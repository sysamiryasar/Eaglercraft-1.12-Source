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
	statusEl.textContent = text;
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

async function start() {
	play.disabled = true;
	bar.style.display = 'block';

	try {
		status('Loading Java (CheerpJ)...');
		const files = await (await fetch('files.json')).json();
		await cheerpjInit({
			version: 8,
			javaProperties: ['java.library.path=' + base + 'lwjgl/libraries/', 'sythix.web=true', 'sythix.webdebug=' + (location.hash === '#debug')],
			libraries: { 'libGL.so.1': base + 'lwjgl/libraries/gl4es.wasm' },
			enableX11: true,
			status: 'none'
		});

		status('Starting Sythix Client...');
		const display = document.getElementById('display');
		display.style.display = 'block';
		await cheerpjCreateDisplay(-1, -1, display);
		document.getElementById('start').style.display = 'none';

		const classpath = files.classpath.map((f) => base + f).join(':');
		const exit = await cheerpjRunMain('Start', classpath, '--gameDir', '/files/sythix', '--width', String(window.innerWidth), '--height', String(window.innerHeight));
		document.getElementById('start').style.display = 'flex';
		display.style.display = 'none';
		bar.style.display = 'none';
		status('The game closed (exit code ' + exit + '). Press Play to start again.');
		play.disabled = false;
	} catch (e) {
		console.error(e);
		bar.style.display = 'none';
		status('Could not start: ' + e);
		play.disabled = false;
	}
}
