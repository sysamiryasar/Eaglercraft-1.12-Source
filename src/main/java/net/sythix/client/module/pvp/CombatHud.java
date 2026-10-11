package net.sythix.client.module.pvp;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.sythix.client.module.Category;
import net.sythix.client.module.hud.TextHudModule;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.NumberSetting;

/**
 * #10 Combo count, hit statistics and information about your current target. Display only: it
 * shows what the client already knows and never changes how combat works.
 */
public class CombatHud extends TextHudModule
{
    private final BooleanSetting showCombo;
    private final BooleanSetting showReach;
    private final BooleanSetting showAccuracy;
    private final BooleanSetting showTarget;
    private final NumberSetting comboTimeout;
    private final NumberSetting targetTime;

    private int combo;
    private long lastHit;
    private int lastHurtTime;
    private double lastReach = -1.0D;
    private int swings;
    private int hits;
    private EntityLivingBase target;
    private long targetTimeMs;

    public CombatHud()
    {
        super("Combat HUD", "Combo count, last hit distance, hit accuracy and target information.", Category.PVP, false, 0.5D, 0.62D);
        this.showCombo = this.add(new BooleanSetting("Combo", "Hits in a row; resets when you get hit.", true));
        this.showReach = this.add(new BooleanSetting("Hit Distance", "Distance of your last hit (display only).", true));
        this.showAccuracy = this.add(new BooleanSetting("Accuracy", "Hits compared to swings this session.", false));
        this.showTarget = this.add(new BooleanSetting("Target Info", "Name, health and distance of who you are fighting, when the server sends it.", true));
        this.comboTimeout = this.add(new NumberSetting("Combo Timeout", "Combo resets after this long without a hit.", 3.0D, 1.0D, 10.0D, 0.5D, "s"));
        this.targetTime = this.add(new NumberSetting("Target Time", "How long the last target stays shown.", 5.0D, 1.0D, 20.0D, 1.0D, "s"));
        this.add(new net.sythix.client.setting.ButtonSetting("Reset Stats", "Clear accuracy and combo statistics.", "Reset", () -> {
            this.swings = 0;
            this.hits = 0;
            this.combo = 0;
        }));
    }

    public void onMouseClick(int button)
    {
        if (button == 0)
        {
            ++this.swings;
        }
    }

    public void onAttack(Entity entity)
    {
        if (!(entity instanceof EntityLivingBase))
        {
            return;
        }

        EntityLivingBase living = (EntityLivingBase)entity;
        ++this.hits;
        this.target = living;
        this.targetTimeMs = Minecraft.getSystemTime();

        if (living.hurtTime == 0)
        {
            ++this.combo;
            this.lastHit = Minecraft.getSystemTime();
        }

        Entity viewer = this.mc.getRenderViewEntity();

        if (viewer != null)
        {
            Vec3d eyes = viewer.getPositionEyes(1.0F);
            Vec3d look = viewer.getLook(1.0F);
            AxisAlignedBB box = entity.getEntityBoundingBox().grow((double)entity.getCollisionBorderSize());
            RayTraceResult hit = box.calculateIntercept(eyes, eyes.addVector(look.x * 6.0D, look.y * 6.0D, look.z * 6.0D));
            this.lastReach = box.contains(eyes) ? 0.0D : (hit == null ? eyes.distanceTo(entity.getPositionVector()) : eyes.distanceTo(hit.hitVec));
        }
    }

    public void onTick()
    {
        if (this.mc.player == null)
        {
            this.combo = 0;
            this.target = null;
            return;
        }

        int hurt = this.mc.player.hurtTime;

        if (hurt > this.lastHurtTime)
        {
            this.combo = 0;
        }

        this.lastHurtTime = hurt;

        if (this.combo > 0 && Minecraft.getSystemTime() - this.lastHit > (long)(this.comboTimeout.getFloat() * 1000.0F))
        {
            this.combo = 0;
        }

        if (this.target != null && (this.target.isDead || Minecraft.getSystemTime() - this.targetTimeMs > (long)(this.targetTime.getFloat() * 1000.0F)))
        {
            this.target = null;
        }
    }

    protected List<String[]> getLines()
    {
        List<String[]> lines = new ArrayList<String[]>();

        if (this.showCombo.isOn())
        {
            lines.add(new String[] {"Combo", String.valueOf(this.combo)});
        }

        if (this.showReach.isOn())
        {
            lines.add(new String[] {"Hit Dist", this.lastReach < 0.0D ? "--" : String.format("%.2f", this.lastReach)});
        }

        if (this.showAccuracy.isOn())
        {
            lines.add(new String[] {"Accuracy", this.swings == 0 ? "--" : Math.min(100, this.hits * 100 / this.swings) + "%"});
        }

        if (this.showTarget.isOn())
        {
            if (this.target != null && this.mc.player != null)
            {
                lines.add(new String[] {"Target", this.target.getName()});
                lines.add(new String[] {"Health", String.format("%.1f / %.0f", this.target.getHealth(), this.target.getMaxHealth())});
                lines.add(new String[] {"Distance", String.format("%.1f", this.mc.player.getDistanceToEntity(this.target))});
            }
            else
            {
                lines.add(new String[] {"Target", "None"});
            }
        }

        if (lines.isEmpty())
        {
            lines.add(new String[] {"Combat", "--"});
        }

        return lines;
    }
}
