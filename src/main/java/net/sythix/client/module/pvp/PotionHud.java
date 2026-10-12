package net.sythix.client.module.pvp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.sythix.client.module.Category;
import net.sythix.client.module.hud.StyledHudModule;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;
import net.sythix.client.ui.SythixTheme;

/**
 * #14 Effect icons, timers, sorting and warning thresholds.
 */
public class PotionHud extends StyledHudModule
{
    private final BooleanSetting showIcon;
    private final BooleanSetting showName;
    private final BooleanSetting showLevel;
    private final BooleanSetting potionColors;
    private final ModeSetting sort;
    private final BooleanSetting blink;
    private final NumberSetting warnSeconds;
    private final BooleanSetting hideVanilla;

    public PotionHud()
    {
        super("Potion HUD", "Active effects with icons, timers, sorting and ending warnings.", Category.PVP, false, 0.995D, 0.5D);
        this.showIcon = this.add(new BooleanSetting("Icons", "Show effect icons.", true));
        this.showName = this.add(new BooleanSetting("Names", "Show effect names.", true));
        this.showLevel = this.add(new BooleanSetting("Levels", "Show the level, for example \"II\".", true));
        this.potionColors = this.add(new BooleanSetting("Effect Colors", "Colour each name with the effect's own colour.", false));
        this.sort = this.add(new ModeSetting("Sort", "Order of the list.", "Duration", "Duration", "Name", "Level"));
        this.blink = this.add(new BooleanSetting("Blink When Ending", "Flash the timer when the effect is ending.", true));
        this.warnSeconds = this.add(new NumberSetting("Warning Time", "Effects with less time than this are shown in red.", 10.0D, 3.0D, 60.0D, 1.0D, "s"));
        this.hideVanilla = this.add(new BooleanSetting("Hide Vanilla Icons", "Hide the default effect icons in the top right corner.", true));
    }

    public boolean hideVanillaOverlay()
    {
        return this.hideVanilla.isOn();
    }

    private List<PotionEffect> effects(boolean preview)
    {
        List<PotionEffect> list = new ArrayList<PotionEffect>();

        if (this.mc.player != null)
        {
            list.addAll(this.mc.player.getActivePotionEffects());
        }

        if (list.isEmpty() && preview)
        {
            list.add(new PotionEffect(MobEffects.SPEED, 1800, 1));
            list.add(new PotionEffect(MobEffects.STRENGTH, 900, 0));
            list.add(new PotionEffect(MobEffects.FIRE_RESISTANCE, 150, 0));
        }

        if (this.sort.is("Name"))
        {
            Collections.sort(list, Comparator.comparing(e -> I18n.format(e.getPotion().getName())));
        }
        else if (this.sort.is("Level"))
        {
            Collections.sort(list, Comparator.comparingInt(PotionEffect::getAmplifier).reversed());
        }
        else
        {
            Collections.sort(list, Comparator.comparingInt(PotionEffect::getDuration).reversed());
        }

        return list;
    }

    private String name(PotionEffect effect)
    {
        String s = this.showName.isOn() ? I18n.format(effect.getPotion().getName()) : "";

        if (this.showLevel.isOn() && effect.getAmplifier() > 0)
        {
            s = s + (s.isEmpty() ? "" : " ") + I18n.format("enchantment.level." + (effect.getAmplifier() + 1));
        }

        return s;
    }

    private int iconSpace()
    {
        return this.showIcon.isOn() ? 20 : 0;
    }

    public boolean shouldRender()
    {
        return this.mc.player != null && !this.mc.player.getActivePotionEffects().isEmpty();
    }

    public int getWidth()
    {
        int w = 40;

        for (PotionEffect e : this.effects(true))
        {
            w = Math.max(w, Math.max(this.mc.fontRenderer.getStringWidth(this.name(e)), this.mc.fontRenderer.getStringWidth(Potion.getPotionDurationString(e, 1.0F))));
        }

        return w + this.iconSpace() + this.getPad() * 2;
    }

    public int getHeight()
    {
        return Math.max(1, this.effects(true).size()) * 22 - 2 + this.getPad() * 2;
    }

    protected void renderContent(boolean preview)
    {
        this.drawBox(this.getWidth(), this.getHeight());
        int y = this.getPad();

        for (PotionEffect effect : this.effects(preview))
        {
            Potion potion = effect.getPotion();
            int x = this.getPad();

            if (this.showIcon.isOn() && potion.hasStatusIcon())
            {
                int index = potion.getStatusIconIndex();
                float u = (float)(index % 8 * 18);
                float v = (float)(198 + index / 8 * 18);
                SythixTheme.drawTexture(GuiContainer.INVENTORY_BACKGROUND, (float)x, (float)(y + 1), 18.0F, 18.0F, u / 256.0F, v / 256.0F, (u + 18.0F) / 256.0F, (v + 18.0F) / 256.0F, 1.0F);
            }

            x += this.iconSpace();
            String name = this.name(effect);
            boolean ending = !effect.getIsPotionDurationMax() && effect.getDuration() < this.warnSeconds.getInt() * 20;
            boolean hidden = this.blink.isOn() && ending && (Minecraft.getSystemTime() / 400L) % 2L == 0L;

            if (!name.isEmpty())
            {
                this.drawText(name, (float)x, (float)(y + 1), this.potionColors.isOn() ? potion.getLiquidColor() : this.labelColor.getColor());
            }

            if (!hidden)
            {
                this.drawText(Potion.getPotionDurationString(effect, 1.0F), (float)x, (float)(y + (name.isEmpty() ? 6 : 11)), ending ? 0xFFFF5555 : this.valueColor.getColor());
            }

            y += 22;
        }
    }
}
