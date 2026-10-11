package net.sythix.client.module.hud;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.init.SoundEvents;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;
import net.sythix.client.ui.SythixTheme;

/**
 * #31 Toast notifications: duration, position, sound and event filters.
 */
public class NotificationsModule extends Module
{
    private final NumberSetting duration;
    private final ModeSetting corner;
    private final BooleanSetting sound;
    private final BooleanSetting info;
    private final BooleanSetting success;
    private final BooleanSetting warnings;
    private final BooleanSetting errors;
    private final BooleanSetting social;
    private int lastCount;

    public NotificationsModule()
    {
        super("Notifications", "Pop-up messages from Sythix: duration, position, sound and filters.", Category.INTERFACE, true);
        this.duration = this.add(new NumberSetting("Duration", "How long each notification stays.", 4.0D, 1.0D, 15.0D, 0.5D, "s"));
        this.corner = this.add(new ModeSetting("Position", "Screen corner for notifications.", "Bottom Right", "Top Right", "Bottom Right", "Top Left", "Bottom Left"));
        this.sound = this.add(new BooleanSetting("Sound", "Play a soft sound for new notifications.", false));
        this.info = this.add(new BooleanSetting("Info", "General information.", true));
        this.success = this.add(new BooleanSetting("Success", "Confirmations, such as \"saved\".", true));
        this.warnings = this.add(new BooleanSetting("Warnings", "Warnings, such as low durability.", true));
        this.errors = this.add(new BooleanSetting("Errors", "Problems, such as a module being switched off.", true));
        this.social = this.add(new BooleanSetting("Social", "Friends joining, party invites.", true));
    }

    private boolean allowed(Notifications.Kind kind)
    {
        switch (kind)
        {
            case SUCCESS:
                return this.success.isOn();
            case WARNING:
                return this.warnings.isOn();
            case ERROR:
                return this.errors.isOn();
            case SOCIAL:
                return this.social.isOn();
            default:
                return this.info.isOn();
        }
    }

    private static int color(Notifications.Kind kind)
    {
        switch (kind)
        {
            case SUCCESS:
                return 0xFF55FF55;
            case WARNING:
                return 0xFFFFD24A;
            case ERROR:
                return SythixTheme.RED;
            case SOCIAL:
                return SythixTheme.LIGHT_BLUE;
            default:
                return SythixTheme.BLUE;
        }
    }

    /** Draws active notifications; called after the HUD and menus every frame. */
    public void render(ScaledResolution res)
    {
        long life = (long)(this.duration.getFloat() * 1000.0F);
        List<Notifications.Toast> toasts = Notifications.active(life);
        toasts.removeIf(t -> !this.allowed(t.kind));

        if (toasts.size() > this.lastCount && this.sound.isOn())
        {
            this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.BLOCK_NOTE_HAT, 1.6F));
        }

        this.lastCount = toasts.size();
        FontRenderer font = this.mc.fontRenderer;
        boolean right = this.corner.get().endsWith("Right");
        boolean bottom = this.corner.get().startsWith("Bottom");
        int y = bottom ? res.getScaledHeight() - 6 : 6;
        long now = Minecraft.getSystemTime();

        for (int i = toasts.size() - 1; i >= 0; --i)
        {
            Notifications.Toast toast = toasts.get(i);
            int w = Math.max(font.getStringWidth(toast.title), font.getStringWidth(toast.message)) + 14;
            w = Math.min(w, res.getScaledWidth() / 2);
            int h = 24;
            long age = now - toast.created;
            float slide = SythixTheme.reducedMotion ? 1.0F : Math.min(1.0F, Math.min((float)age / 200.0F, (float)(life - age) / 200.0F));
            int offset = (int)((1.0F - slide) * (float)(w + 8));
            int x = right ? res.getScaledWidth() - 6 - w + offset : 6 - offset;
            int top = bottom ? y - h : y;
            SythixTheme.drawRect((float)x, (float)top, (float)(x + w), (float)(top + h), 0xE016161B);
            SythixTheme.drawRect((float)x, (float)top, (float)(x + 2), (float)(top + h), color(toast.kind));
            float progress = 1.0F - (float)age / (float)life;
            SythixTheme.drawRect((float)(x + 2), (float)(top + h - 1), (float)(x + 2) + (float)(w - 2) * Math.max(0.0F, progress), (float)(top + h), SythixTheme.withAlpha(color(toast.kind), 0xA0));
            font.drawString(font.trimStringToWidth(toast.title, w - 10), (float)(x + 7), (float)(top + 4), 0xFFFFFFFF, true);
            font.drawString(font.trimStringToWidth(toast.message, w - 10), (float)(x + 7), (float)(top + 14), SythixTheme.TEXT_MUTED, false);
            y = bottom ? top - 4 : top + h + 4;
        }
    }
}
