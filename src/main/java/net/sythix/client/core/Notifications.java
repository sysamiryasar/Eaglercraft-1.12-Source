package net.sythix.client.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;

/**
 * Toast notifications shown in the corner of the screen. Drawn by the Notifications module.
 */
public final class Notifications
{
    public enum Kind
    {
        INFO,
        SUCCESS,
        WARNING,
        ERROR,
        SOCIAL
    }

    public static final class Toast
    {
        public final String title;
        public final String message;
        public final Kind kind;
        public final long created;

        Toast(String title, String message, Kind kind)
        {
            this.title = title;
            this.message = message;
            this.kind = kind;
            this.created = Minecraft.getSystemTime();
        }
    }

    private static final List<Toast> ACTIVE = new ArrayList<Toast>();
    private static final List<Toast> HISTORY = new ArrayList<Toast>();

    private Notifications()
    {
    }

    public static synchronized void post(String title, String message, Kind kind)
    {
        Toast toast = new Toast(title, message, kind);
        ACTIVE.add(toast);
        HISTORY.add(0, toast);

        while (ACTIVE.size() > 5)
        {
            ACTIVE.remove(0);
        }

        while (HISTORY.size() > 50)
        {
            HISTORY.remove(HISTORY.size() - 1);
        }
    }

    public static void info(String title, String message)
    {
        post(title, message, Kind.INFO);
    }

    /** Toasts still on screen; ones older than lifetimeMs are removed. */
    public static synchronized List<Toast> active(long lifetimeMs)
    {
        long now = Minecraft.getSystemTime();

        for (Iterator<Toast> it = ACTIVE.iterator(); it.hasNext();)
        {
            if (now - it.next().created > lifetimeMs)
            {
                it.remove();
            }
        }

        return new ArrayList<Toast>(ACTIVE);
    }

    public static synchronized List<Toast> history()
    {
        return new ArrayList<Toast>(HISTORY);
    }
}
