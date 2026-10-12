// CheerpJ loads this module for System.loadLibrary("lwjgl"): it hands over LWJGL's native code,
// compiled to WebAssembly. CheerpJ loads the module from memory, so the address is worked out from
// the page (index.html sits next to the lwjgl folder).
var page = typeof document !== 'undefined' ? document.baseURI : self.location.href;
var r = await fetch(new URL('lwjgl/libraries/liblwjgl.so', page));
var buf = await r.arrayBuffer();
export default {
	wasmModule: buf
};
