package net.sythix.client.cosmetic;

import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.MathHelper;
import net.sythix.client.module.visuals.CosmeticsModule;
import net.sythix.client.ui.SythixTheme;
import org.lwjgl.input.Keyboard;

/**
 * The emote wheel: hold the wheel key (M by default), point at an emote and let go to play it.
 * Tapping the key keeps the wheel open; then click an emote, press 1-8, or press the key again to
 * close it. The middle shows you doing the emote you point at.
 */
public class GuiEmoteWheel extends GuiScreen
{
    private static final long TAP_MS = 250L;
    private final CosmeticsModule cosmetics;
    private final long opened = Minecraft.getSystemTime();
    private boolean held = true;
    private int hovered = -1;
    private int lastHovered = -1;
    private long hoverStart;

    public GuiEmoteWheel(CosmeticsModule cosmetics)
    {
        this.cosmetics = cosmetics;
    }

    private float radius()
    {
        return Math.min((float)this.height * 0.3F, 110.0F);
    }

    private int slotAt(int mouseX, int mouseY)
    {
        float dx = (float)mouseX - (float)this.width / 2.0F;
        float dy = (float)mouseY - (float)this.height / 2.0F;

        if (dx * dx + dy * dy < 22.0F * 22.0F)
        {
            return -1;
        }

        // slot 0 at the top, going clockwise
        double angle = Math.atan2(dx, -dy);
        int slot = (int)Math.round(angle / (Math.PI / 4.0D));
        return (slot + 8) % 8;
    }

    public void updateScreen()
    {
        int key = this.cosmetics.wheelKey();

        if (this.held && key > 0 && !Keyboard.isKeyDown(key))
        {
            this.held = false;

            if (Minecraft.getSystemTime() - this.opened > TAP_MS)
            {
                // released after holding: play what is pointed at (or nothing) and close
                this.choose(this.hovered);
            }
        }
    }

    private void choose(int slot)
    {
        this.mc.displayGuiScreen(null);

        if (slot >= 0)
        {
            this.cosmetics.play(this.cosmetics.wheelSlot(slot));
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.hovered = this.slotAt(mouseX, mouseY);

        if (this.hovered != this.lastHovered)
        {
            this.lastHovered = this.hovered;
            this.hoverStart = Minecraft.getSystemTime();
        }

        float open = Math.min(1.0F, (float)(Minecraft.getSystemTime() - this.opened) / 150.0F);
        drawRect(0, 0, this.width, this.height, SythixTheme.withAlpha(0xFF000000, (int)(110 * open)));
        float cx = (float)this.width / 2.0F;
        float cy = (float)this.height / 2.0F;
        float r = this.radius() * (0.85F + 0.15F * open);

        // the ring
        SythixTheme.drawRounded(cx - r - 24.0F, cy - r - 24.0F, cx + r + 24.0F, cy + r + 24.0F, r + 24.0F, SythixTheme.withAlpha(0xFF0B0B0E, (int)(150 * open)));
        SythixTheme.drawRoundedOutline(cx - r - 24.0F, cy - r - 24.0F, cx + r + 24.0F, cy + r + 24.0F, r + 24.0F, 1.0F, SythixTheme.withAlpha(SythixTheme.RED, (int)(160 * open)));
        SythixTheme.drawRounded(cx - r + 30.0F, cy - r + 30.0F, cx + r - 30.0F, cy + r - 30.0F, r - 30.0F, SythixTheme.withAlpha(0xFF16161B, (int)(200 * open)));

        // you, doing the pointed-at emote
        PlayerPreview.Look look = PlayerPreview.Look.current(this.cosmetics);

        if (this.hovered >= 0)
        {
            look.emote = this.cosmetics.wheelSlot(this.hovered);
            look.emoteStart = this.hoverStart;
        }

        float scale = (r - 34.0F) / 1.35F;

        try
        {
            PlayerPreview.draw((int)cx, (int)(cy - scale * 0.95F), scale, 200.0F, look);
        }
        catch (Throwable t)
        {
            net.sythix.client.core.ModuleErrors.report(this.cosmetics, "emote wheel preview", t);
        }

        for (int i = 0; i < 8; ++i)
        {
            float a = (float)i * (float)Math.PI / 4.0F;
            float sx = cx + MathHelper.sin(a) * r;
            float sy = cy - MathHelper.cos(a) * r;
            String name = this.cosmetics.wheelSlot(i);
            int w = Math.max(56, this.fontRenderer.getStringWidth(name) + 16);
            boolean hover = i == this.hovered;
            float grow = hover ? 1.08F : 1.0F;
            float hw = (float)w / 2.0F * grow;
            float hh = 11.0F * grow;
            SythixTheme.drawRounded(sx - hw, sy - hh, sx + hw, sy + hh, 6.0F, hover ? SythixTheme.withAlpha(SythixTheme.RED, 230) : SythixTheme.withAlpha(0xFF22222A, 235));
            SythixTheme.drawRoundedOutline(sx - hw, sy - hh, sx + hw, sy + hh, 6.0F, 1.0F, hover ? 0xFFFF7A3D : SythixTheme.withAlpha(0xFFFFFFFF, 40));
            this.fontRenderer.drawString(name, sx - (float)this.fontRenderer.getStringWidth(name) / 2.0F, sy - 3.5F, 0xFFFFFFFF, hover);
            String number = String.valueOf(i + 1);
            this.fontRenderer.drawString(number, sx - hw + 4.0F, sy - hh + 2.0F, SythixTheme.withAlpha(0xFFFFFFFF, 110), false);
        }

        String hint = this.held ? "Point at an emote and let go" : "Click an emote, or press 1-8";
        this.fontRenderer.drawString(hint, cx - (float)this.fontRenderer.getStringWidth(hint) / 2.0F, Math.min(cy + r + 30.0F, (float)this.height - 12.0F), SythixTheme.TEXT_MUTED, true);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        int slot = this.slotAt(mouseX, mouseY);

        if (mouseButton == 0)
        {
            this.choose(slot);
        }
        else
        {
            this.mc.displayGuiScreen(null);
        }
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (keyCode >= Keyboard.KEY_1 && keyCode <= Keyboard.KEY_8)
        {
            this.choose(keyCode - Keyboard.KEY_1);
            return;
        }

        if (keyCode == 1 || (!this.held && keyCode == this.cosmetics.wheelKey()))
        {
            this.mc.displayGuiScreen(null);
        }
    }

    public boolean doesGuiPauseGame()
    {
        return false;
    }
}
