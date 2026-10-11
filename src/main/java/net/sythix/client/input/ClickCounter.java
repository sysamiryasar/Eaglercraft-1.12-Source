package net.sythix.client.input;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;

/**
 * Counts real mouse clicks over the last second. It only observes clicks; it never creates any.
 */
public class ClickCounter
{
    private static final long WINDOW_MS = 1000L;
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
        return count(this.leftClicks);
    }

    public int getRightCps()
    {
        return count(this.rightClicks);
    }

    private static int count(Deque<Long> clicks)
    {
        long cutoff = Minecraft.getSystemTime() - WINDOW_MS;

        while (!clicks.isEmpty() && clicks.peekFirst().longValue() < cutoff)
        {
            clicks.removeFirst();
        }

        return clicks.size();
    }
}
