package net.sythix.client.web;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

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
    private static Field mouseBuffer;
    private static boolean broken;

    private WebInput()
    {
    }

    /** Mouse wheel steps the page counted while the mouse was locked (up positive), since the last call. */
    private static native int wheel();

    /** 1 when the browser unlocked the mouse since the last call (the player pressed Escape), else 0. */
    private static native int pointerLockLost();

    /**
     * True when the browser let go of the mouse by itself since the last call. The browser keeps
     * Escape for that and doesn't pass it on, so the game treats this as Escape.
     */
    public static boolean mouseReleasedByBrowser()
    {
        return !broken && pointerLockLost() != 0;
    }

    /**
     * Adds the wheel steps as LWJGL mouse events (button -1, deltas 0, wheel 120 per step), so the
     * game scrolls the hotbar and menus as with a wheel on desktop. Grabbed, an event carries mouse
     * movement (none here); otherwise the mouse position, so the list under the mouse scrolls.
     */
    private static void injectWheel()
    {
        int steps = wheel();

        if (steps == 0 || !Mouse.isCreated())
        {
            return;
        }

        boolean grabbed = Mouse.isGrabbed();
        int x = grabbed ? 0 : Mouse.getX();
        int y = grabbed ? 0 : Mouse.getY();

        try
        {
            if (mouseBuffer == null)
            {
                mouseBuffer = Mouse.class.getDeclaredField("readBuffer");
                mouseBuffer.setAccessible(true);
            }

            ByteBuffer queue = (ByteBuffer)mouseBuffer.get(null);
            queue.compact();
            int step = steps > 0 ? 1 : -1;

            for (int i = 0; i < Math.abs(steps) && queue.remaining() >= Mouse.EVENT_SIZE; ++i)
            {
                queue.put((byte)-1).put((byte)0).putInt(x).putInt(y).putInt(120 * step).putLong(System.nanoTime());
            }

            queue.flip();
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            broken = true;
            org.apache.logging.log4j.LogManager.getLogger().warn("[web] could not add mouse wheel events", e);
        }
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

        injectWheel();

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
