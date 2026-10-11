package net.sythix.client.module.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.sythix.client.cosmetic.GuiProfile;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.ColorSetting;
import net.sythix.client.setting.KeybindSetting;
import net.sythix.client.setting.NumberSetting;

/**
 * #38 Capes, wings and emotes for your own player. Cosmetics are drawn only on your screen;
 * other players do not see them.
 */
public class CosmeticsModule extends Module
{
    public static final String[] EMOTES = new String[] {"Wave", "Point", "T-Pose", "Dab", "Cheer"};

    private final BooleanSetting showCape;
    private final BooleanSetting wings;
    private final ColorSetting wingColor;
    private final NumberSetting wingFlap;
    private final NumberSetting wingSize;
    private final KeybindSetting[] emoteKeys = new KeybindSetting[EMOTES.length];
    private final NumberSetting emoteLength;
    private String emote;
    private long emoteStart;

    public CosmeticsModule()
    {
        super("Cosmetics", "Capes, wings and emotes for your player (visible on your screen).", Category.VISUALS, true);
        this.add(new ButtonSetting("Profile", "Change your name, skin and cape with a live preview.", "Open", () -> this.mc.displayGuiScreen(new GuiProfile(this.mc.currentScreen))));
        this.showCape = this.add(new BooleanSetting("Show Cape", "Show the cape chosen in your profile.", true));
        this.wings = this.add(new BooleanSetting("Wings", "Show wings on your back.", false));
        this.wingColor = this.add(new ColorSetting("Wing Color", "Colour of the wings.", 0xE0D81820, true)).visibleWhen(this.wings::isOn);
        this.wingFlap = this.add(new NumberSetting("Wing Flap Speed", "How fast the wings move.", 1.0D, 0.0D, 3.0D, 0.1D, "x")).visibleWhen(this.wings::isOn);
        this.wingSize = this.add(new NumberSetting("Wing Size", "Size of the wings.", 1.0D, 0.5D, 1.5D, 0.05D, "x")).visibleWhen(this.wings::isOn);

        for (int i = 0; i < EMOTES.length; ++i)
        {
            this.emoteKeys[i] = this.add(new KeybindSetting(EMOTES[i] + " Emote Key", "Plays the " + EMOTES[i] + " emote (see it in third person, F5).", 0));
        }

        this.emoteLength = this.add(new NumberSetting("Emote Length", "How long an emote plays.", 3.0D, 1.0D, 10.0D, 0.5D, "s"));
    }

    public GuiScreen createScreen(GuiScreen parent)
    {
        return new GuiProfile(parent);
    }

    public boolean showCape()
    {
        return this.showCape.isOn();
    }

    public boolean showWings()
    {
        return this.wings.isOn();
    }

    public int wingColor()
    {
        return this.wingColor.getColor();
    }

    public float wingFlap()
    {
        return this.wingFlap.getFloat();
    }

    public float wingSize()
    {
        return this.wingSize.getFloat();
    }

    public void onKey(int keyCode)
    {
        for (int i = 0; i < EMOTES.length; ++i)
        {
            if (keyCode != 0 && keyCode == this.emoteKeys[i].getKey())
            {
                this.play(EMOTES[i]);
            }
        }
    }

    public void play(String name)
    {
        this.emote = name;
        this.emoteStart = Minecraft.getSystemTime();
    }

    public String currentEmote()
    {
        if (this.emote != null && Minecraft.getSystemTime() - this.emoteStart > (long)(this.emoteLength.getFloat() * 1000.0F))
        {
            this.emote = null;
        }

        return this.emote;
    }

    /** Overrides arm angles while an emote plays. */
    public static void pose(ModelBiped model, String emote, float ageInTicks)
    {
        float t = ageInTicks * 0.3F;

        switch (emote)
        {
            case "Wave":
                model.bipedRightArm.rotateAngleX = (float)Math.PI;
                model.bipedRightArm.rotateAngleZ = 0.4F + MathHelper.sin(t) * 0.4F;
                break;
            case "Point":
                model.bipedRightArm.rotateAngleX = -(float)Math.PI / 2.0F;
                model.bipedRightArm.rotateAngleY = -0.1F;
                break;
            case "T-Pose":
                model.bipedRightArm.rotateAngleX = 0.0F;
                model.bipedLeftArm.rotateAngleX = 0.0F;
                model.bipedRightArm.rotateAngleZ = (float)Math.PI / 2.0F;
                model.bipedLeftArm.rotateAngleZ = -(float)Math.PI / 2.0F;
                break;
            case "Dab":
                model.bipedRightArm.rotateAngleX = -2.3F;
                model.bipedRightArm.rotateAngleZ = -0.6F;
                model.bipedLeftArm.rotateAngleX = -1.4F;
                model.bipedLeftArm.rotateAngleY = 0.8F;
                model.bipedHead.rotateAngleX = 0.6F;
                model.bipedHead.rotateAngleY = -0.5F;
                break;
            default:
                model.bipedRightArm.rotateAngleX = (float)Math.PI + MathHelper.sin(t) * 0.3F;
                model.bipedLeftArm.rotateAngleX = (float)Math.PI - MathHelper.sin(t) * 0.3F;
                model.bipedRightArm.rotateAngleZ = 0.3F;
                model.bipedLeftArm.rotateAngleZ = -0.3F;
                break;
        }
    }

    /** Applies the active emote if the model belongs to the local player. */
    public void applyEmote(ModelBiped model, Entity entity, float ageInTicks)
    {
        String e = this.currentEmote();

        if (e != null && entity == this.mc.player)
        {
            pose(model, e, ageInTicks);
        }
    }
}
