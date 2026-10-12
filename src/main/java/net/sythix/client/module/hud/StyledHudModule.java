package net.sythix.client.module.hud;

import net.minecraft.client.gui.FontRenderer;
import net.sythix.client.module.Category;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ColorSetting;
import net.sythix.client.setting.NumberSetting;
import net.sythix.client.ui.SythixTheme;

/**
 * A HUD element with the shared Sythix look: optional background box, accent line, padding and
 * label/value colours.
 */
public abstract class StyledHudModule extends HudModule
{
    protected final ColorSetting labelColor;
    protected final ColorSetting valueColor;
    protected final BooleanSetting textShadow;
    protected final BooleanSetting background;
    protected final ColorSetting backgroundColor;
    protected final BooleanSetting accentLine;
    protected final NumberSetting padding;

    protected StyledHudModule(String name, String description, Category category, boolean defaultEnabled, double defaultX, double defaultY)
    {
        super(name, description, category, defaultEnabled, defaultX, defaultY);
        this.labelColor = this.add(new ColorSetting("Label Color", "Colour of labels and brackets.", SythixTheme.RED | 0xFF000000, false));
        this.valueColor = this.add(new ColorSetting("Value Color", "Colour of values.", 0xFFFFFFFF, false));
        this.textShadow = this.add(new BooleanSetting("Text Shadow", "Draw a shadow behind text.", true));
        this.background = this.add(new BooleanSetting("Background", "Draw a box behind the element.", true));
        this.backgroundColor = this.add(new ColorSetting("Background Color", "Colour and transparency of the box.", 0xA016161B, true)).visibleWhen(this.background::isOn);
        this.accentLine = this.add(new BooleanSetting("Accent Line", "Red-to-blue line along the bottom of the box.", true)).visibleWhen(this.background::isOn);
        this.padding = this.add(new NumberSetting("Padding", "Space between the content and the edge of the box.", 4.0D, 0.0D, 10.0D, 1.0D, "px")).visibleWhen(this.background::isOn);
    }

    protected StyledHudModule(String name, String description, boolean defaultEnabled, double defaultX, double defaultY)
    {
        this(name, description, Category.INTERFACE, defaultEnabled, defaultX, defaultY);
    }

    protected int getPad()
    {
        return this.background.isOn() ? this.padding.getInt() : 0;
    }

    protected void drawBox(int width, int height)
    {
        if (this.background.isOn() || net.sythix.client.module.client.Accessibility.highContrast())
        {
            int color = net.sythix.client.module.client.Accessibility.highContrast() ? 0xE8000000 : this.backgroundColor.getColor();
            SythixTheme.drawRect(0.0F, 0.0F, (float)width, (float)height, color);

            if (this.accentLine.isOn())
            {
                SythixTheme.drawAccentLine(0.0F, (float)(height - 1), (float)width, 1.0F);
            }
        }
    }

    /** Draws an item icon (and its durability bar / count) at x, y. */
    protected void drawItem(net.minecraft.item.ItemStack stack, int x, int y, boolean overlay)
    {
        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
        net.minecraft.client.renderer.GlStateManager.enableDepth();
        this.mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);

        if (overlay)
        {
            this.mc.getRenderItem().renderItemOverlayIntoGUI(this.mc.fontRenderer, stack, x, y, "");
        }

        net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
        net.minecraft.client.renderer.GlStateManager.disableDepth();
        net.minecraft.client.renderer.GlStateManager.enableBlend();
        net.minecraft.client.renderer.GlStateManager.popMatrix();
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Green at full durability through yellow to red when nearly broken. */
    protected static int durabilityColor(float fraction)
    {
        return 0xFF000000 | net.sythix.client.ui.ColorUtil.hsbToRgb(Math.max(0.0F, Math.min(1.0F, fraction)) / 3.0F, 0.85F, 1.0F);
    }

    protected void drawText(String text, float x, float y, int color)
    {
        FontRenderer font = this.mc.fontRenderer;
        font.drawString(text, x, y, color | 0xFF000000, this.textShadow.isOn() || net.sythix.client.module.client.Accessibility.highContrast());
    }
}
