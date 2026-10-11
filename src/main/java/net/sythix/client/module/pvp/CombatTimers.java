package net.sythix.client.module.pvp;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.sythix.client.module.Category;
import net.sythix.client.module.hud.StyledHudModule;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.KeybindSetting;
import net.sythix.client.ui.SythixTheme;

/**
 * #15 Match timer and item cooldown indicators (ender pearl, chorus fruit, shield, totem...).
 */
public class CombatTimers extends StyledHudModule
{
    private static final Item[] TRACKED = new Item[] {Items.ENDER_PEARL, Items.CHORUS_FRUIT, Items.SHIELD, Items.ELYTRA, Items.SNOWBALL, Items.EGG};
    private final BooleanSetting matchTimer;
    private final BooleanSetting autoStart;
    private final KeybindSetting resetKey;
    private final BooleanSetting cooldowns;
    private final BooleanSetting attackCooldown;
    private long matchStart;

    public CombatTimers()
    {
        super("Combat Timers", "Match timer plus cooldown bars for pearls, chorus fruit, shields and attacks.", Category.PVP, false, 0.5D, 0.12D);
        this.matchTimer = this.add(new BooleanSetting("Match Timer", "Time since the match (world) started.", true));
        this.autoStart = this.add(new BooleanSetting("Restart On Join", "Restart the timer whenever you join a world or server.", true)).visibleWhen(this.matchTimer::isOn);
        this.resetKey = this.add(new KeybindSetting("Restart Key", "Restart the match timer by hand.", 0));
        this.cooldowns = this.add(new BooleanSetting("Item Cooldowns", "Bars for items on cooldown (pearl, chorus, shield...).", true));
        this.attackCooldown = this.add(new BooleanSetting("Attack Cooldown", "Bar showing your weapon cooldown.", false));
    }

    public void onWorldJoin()
    {
        if (this.autoStart.isOn())
        {
            this.matchStart = Minecraft.getSystemTime();
        }
    }

    public void onKey(int keyCode)
    {
        if (keyCode != 0 && keyCode == this.resetKey.getKey())
        {
            this.matchStart = Minecraft.getSystemTime();
        }
    }

    /** {item, progress 0..1 remaining} for each item on cooldown. */
    private List<Object[]> activeCooldowns(boolean preview)
    {
        List<Object[]> list = new ArrayList<Object[]>();

        if (this.cooldowns.isOn() && this.mc.player != null)
        {
            for (Item item : TRACKED)
            {
                float f = this.mc.player.getCooldownTracker().getCooldown(item, 0.0F);

                if (f > 0.0F)
                {
                    list.add(new Object[] {item, Float.valueOf(f)});
                }
            }
        }

        if (preview && list.isEmpty() && this.cooldowns.isOn())
        {
            list.add(new Object[] {Items.ENDER_PEARL, Float.valueOf(0.6F)});
        }

        return list;
    }

    public int getWidth()
    {
        return 90 + this.getPad() * 2;
    }

    public int getHeight()
    {
        int h = this.matchTimer.isOn() ? 10 : 0;
        h += this.activeCooldowns(true).size() * 18;
        h += this.attackCooldown.isOn() ? 8 : 0;
        return Math.max(8, h) + this.getPad() * 2;
    }

    protected void renderContent(boolean preview)
    {
        int width = this.getWidth();
        this.drawBox(width, this.getHeight());
        int x = this.getPad();
        int y = this.getPad();
        int inner = width - this.getPad() * 2;

        if (this.matchTimer.isOn())
        {
            long ms = this.matchStart == 0L ? 0L : Minecraft.getSystemTime() - this.matchStart;
            this.drawText("Match ", (float)x, (float)y, this.labelColor.getColor());
            this.drawText(String.format("%d:%02d", ms / 60000L, ms / 1000L % 60L), (float)(x + this.mc.fontRenderer.getStringWidth("Match ")), (float)y, this.valueColor.getColor());
            y += 10;
        }

        for (Object[] cd : this.activeCooldowns(preview))
        {
            this.drawItem(new ItemStack((Item)cd[0]), x, y, false);
            float f = ((Float)cd[1]).floatValue();
            SythixTheme.drawRect((float)(x + 18), (float)(y + 6), (float)(x + inner), (float)(y + 10), 0x60000000);
            SythixTheme.drawGradientH((float)(x + 18), (float)(y + 6), (float)(x + 18) + (float)(inner - 18) * f, (float)(y + 10), SythixTheme.RED, SythixTheme.BLUE);
            y += 18;
        }

        if (this.attackCooldown.isOn())
        {
            float strength = this.mc.player == null ? 1.0F : this.mc.player.getCooledAttackStrength(0.0F);
            SythixTheme.drawRect((float)x, (float)(y + 2), (float)(x + inner), (float)(y + 6), 0x60000000);
            SythixTheme.drawRect((float)x, (float)(y + 2), (float)x + (float)inner * strength, (float)(y + 6), strength >= 1.0F ? 0xFF55FF55 : SythixTheme.LIGHT_BLUE);
        }
    }
}
