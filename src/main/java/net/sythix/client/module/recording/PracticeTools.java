package net.sythix.client.module.recording;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.hud.TextHudModule;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.KeybindSetting;
import net.sythix.client.setting.NumberSetting;

/**
 * #50 Training timers, a CPS test and a practice session HUD.
 */
public class PracticeTools extends TextHudModule
{
    private final KeybindSetting cpsTestKey;
    private final NumberSetting cpsTestLength;
    private final KeybindSetting timerKey;
    private final NumberSetting timerInterval;
    private final BooleanSetting showSession;
    private long testStart;
    private int testClicks;
    private boolean testing;
    private float bestCps;
    private float lastCps;
    private boolean timerOn;
    private long timerLast;
    private long sessionStart;
    private int hits;
    private int swings;

    public PracticeTools()
    {
        super("Practice Tools", "CPS test, interval training timer and a practice session HUD.", Category.RECORDING, false, 0.5D, 0.3D);
        this.cpsTestKey = this.add(new KeybindSetting("CPS Test Key", "Start a click-speed test, then click as fast as you can.", 0));
        this.cpsTestLength = this.add(new NumberSetting("CPS Test Length", "How long the test lasts.", 10.0D, 5.0D, 30.0D, 5.0D, "s"));
        this.timerKey = this.add(new KeybindSetting("Interval Timer Key", "Start or stop a beeping interval timer.", 0));
        this.timerInterval = this.add(new NumberSetting("Interval", "Seconds between beeps.", 30.0D, 5.0D, 300.0D, 5.0D, "s"));
        this.showSession = this.add(new BooleanSetting("Session Stats", "Practice time, hits and accuracy.", true));
    }

    public void onKey(int keyCode)
    {
        if (keyCode == 0)
        {
            return;
        }

        if (keyCode == this.cpsTestKey.getKey())
        {
            this.testing = true;
            this.testClicks = 0;
            this.testStart = Minecraft.getSystemTime();
            Notifications.post("CPS test", "Click as fast as you can for " + this.cpsTestLength.getInt() + " seconds!", Notifications.Kind.INFO);
        }
        else if (keyCode == this.timerKey.getKey())
        {
            this.timerOn = !this.timerOn;
            this.timerLast = Minecraft.getSystemTime();
            Notifications.post("Interval timer", this.timerOn ? "Started (" + this.timerInterval.getInt() + "s)." : "Stopped.", Notifications.Kind.INFO);
        }
    }

    public void onMouseClick(int button)
    {
        if (button == 0)
        {
            ++this.swings;

            if (this.testing)
            {
                ++this.testClicks;
            }
        }
    }

    public void onAttack(Entity target)
    {
        if (target instanceof EntityLivingBase)
        {
            ++this.hits;
        }
    }

    public void onTick()
    {
        long now = Minecraft.getSystemTime();

        if (this.sessionStart == 0L)
        {
            this.sessionStart = now;
        }

        if (this.testing && now - this.testStart >= (long)(this.cpsTestLength.getInt() * 1000))
        {
            this.testing = false;
            this.lastCps = (float)this.testClicks / (float)this.cpsTestLength.getInt();
            boolean best = this.lastCps > this.bestCps;
            this.bestCps = Math.max(this.bestCps, this.lastCps);
            Notifications.post("CPS test done", String.format("%.1f CPS (%d clicks)%s", this.lastCps, this.testClicks, best ? " - new best!" : ""), Notifications.Kind.SUCCESS);
        }

        if (this.timerOn && now - this.timerLast >= (long)(this.timerInterval.getInt() * 1000))
        {
            this.timerLast = now;
            this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.BLOCK_NOTE_PLING, 1.0F));
        }
    }

    protected List<String[]> getLines()
    {
        List<String[]> lines = new ArrayList<String[]>();
        long now = Minecraft.getSystemTime();

        if (this.testing)
        {
            long left = (long)(this.cpsTestLength.getInt() * 1000) - (now - this.testStart);
            lines.add(new String[] {"CPS Test", String.format("%.1fs left, %d clicks", Math.max(0L, left) / 1000.0F, this.testClicks)});
        }
        else
        {
            lines.add(new String[] {"CPS Test", String.format("last %.1f, best %.1f", this.lastCps, this.bestCps)});
        }

        if (this.timerOn)
        {
            long left = (long)(this.timerInterval.getInt() * 1000) - (now - this.timerLast);
            lines.add(new String[] {"Timer", String.format("%ds", Math.max(0L, left) / 1000L + 1L)});
        }

        if (this.showSession.isOn())
        {
            long ms = this.sessionStart == 0L ? 0L : now - this.sessionStart;
            lines.add(new String[] {"Practice", String.format("%d:%02d", ms / 60000L, ms / 1000L % 60L)});
            lines.add(new String[] {"Hits", this.hits + " (" + (this.swings == 0 ? 0 : Math.min(100, this.hits * 100 / this.swings)) + "% acc)"});
        }

        return lines;
    }
}
