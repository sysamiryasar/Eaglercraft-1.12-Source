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

        net.sythix.client.ui.font.SmoothFont title = net.sythix.client.ui.font.SmoothFont.BOLD;
        net.sythix.client.ui.font.SmoothFont body = net.sythix.client.ui.font.SmoothFont.REGULAR;

        for (int i = toasts.size() - 1; i >= 0; --i)
        {
            Notifications.Toast toast = toasts.get(i);
            float tw = Math.max(title.width(toast.title, 0.95F), body.width(toast.message, 0.85F));
            int w = (int)Math.min(tw + 36.0F, res.getScaledWidth() / 2.0F);
            int h = 28;
            long age = now - toast.created;
            float slide = SythixTheme.reducedMotion ? 1.0F : Math.min(1.0F, Math.min((float)age / 220.0F, (float)(life - age) / 220.0F));
            slide = 1.0F - (1.0F - slide) * (1.0F - slide);
            int offset = (int)((1.0F - slide) * (float)(w + 8));
            int x = right ? res.getScaledWidth() - 6 - w + offset : 6 - offset;
            int top = bottom ? y - h : y;
            int c = color(toast.kind);

            // glass card with an outline in the toast's colour and an icon badge
            SythixTheme.drawRounded(x, top, x + w, top + h, 5.0F, SythixTheme.withAlpha(0xFF0D0C11, (int)(0xE6 * slide)));
            SythixTheme.drawRoundedOutline(x, top, x + w, top + h, 5.0F, 0.75F, SythixTheme.withAlpha(c, (int)(0xC8 * slide)));
            SythixTheme.drawRounded(x + 6, top + 7, x + 20, top + 21, 7.0F, SythixTheme.withAlpha(c, (int)(0x40 * slide)));
            String icon = toast.kind == Notifications.Kind.SUCCESS ? "\u2713" : toast.kind == Notifications.Kind.WARNING ? "!" : toast.kind == Notifications.Kind.ERROR ? "\u2715" : "i";
            title.drawCentered(icon, x + 13.0F, top + 10.5F, 0.85F, SythixTheme.withAlpha(c | 0xFF000000, (int)(255 * slide)));
            String t = toast.title;
            String m = toast.message;

            while (t.length() > 1 && title.width(t, 0.95F) > w - 32)
            {
                t = t.substring(0, t.length() - 1);
            }

            while (m.length() > 1 && body.width(m, 0.85F) > w - 32)
            {
                m = m.substring(0, m.length() - 1);
            }

            title.draw(t, x + 26.0F, top + 5.0F, 0.95F, SythixTheme.withAlpha(0xFFFFFFFF, (int)(255 * slide)));
            body.draw(m, x + 26.0F, top + 15.5F, 0.85F, SythixTheme.withAlpha(SythixTheme.TEXT_MUTED, (int)(255 * slide)));
            float progress = Math.max(0.0F, 1.0F - (float)age / (float)life);
            SythixTheme.drawRect(x + 5.0F, top + h - 1.5F, x + 5.0F + (w - 10.0F) * progress, top + h - 0.75F, SythixTheme.withAlpha(c, (int)(0x90 * slide)));
            y = bottom ? top - 5 : top + h + 5;
        }
    }
}
