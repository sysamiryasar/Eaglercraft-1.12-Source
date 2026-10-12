package net.sythix.client.web;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import org.lwjgl.input.Keyboard;

/**
 * Keys the browser layer drops (Enter, Backspace, Tab, arrows, F1-F12, Delete...). The page catches
 * them (web/sythix-web.js) and they are added to LWJGL's keyboard queue here, so the game sees them
 * like any other key.
 */
public final class WebInput
{
    private static final int[] EVENTS = new int[4 * 64];
    private static Field readBuffer;
    private static Field keyDownBuffer;
    private static boolean broken;

    private WebInput()
    {
    }

    /** Fills out with {key, character, down, repeat} per event and returns how many events. */
    private static native int poll(int[] out);

    /** Moves the page's key events into LWJGL. Called every frame on the client thread. */
    public static void inject()
    {
        if (broken || !Keyboard.isCreated())
        {
            return;
        }

        int n = poll(EVENTS);

        if (n <= 0)
        {
            return;
        }

        try
        {
            if (readBuffer == null)
            {
                readBuffer = Keyboard.class.getDeclaredField("readBuffer");
                readBuffer.setAccessible(true);
                keyDownBuffer = Keyboard.class.getDeclaredField("keyDownBuffer");
                keyDownBuffer.setAccessible(true);
            }

            ByteBuffer queue = (ByteBuffer)readBuffer.get(null);
            ByteBuffer down = (ByteBuffer)keyDownBuffer.get(null);
            queue.compact();

            for (int i = 0; i < n && queue.remaining() >= Keyboard.EVENT_SIZE; ++i)
            {
                int key = EVENTS[i * 4];
                boolean pressed = EVENTS[i * 4 + 2] != 0;
                queue.putInt(key).put((byte)(pressed ? 1 : 0)).putInt(EVENTS[i * 4 + 1]).putLong(System.nanoTime()).put((byte)EVENTS[i * 4 + 3]);
                down.put(key, (byte)(pressed ? 1 : 0));
            }

            queue.flip();
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            broken = true;
            org.apache.logging.log4j.LogManager.getLogger().warn("[web] could not add browser key events", e);
        }
    }
}
