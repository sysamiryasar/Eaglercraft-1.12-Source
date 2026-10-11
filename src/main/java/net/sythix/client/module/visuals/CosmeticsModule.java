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
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;
import net.sythix.client.cosmetic.BuddyRenderer;
import net.sythix.client.cosmetic.HatRenderer;
import net.sythix.client.cosmetic.WingRenderer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.EnumParticleTypes;

/**
 * #38 Capes, wings and emotes for your own player. Cosmetics are drawn only on your screen;
 * other players do not see them.
 */
public class CosmeticsModule extends Module
{
    public static final String[] EMOTES = new String[] {"Wave", "Point", "T-Pose", "Dab", "Cheer", "Clap", "Facepalm"};

    private final BooleanSetting showCape;
    private final BooleanSetting wings;
    private final ColorSetting wingColor;
    private final NumberSetting wingFlap;
    private final NumberSetting wingSize;
    private final ModeSetting wingStyle;
    private final ModeSetting hat;
    private final ColorSetting hatColor;
    private final ModeSetting buddy;
    private final ColorSetting buddyColor;
    private final ModeSetting aura;
    private final NumberSetting auraAmount;
    private final ColorSetting auraColor;
    private final BooleanSetting auraTrail;
    private final BooleanSetting auraFirstPerson;
    private final KeybindSetting[] emoteKeys = new KeybindSetting[EMOTES.length];
    private final NumberSetting emoteLength;
    private String emote;
    private long emoteStart;

    public CosmeticsModule()
    {
        super("Cosmetics", "Capes, wings, hats, buddies, auras and emotes for your player (visible on your screen).", Category.VISUALS, true);
        this.add(new ButtonSetting("Profile", "Change your name, skin and cape with a live preview.", "Open", () -> this.mc.displayGuiScreen(new GuiProfile(this.mc.currentScreen))));
        this.showCape = this.add(new BooleanSetting("Show Cape", "Show the cape chosen in your profile.", true));
        this.wings = this.add(new BooleanSetting("Wings", "Show wings on your back.", false));
        this.wingColor = this.add(new ColorSetting("Wing Color", "Colour of the wings.", 0xE0D81820, true)).visibleWhen(this.wings::isOn);
        this.wingFlap = this.add(new NumberSetting("Wing Flap Speed", "How fast the wings move.", 1.0D, 0.0D, 3.0D, 0.1D, "x")).visibleWhen(this.wings::isOn);
        this.wingSize = this.add(new NumberSetting("Wing Size", "Size of the wings.", 1.0D, 0.5D, 1.5D, 0.05D, "x")).visibleWhen(this.wings::isOn);
        this.wingStyle = this.add(new ModeSetting("Wing Style", "Shape of the wings.", "Feathered", WingRenderer.STYLES)).visibleWhen(this.wings::isOn);
        this.hat = this.add(new ModeSetting("Hat", "Something to wear on your head.", "None", HatRenderer.HATS));
        this.hatColor = this.add(new ColorSetting("Hat Color", "Main colour of the hat.", 0xFF1A1A20, false)).visibleWhen(() -> !this.hat.is("None"));
        this.buddy = this.add(new ModeSetting("Shoulder Buddy", "A little companion on your shoulder.", "None", BuddyRenderer.BUDDIES));
        this.buddyColor = this.add(new ColorSetting("Buddy Color", "Colour of your buddy.", 0xFF55D06A, false)).visibleWhen(() -> !this.buddy.is("None"));
        this.aura = this.add(new ModeSetting("Aura", "Particles around you (only you see them).", "None", AURAS));
        this.auraAmount = this.add(new NumberSetting("Aura Amount", "How many particles.", 3.0D, 1.0D, 10.0D, 1.0D)).visibleWhen(() -> !this.aura.is("None"));
        this.auraColor = this.add(new ColorSetting("Aura Color", "Colour of the Dust aura.", 0xFFD81820, false)).visibleWhen(() -> this.aura.is("Dust"));
        this.auraTrail = this.add(new BooleanSetting("Trail Only", "Only spawn particles while you move (a trail).", false)).visibleWhen(() -> !this.aura.is("None"));
        this.auraFirstPerson = this.add(new BooleanSetting("Aura In First Person", "Also spawn the aura in first person (it can get in your view).", false)).visibleWhen(() -> !this.aura.is("None"));

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

    public static final String[] AURAS = new String[] {"None", "Hearts", "Flames", "Notes", "Sparkles", "Dust", "Portal", "Snow", "Clouds"};

    public String wingStyle()
    {
        return this.wingStyle.get();
    }

    public String hat()
    {
        return this.hat.get();
    }

    public int hatColor()
    {
        return this.hatColor.getColor();
    }

    public String buddy()
    {
        return this.buddy.get();
    }

    public int buddyColor()
    {
        return this.buddyColor.getColor();
    }

    public void onTick()
    {
        EntityPlayerSP p = this.mc.player;

        if (this.aura.is("None") || p == null || this.mc.world == null || p.isInvisible() || this.mc.isGamePaused())
        {
            return;
        }

        if (this.mc.gameSettings.thirdPersonView == 0 && !this.auraFirstPerson.isOn())
        {
            return;
        }

        boolean moving = Math.abs(p.posX - p.prevPosX) + Math.abs(p.posZ - p.prevPosZ) > 0.02D;

        if (this.auraTrail.isOn() && !moving)
        {
            return;
        }

        java.util.Random rand = p.getRNG();
        int count = this.auraAmount.getInt();

        // Hearts and notes are big, so spawn them less often
        if ((this.aura.is("Hearts") || this.aura.is("Notes")) && p.ticksExisted % 4 != 0)
        {
            return;
        }

        for (int i = 0; i < (this.aura.is("Hearts") || this.aura.is("Notes") ? 1 : count); ++i)
        {
            double x = p.posX + (rand.nextDouble() - 0.5D) * (this.auraTrail.isOn() ? 0.4D : 1.2D);
            double y = p.posY + (this.auraTrail.isOn() ? 0.1D : rand.nextDouble() * 2.0D);
            double z = p.posZ + (rand.nextDouble() - 0.5D) * (this.auraTrail.isOn() ? 0.4D : 1.2D);
            this.spawn(x, y, z, rand);
        }
    }

    private void spawn(double x, double y, double z, java.util.Random rand)
    {
        EnumParticleTypes type;
        double a = 0.0D;
        double b = 0.0D;
        double c = 0.0D;

        switch (this.aura.get())
        {
            case "Hearts":
                type = EnumParticleTypes.HEART;
                break;
            case "Flames":
                type = EnumParticleTypes.FLAME;
                b = 0.01D;
                break;
            case "Notes":
                type = EnumParticleTypes.NOTE;
                a = (double)rand.nextInt(25) / 24.0D;
                break;
            case "Sparkles":
                type = EnumParticleTypes.FIREWORKS_SPARK;
                a = (rand.nextDouble() - 0.5D) * 0.02D;
                b = 0.02D;
                c = (rand.nextDouble() - 0.5D) * 0.02D;
                break;
            case "Dust":
                type = EnumParticleTypes.REDSTONE;
                int col = this.auraColor.getArgb();
                // Redstone dust uses the motion as colour; 0 red means "default red", so nudge it
                a = Math.max(0.001D, (double)((col >> 16) & 255) / 255.0D);
                b = (double)((col >> 8) & 255) / 255.0D;
                c = (double)(col & 255) / 255.0D;
                break;
            case "Portal":
                type = EnumParticleTypes.PORTAL;
                a = (rand.nextDouble() - 0.5D) * 0.5D;
                b = -rand.nextDouble() * 0.5D;
                c = (rand.nextDouble() - 0.5D) * 0.5D;
                break;
            case "Snow":
                type = EnumParticleTypes.SNOW_SHOVEL;
                break;
            default:
                type = EnumParticleTypes.CLOUD;
                b = 0.01D;
        }

        this.mc.world.spawnParticle(type, x, y, z, a, b, c);
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
            case "Clap":
                model.bipedRightArm.rotateAngleX = -1.2F;
                model.bipedLeftArm.rotateAngleX = -1.2F;
                model.bipedRightArm.rotateAngleY = -0.35F - MathHelper.abs(MathHelper.sin(t * 1.5F)) * 0.35F;
                model.bipedLeftArm.rotateAngleY = 0.35F + MathHelper.abs(MathHelper.sin(t * 1.5F)) * 0.35F;
                break;
            case "Facepalm":
                model.bipedRightArm.rotateAngleX = -2.4F;
                model.bipedRightArm.rotateAngleY = -0.55F;
                model.bipedHead.rotateAngleX = 0.45F;
                model.bipedHead.rotateAngleY = MathHelper.sin(t * 0.7F) * 0.25F;
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
