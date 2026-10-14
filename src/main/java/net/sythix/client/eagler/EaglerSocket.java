package net.sythix.client.eagler;

/**
 * A WebSocket to an Eaglercraft server that keeps message boundaries (every game packet is one
 * binary message) and text messages (the server list query). The browser build uses the page's
 * WebSocket, the desktop build a Netty one.
 */
public interface EaglerSocket
{
    /** A received message: binary game/handshake data, or text. */
    final class Frame
    {
        public final boolean text;
        public final byte[] data;

        public Frame(boolean text, byte[] data)
        {
            this.text = text;
            this.data = data;
        }

        public String string()
        {
            return new String(this.data, java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /** Sends one binary message (queued until the socket is open). */
    void send(byte[] data);

    /** Sends one text message (queued until the socket is open). */
    void sendText(String text);

    /** The next received message, or null when there is none yet. */
    Frame poll();

    /** True once the socket closed (received messages may still be waiting in poll()). */
    boolean isClosed();

    /** Why the socket closed, or "". */
    String closeReason();

    void close();
}
