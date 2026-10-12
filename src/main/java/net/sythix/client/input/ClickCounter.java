package net.sythix.client.input;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;

/**
 * Counts real mouse clicks over the last second. It only observes clicks; it never creates any.
 */
public class ClickCounter
{
    private static final long MAX_WINDOW_MS = 5000L;
    private final Deque<Long> leftClicks = new ArrayDeque<Long>();
    private final Deque<Long> rightClicks = new ArrayDeque<Long>();

    /** Called for each mouse button press while playing (0 = left, 1 = right). */
    public void onClick(int button)
    {
        if (button == 0)
        {
            this.leftClicks.addLast(Long.valueOf(Minecraft.getSystemTime()));
        }
        else if (button == 1)
        {
            this.rightClicks.addLast(Long.valueOf(Minecraft.getSystemTime()));
        }
    }

    public int getLeftCps()
    {
        return Math.round(this.getLeft(1000L));
    }

    public int getRightCps()
    {
        return Math.round(this.getRight(1000L));
    }

    /** Clicks per second averaged over the last windowMs milliseconds. */
    public float getLeft(long windowMs)
    {
        return count(this.leftClicks, windowMs) * 1000.0F / (float)windowMs;
    }

    public float getRight(long windowMs)
    {
        return count(this.rightClicks, windowMs) * 1000.0F / (float)windowMs;
    }

    /** Total clicks in the last windowMs milliseconds. */
    public int countLeft(long windowMs)
    {
        return count(this.leftClicks, windowMs);
    }

    private static int count(Deque<Long> clicks, long windowMs)
    {
        long now = Minecraft.getSystemTime();

        while (!clicks.isEmpty() && clicks.peekFirst().longValue() < now - MAX_WINDOW_MS)
        {
            clicks.removeFirst();
        }

        int n = 0;

        for (Long t : clicks)
        {
            if (t.longValue() >= now - windowMs)
            {
                ++n;
            }
        }

        return n;
    }
}
