package net.sythix.client.eagler;

import net.sythix.client.web.WebSocketBridge;

/** The browser's own WebSocket (see web/sythix-web.js): no proxy needed for Eaglercraft servers. */
final class WebEaglerSocket implements EaglerSocket
{
    private final int id;
    private final byte[] buffer = new byte[65536];
    private byte[] pending = new byte[0];
    private int offset;
    private boolean closed;

    WebEaglerSocket(String url)
    {
        this.id = WebSocketBridge.openFramed(url);
    }

    public void send(byte[] data)
    {
        WebSocketBridge.send(this.id, data, data.length);
    }

    public void sendText(String text)
    {
        WebSocketBridge.sendText(this.id, text);
    }

    public Frame poll()
    {
        Frame frame = this.next();

        if (frame != null)
        {
            return frame;
        }

        // read what the page received into the stream, then try again
        while (true)
        {
            int n = WebSocketBridge.read(this.id, this.buffer);

            if (n < 0)
            {
                this.closed = true;
                break;
            }

            if (n == 0)
            {
                break;
            }

            this.append(this.buffer, n);

            if (n < this.buffer.length)
            {
                break;
            }
        }

        return this.next();
    }

    private void append(byte[] data, int n)
    {
        int left = this.pending.length - this.offset;
        byte[] joined = new byte[left + n];
        System.arraycopy(this.pending, this.offset, joined, 0, left);
        System.arraycopy(data, 0, joined, left, n);
        this.pending = joined;
        this.offset = 0;
    }

    /** One whole message from the received bytes, or null. */
    private Frame next()
    {
        int left = this.pending.length - this.offset;

        if (left < 5)
        {
            return null;
        }

        int o = this.offset;
        int length = (this.pending[o + 1] & 255) << 24 | (this.pending[o + 2] & 255) << 16 | (this.pending[o + 3] & 255) << 8 | this.pending[o + 4] & 255;

        if (left < 5 + length)
        {
            return null;
        }

        byte[] data = new byte[length];
        System.arraycopy(this.pending, o + 5, data, 0, length);
        this.offset += 5 + length;
        return new Frame(this.pending[o] == 1, data);
    }

    public boolean isClosed()
    {
        // set by poll() once the page has nothing more for us and the socket closed
        return this.closed;
    }

    public String closeReason()
    {
        return WebSocketBridge.closeReason(this.id);
    }

    public void close()
    {
        WebSocketBridge.close(this.id);
    }
}
