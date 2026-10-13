package net.sythix.client.module.performance;

import net.minecraft.client.settings.GameSettings;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.GameOptionSetting;
import net.sythix.client.setting.InfoSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;
import net.sythix.client.ui.SythixTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

/**
 * #2 Water, lava, fire, terrain, item and entity animations, and interface animations
 * (menu transitions, smooth hotbar, smooth chat).
 */
public class AnimationController extends Module
{
    private final BooleanSetting itemSpin;
    private final BooleanSetting itemBob;
    private final ModeSetting menuAnimation;
    private final NumberSetting menuSpeed;
    private final BooleanSetting smoothHotbar;
    private final NumberSetting hotbarSpeed;
    private final BooleanSetting smoothChat;

    // Animation state
    private Object lastScreen;
    private long screenOpened;
    private float hotbarX = -1.0F;
    private long hotbarTime;
    private long chatLineTime;
    private int chatLinesAdded;

    public AnimationController()
    {
        super("Animation Controller", "Water, lava, fire, terrain and item animations, plus menu, hotbar and chat animations.", Category.PERFORMANCE, true);
        this.add(new GameOptionSetting("Animated water texture.", GameSettings.Options.ANIMATED_WATER));
        this.add(new GameOptionSetting("Animated lava texture.", GameSettings.Options.ANIMATED_LAVA));
        this.add(new GameOptionSetting("Animated fire texture.", GameSettings.Options.ANIMATED_FIRE));
        this.add(new GameOptionSetting("Animated nether portal texture.", GameSettings.Options.ANIMATED_PORTAL));
        this.add(new GameOptionSetting("Animated terrain textures (all other animated blocks).", GameSettings.Options.ANIMATED_TERRAIN));
        this.add(new GameOptionSetting("All animated textures, including items.", GameSettings.Options.ANIMATED_TEXTURES));
        this.add(new GameOptionSetting("Redstone dust particles.", GameSettings.Options.ANIMATED_REDSTONE));
        this.add(new GameOptionSetting("Explosion animation.", GameSettings.Options.ANIMATED_EXPLOSION));
        this.add(new GameOptionSetting("Torch and furnace flames.", GameSettings.Options.ANIMATED_FLAME));
        this.add(new GameOptionSetting("Smoke particles.", GameSettings.Options.ANIMATED_SMOKE));
        this.itemSpin = this.add(new BooleanSetting("Dropped Item Spin", "Dropped items rotate on the ground.", true));
        this.itemBob = this.add(new BooleanSetting("Dropped Item Bob", "Dropped items float up and down.", true));
        this.menuAnimation = this.add(new ModeSetting("Menu Animation", "How menus appear when they open.", "Zoom", "Off", "Fade", "Slide", "Zoom"));
        this.menuSpeed = this.add(new NumberSetting("Menu Animation Time", "How long the menu animation takes.", 220.0D, 80.0D, 600.0D, 20.0D, " ms")).visibleWhen(() -> !this.menuAnimation.is("Off"));
        this.smoothHotbar = this.add(new BooleanSetting("Smooth Hotbar", "The hotbar selection slides to the new slot.", true));
        this.hotbarSpeed = this.add(new NumberSetting("Hotbar Speed", "How fast the selection slides.", 12.0D, 4.0D, 30.0D, 1.0D, "x")).visibleWhen(this.smoothHotbar::isOn);
        this.smoothChat = this.add(new BooleanSetting("Smooth Chat", "New chat messages slide in from below.", true));
        this.add(new InfoSetting("Reduced Motion", "Accessibility > Reduced Motion turns these off.", () -> SythixTheme.reducedMotion ? "On - interface animations off" : "Off"));
    }

    private boolean motion()
    {
        return !SythixTheme.reducedMotion;
    }

    /** Called just before a menu is drawn; returns true if a transform was pushed. */
    public boolean beginScreen(GuiScreen screen, int width, int height)
    {
        if (screen != this.lastScreen)
        {
            // Only animate a fresh menu, not chat or the HUD editor
            this.lastScreen = screen;
            this.screenOpened = Minecraft.getSystemTime();
        }

        if (!this.motion() || this.menuAnimation.is("Off") || screen instanceof net.minecraft.client.gui.GuiChat || screen instanceof net.sythix.client.ui.GuiHudEditor
            || (screen instanceof net.minecraft.client.gui.GuiMainMenu && net.minecraft.client.gui.GuiMainMenu.introRunning()))
        {
            return false;
        }

        float t = this.screenProgress();

        if (t >= 1.0F)
        {
            return false;
        }

        float e = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
        GlStateManager.pushMatrix();

        if (this.menuAnimation.is("Slide"))
        {
            GlStateManager.translate(0.0F, (1.0F - e) * 24.0F, 0.0F);
        }
        else if (this.menuAnimation.is("Zoom"))
        {
            float scale = 0.9F + 0.1F * e;
            GlStateManager.translate((float)width / 2.0F, (float)height / 2.0F, 0.0F);
            GlStateManager.scale(scale, scale, 1.0F);
            GlStateManager.translate((float)(-width) / 2.0F, (float)(-height) / 2.0F, 0.0F);
        }

        return true;
    }

    /** After the menu is drawn: pops the transform and draws the fade overlay. */
    public void endScreen(boolean pushed, int width, int height)
    {
        if (pushed)
        {
            GlStateManager.popMatrix();
        }

        if (pushed)
        {
            float t = this.screenProgress();
            int alpha = (int)((1.0F - t) * 200.0F);

            if (alpha > 4)
            {
                GlStateManager.disableDepth();
                SythixTheme.drawRect(0.0F, 0.0F, (float)width, (float)height, SythixTheme.withAlpha(SythixTheme.BLACK, alpha));
                GlStateManager.enableDepth();
            }
        }
    }

    private float screenProgress()
    {
        return Math.min(1.0F, (float)(Minecraft.getSystemTime() - this.screenOpened) / (float)Math.max(1, this.menuSpeed.getInt()));
    }

    /** X offset of the hotbar selection box (vanilla: slot * 20). */
    public int hotbarX(int target)
    {
        long now = Minecraft.getSystemTime();
        float seconds = this.hotbarTime == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.hotbarTime) / 1000.0F);
        this.hotbarTime = now;

        if (!this.smoothHotbar.isOn() || !this.motion() || this.hotbarX < 0.0F)
        {
            this.hotbarX = (float)target;
            return target;
        }

        // Ease: move a share of the remaining distance each frame
        float k = Math.min(1.0F, seconds * this.hotbarSpeed.getFloat());
        this.hotbarX += ((float)target - this.hotbarX) * k;

        if (Math.abs((float)target - this.hotbarX) < 0.5F)
        {
            this.hotbarX = (float)target;
        }

        return Math.round(this.hotbarX);
    }

    public void onChatLine(int lines)
    {
        long now = Minecraft.getSystemTime();
        // Several lines arriving together slide as one block
        this.chatLinesAdded = now - this.chatLineTime < 150L ? this.chatLinesAdded + lines : lines;
        this.chatLineTime = now;
    }

    /** Extra downward offset for the chat while new lines slide in. */
    public float chatOffset(float lineHeight)
    {
        if (!this.smoothChat.isOn() || !this.motion())
        {
            return 0.0F;
        }

        float t = Math.min(1.0F, (float)(Minecraft.getSystemTime() - this.chatLineTime) / 180.0F);
        return t >= 1.0F ? 0.0F : (1.0F - t) * (1.0F - t) * lineHeight * (float)Math.min(3, this.chatLinesAdded);
    }

    public boolean itemSpin()
    {
        return this.itemSpin.isOn();
    }

    public boolean itemBob()
    {
        return this.itemBob.isOn();
    }
}
