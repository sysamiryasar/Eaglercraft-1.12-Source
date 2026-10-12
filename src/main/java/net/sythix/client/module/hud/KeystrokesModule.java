package net.sythix.client.module.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.sythix.client.SythixClient;
import net.sythix.client.input.InputUtil;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ColorSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;
import net.sythix.client.ui.SythixTheme;

/**
 * Shows movement keys, mouse buttons and the space bar lighting up as you press them.
 */
public class KeystrokesModule extends HudModule
{
    private static final int FORWARD = 0;
    private static final int LEFT = 1;
    private static final int BACK = 2;
    private static final int RIGHT = 3;
    private static final int ATTACK = 4;
    private static final int USE = 5;
    private static final int JUMP = 6;

    private final NumberSetting keySize;
    private final NumberSetting gap;
    private final ModeSetting keyNames;
    private final BooleanSetting showMovement;
    private final BooleanSetting showMouse;
    private final BooleanSetting showCps;
    private final BooleanSetting showSpace;
    private final ModeSetting spaceStyle;
    private final ColorSetting backgroundColor;
    private final ColorSetting pressedColor;
    private final ColorSetting textColor;
    private final ColorSetting pressedTextColor;
    private final BooleanSetting outline;
    private final ColorSetting outlineColor;
    private final BooleanSetting textShadow;
    private final NumberSetting fadeTime;

    /** Animated press amount (0..1) for each key. */
    private final float[] pressAnimation = new float[7];
    private long lastFrameTime;

    public KeystrokesModule()
    {
        super("Keystrokes", "Shows WASD, mouse buttons and space bar presses.", net.sythix.client.module.Category.PVP, true, 0.005D, 0.34D);
        this.keySize = this.add(new NumberSetting("Key Size", "Width and height of each key.", 22.0D, 14.0D, 40.0D, 1.0D, "px"));
        this.gap = this.add(new NumberSetting("Gap", "Space between keys.", 2.0D, 0.0D, 6.0D, 1.0D, "px"));
        this.showMovement = this.add(new BooleanSetting("Movement Keys", "Show the W A S D keys.", true));
        this.keyNames = this.add(new ModeSetting("Key Names", "Show the keys you actually use, or always W A S D.", "Bound Keys", "Bound Keys", "WASD"));
        this.showMouse = this.add(new BooleanSetting("Mouse Buttons", "Show left and right mouse buttons.", true));
        this.showCps = this.add(new BooleanSetting("Show CPS", "Show clicks per second on the mouse buttons.", true)).visibleWhen(this.showMouse::isOn);
        this.showSpace = this.add(new BooleanSetting("Space Bar", "Show the jump key.", true));
        this.spaceStyle = this.add(new ModeSetting("Space Style", "Draw the space bar as a line or as text.", "Line", "Line", "Text")).visibleWhen(this.showSpace::isOn);
        this.backgroundColor = this.add(new ColorSetting("Key Color", "Colour of keys that are not pressed.", 0xA016161B, true));
        this.pressedColor = this.add(new ColorSetting("Pressed Color", "Colour of keys while held.", 0xE0D81820, true));
        this.textColor = this.add(new ColorSetting("Text Color", "Text colour of keys that are not pressed.", 0xFFFFFFFF, false));
        this.pressedTextColor = this.add(new ColorSetting("Pressed Text", "Text colour of keys while held.", 0xFFFFFFFF, false));
        this.outline = this.add(new BooleanSetting("Outline", "Draw a thin border around each key.", false));
        this.outlineColor = this.add(new ColorSetting("Outline Color", "Colour of the key border.", 0xFF2F6BFF, true)).visibleWhen(this.outline::isOn);
        this.textShadow = this.add(new BooleanSetting("Text Shadow", "Draw a shadow behind key labels.", true));
        this.fadeTime = this.add(new NumberSetting("Fade Time", "How long the press animation takes. 0 = instant.", 100.0D, 0.0D, 400.0D, 10.0D, "ms"));
    }

    private int size()
    {
        return this.keySize.getInt();
    }

    private int gapPx()
    {
        return this.gap.getInt();
    }

    private int spaceHeight()
    {
        return Math.max(8, Math.round((float)this.size() * 0.55F));
    }

    public int getWidth()
    {
        return this.size() * 3 + this.gapPx() * 2;
    }

    public int getHeight()
    {
        int rows = (this.showMovement.isOn() ? 2 : 0) + (this.showMouse.isOn() ? 1 : 0);
        int height = Math.max(0, rows * this.size() + (rows - 1) * this.gapPx());

        if (this.showSpace.isOn())
        {
            height += (height > 0 ? this.gapPx() : 0) + this.spaceHeight();
        }

        return Math.max(8, height);
    }

    private void updateAnimations()
    {
        GameSettings settings = this.mc.gameSettings;
        KeyBinding[] bindings = new KeyBinding[] {settings.keyBindForward, settings.keyBindLeft, settings.keyBindBack, settings.keyBindRight, settings.keyBindAttack, settings.keyBindUseItem, settings.keyBindJump};
        long now = Minecraft.getSystemTime();
        float seconds = this.lastFrameTime == 0L ? 1.0F : Math.min(1.0F, (float)(now - this.lastFrameTime) / 1000.0F);
        this.lastFrameTime = now;
        float fade = this.fadeTime.getFloat();
        float speed = fade <= 0.0F ? 1000.0F : 1000.0F / fade;

        for (int i = 0; i < bindings.length; ++i)
        {
            this.pressAnimation[i] = SythixTheme.approach(this.pressAnimation[i], InputUtil.isDown(bindings[i]) ? 1.0F : 0.0F, speed, seconds);
        }
    }

    private String keyLabel(KeyBinding binding, String fallback)
    {
        if (this.keyNames.is("WASD"))
        {
            return fallback;
        }

        String name = GameSettings.getKeyDisplayString(binding.getKeyCode());
        return name.length() > 6 ? name.substring(0, 6) : name;
    }

    protected void renderContent(boolean preview)
    {
        this.updateAnimations();
        GameSettings settings = this.mc.gameSettings;
        int s = this.size();
        int g = this.gapPx();
        int row = s + g;

        int y = 0;

        if (this.showMovement.isOn())
        {
            this.drawKey(FORWARD, s + g, 0, s, s, this.keyLabel(settings.keyBindForward, "W"), null);
            this.drawKey(LEFT, 0, row, s, s, this.keyLabel(settings.keyBindLeft, "A"), null);
            this.drawKey(BACK, row, row, s, s, this.keyLabel(settings.keyBindBack, "S"), null);
            this.drawKey(RIGHT, row * 2, row, s, s, this.keyLabel(settings.keyBindRight, "D"), null);
            y = row * 2;
        }

        if (this.showMouse.isOn())
        {
            int half = (this.getWidth() - g) / 2;
            boolean cps = this.showCps.isOn();
            String leftCps = cps ? SythixClient.get().getClickCounter().getLeftCps() + " CPS" : null;
            String rightCps = cps ? SythixClient.get().getClickCounter().getRightCps() + " CPS" : null;
            this.drawKey(ATTACK, 0, y, half, s, "LMB", leftCps);
            this.drawKey(USE, this.getWidth() - half, y, half, s, "RMB", rightCps);
            y += row;
        }

        if (this.showSpace.isOn())
        {
            this.drawKey(JUMP, 0, y, this.getWidth(), this.spaceHeight(), this.spaceStyle.is("Text") ? this.keyLabel(settings.keyBindJump, "Space") : null, null);

            if (this.spaceStyle.is("Line"))
            {
                float p = this.pressAnimation[JUMP];
                int color = SythixTheme.lerpColor(this.textColor.getColor(), this.pressedTextColor.getColor(), p);
                float lineWidth = (float)this.getWidth() * 0.4F;
                float lineY = (float)y + (float)this.spaceHeight() / 2.0F - 0.5F;
                SythixTheme.drawRect(((float)this.getWidth() - lineWidth) / 2.0F, lineY, ((float)this.getWidth() + lineWidth) / 2.0F, lineY + 1.0F, color);
            }
        }
    }

    private void drawKey(int index, int x, int y, int width, int height, String label, String subLabel)
    {
        float p = this.pressAnimation[index];
        float offset = (float)x / (float)Math.max(1, this.getWidth()) * 0.3F;
        int fill = SythixTheme.lerpColor(this.backgroundColor.getColor(offset), this.pressedColor.getColor(offset), p);
        SythixTheme.drawRect((float)x, (float)y, (float)(x + width), (float)(y + height), fill);

        if (this.outline.isOn())
        {
            SythixTheme.drawOutline((float)x, (float)y, (float)(x + width), (float)(y + height), this.outlineColor.getColor(offset));
        }

        if (label == null)
        {
            return;
        }

        FontRenderer font = this.mc.fontRenderer;
        int textColor = SythixTheme.lerpColor(this.textColor.getColor(offset), this.pressedTextColor.getColor(offset), p) | 0xFF000000;
        int labelWidth = font.getStringWidth(label);
        float fit = Math.min(1.0F, (float)(width - 4) / (float)Math.max(1, labelWidth));
        float labelY = subLabel != null && height >= 18 ? (float)y + (float)height / 2.0F - 7.0F : (float)y + (float)(height - 8) / 2.0F;

        GlStateManager.pushMatrix();
        GlStateManager.translate((float)x + (float)width / 2.0F, labelY, 0.0F);
        GlStateManager.scale(fit, fit, 1.0F);
        font.drawString(label, (float)(-labelWidth) / 2.0F, 0.0F, textColor, this.textShadow.isOn());
        GlStateManager.popMatrix();

        if (subLabel != null && height >= 18)
        {
            int subWidth = font.getStringWidth(subLabel);
            GlStateManager.pushMatrix();
            GlStateManager.translate((float)x + (float)width / 2.0F, (float)y + (float)height / 2.0F + 2.0F, 0.0F);
            GlStateManager.scale(0.5F, 0.5F, 1.0F);
            font.drawString(subLabel, (float)(-subWidth) / 2.0F, 0.0F, textColor, this.textShadow.isOn());
            GlStateManager.popMatrix();
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
