package net.sythix.client.web;

/**
 * Browser WebSockets for the browser build. The methods are implemented in JavaScript
 * (web/sythix-web.js, passed to CheerpJ as natives); they exist only in the browser.
 */
public final class WebSocketBridge
{
    public static final int CONNECTING = 0;
    public static final int OPEN = 1;
    public static final int CLOSED = 2;

    private WebSocketBridge()
    {
    }

    /** Opens a binary WebSocket and returns its id. */
    public static native int open(String url);

    /** CONNECTING, OPEN or CLOSED. */
    public static native int state(int id);

    /** Copies received bytes into buf; returns how many (0 = nothing yet, -1 = closed and empty). */
    public static native int read(int id, byte[] buf);

    /** Sends the first len bytes of data (queued until the socket is open). */
    public static native void send(int id, byte[] data, int len);

    public static native void close(int id);

    /** Why the socket closed, or "". */
    public static native String closeReason(int id);

    /** The multiplayer proxy set on the start page (for example wss://proxy.example.com), or "". */
    public static native String proxyUrl();
}
