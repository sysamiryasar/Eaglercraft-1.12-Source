package net.sythix.client.module.movement;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.sythix.client.module.Category;
import net.sythix.client.module.hud.TextHudModule;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ModeSetting;

/**
 * #18 Speed, velocity, jump and movement statistics.
 */
public class MovementHud extends TextHudModule
{
    private final ModeSetting unit;
    private final BooleanSetting showSpeed;
    private final BooleanSetting showVelocity;
    private final BooleanSetting showJump;
    private final BooleanSetting showAirTime;
    private final BooleanSetting showDistance;

    private double speed;
    private double vx;
    private double vy;
    private double vz;
    private double jumpStartY;
    private double jumpPeakY;
    private double lastJumpHeight;
    private int airTicks;
    private int lastAirTicks;
    private double distance;

    public MovementHud()
    {
        super("Movement HUD", "Speed, velocity, jump height, air time and distance travelled.", Category.MOVEMENT, false, 0.005D, 0.88D);
        this.unit = this.add(new ModeSetting("Speed Unit", "Blocks per second or km/h.", "Blocks/s", "Blocks/s", "km/h"));
        this.showSpeed = this.add(new BooleanSetting("Speed", "Horizontal speed.", true));
        this.showVelocity = this.add(new BooleanSetting("Velocity", "Movement per second on each axis.", false));
        this.showJump = this.add(new BooleanSetting("Jump Height", "Height of your last jump or fall.", false));
        this.showAirTime = this.add(new BooleanSetting("Air Time", "How long your last jump lasted.", false));
        this.showDistance = this.add(new BooleanSetting("Distance", "Distance travelled this session.", false));
        this.add(new net.sythix.client.setting.ButtonSetting("Reset Distance", "Set the distance counter back to 0.", "Reset", () -> this.distance = 0.0D));
    }

    public void onTick()
    {
        Entity e = this.mc.getRenderViewEntity();

        if (e == null)
        {
            return;
        }

        double dx = e.posX - e.prevPosX;
        double dy = e.posY - e.prevPosY;
        double dz = e.posZ - e.prevPosZ;
        this.vx = dx * 20.0D;
        this.vy = dy * 20.0D;
        this.vz = dz * 20.0D;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        this.speed = horizontal * 20.0D;

        if (horizontal < 10.0D)
        {
            this.distance += horizontal;
        }

        if (e.onGround)
        {
            if (this.airTicks > 0)
            {
                this.lastAirTicks = this.airTicks;
                this.lastJumpHeight = this.jumpPeakY - this.jumpStartY;
            }

            this.airTicks = 0;
            this.jumpStartY = e.posY;
            this.jumpPeakY = e.posY;
        }
        else
        {
            ++this.airTicks;
            this.jumpPeakY = Math.max(this.jumpPeakY, e.posY);
        }
    }

    private String speed(double blocksPerSecond)
    {
        return this.unit.is("km/h") ? String.format("%.1f km/h", blocksPerSecond * 3.6D) : String.format("%.2f b/s", blocksPerSecond);
    }

    protected List<String[]> getLines()
    {
        List<String[]> lines = new ArrayList<String[]>();

        if (this.showSpeed.isOn())
        {
            lines.add(new String[] {"Speed", this.speed(this.speed)});
        }

        if (this.showVelocity.isOn())
        {
            lines.add(new String[] {"Velocity", String.format("%.1f, %.1f, %.1f", this.vx, this.vy, this.vz)});
        }

        if (this.showJump.isOn())
        {
            lines.add(new String[] {"Jump", String.format("%.2f blocks", this.lastJumpHeight)});
        }

        if (this.showAirTime.isOn())
        {
            lines.add(new String[] {"Air", String.format("%.2fs", (float)this.lastAirTicks / 20.0F)});
        }

        if (this.showDistance.isOn())
        {
            lines.add(new String[] {"Distance", String.format("%.0f blocks", this.distance)});
        }

        if (lines.isEmpty())
        {
            lines.add(new String[] {"Speed", this.speed(this.speed)});
        }

        return lines;
    }
}
