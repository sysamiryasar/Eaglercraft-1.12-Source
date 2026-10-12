package net.sythix.client.module.visuals;

import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sythix.client.cosmetic.AuraRenderer;
import net.sythix.client.cosmetic.CosmeticCatalog;
import net.sythix.client.cosmetic.Emotes;
import net.sythix.client.cosmetic.ExtrasRenderer;
import net.sythix.client.cosmetic.GuiCosmetics;
import net.sythix.client.cosmetic.GuiEmoteWheel;
import net.sythix.client.cosmetic.GuiProfile;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.ColorSetting;
import net.sythix.client.setting.KeybindSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;
import net.sythix.client.setting.TextSetting;

/**
 * #38 Cosmetics and emotes for your own player: the 48 Sythix cosmetics (capes, headwear, wings,
 * auras and extras) and the emote wheel. Cosmetics are drawn only on your screen; other players
 * do not see them.
 */
public class CosmeticsModule extends Module
{
    private final BooleanSetting showCape;
    private final ModeSetting headwear;
    private final ModeSetting wings;
    private final NumberSetting wingFlap;
    private final NumberSetting wingSize;
    private final ModeSetting aura;
    private final NumberSetting auraAmount;
    private final BooleanSetting auraFirstPerson;
    private final BooleanSetting trails;
    private final ModeSetting trailColor;
    private final BooleanSetting runes;
    private final BooleanSetting companion;
    private final BooleanSetting victory;
    private final BooleanSetting afterimage;
    private final BooleanSetting spawnEntrance;
    private final BooleanSetting emblem;
    private final TextSetting emblemText;
    private final ColorSetting emblemColor;
    private final ModeSetting emblemStyle;
    private final KeybindSetting wheelKey;
    private final ModeSetting[] wheelSlots = new ModeSetting[8];
    private final NumberSetting emoteLength;
    private final BooleanSetting emoteCamera;
    private final BooleanSetting stopWhenMoving;
    private String emote;
    private long emoteStart;
    private int cameraBefore = -1;
    private EntityPlayerSP lastPlayer;
    private double runeX = Double.NaN;
    private double runeZ;
    private int runeCount;
    private boolean swinging;

    public CosmeticsModule()
    {
        super("Cosmetics", "Capes, headwear, wings, auras, extras and the emote wheel for your player (shown on your screen).", Category.VISUALS, true);
        this.add(new ButtonSetting("Wardrobe", "Pick cosmetics and emotes with a live preview.", "Open", () -> this.mc.displayGuiScreen(new GuiCosmetics(this.mc.currentScreen))));
        this.add(new ButtonSetting("Profile", "Change your name, skin and cape.", "Open", () -> this.mc.displayGuiScreen(new GuiProfile(this.mc.currentScreen))));
        this.showCape = this.add(new BooleanSetting("Show Cape", "Show the cape chosen in the wardrobe or profile.", true));
        this.headwear = this.add(new ModeSetting("Headwear", "Something to wear on your head.", CosmeticCatalog.NONE, CosmeticCatalog.choices(CosmeticCatalog.HEADWEAR)));
        this.wings = this.add(new ModeSetting("Wings", "Wings on your back.", CosmeticCatalog.NONE, CosmeticCatalog.choices(CosmeticCatalog.WINGS)));
        this.wingFlap = this.add(new NumberSetting("Wing Flap Speed", "How fast the wings move.", 1.0D, 0.0D, 3.0D, 0.1D, "x")).visibleWhen(() -> !this.wings.is(CosmeticCatalog.NONE));
        this.wingSize = this.add(new NumberSetting("Wing Size", "Size of the wings.", 1.0D, 0.5D, 1.5D, 0.05D, "x")).visibleWhen(() -> !this.wings.is(CosmeticCatalog.NONE));
        this.aura = this.add(new ModeSetting("Aura", "An effect all around you.", CosmeticCatalog.NONE, CosmeticCatalog.choices(CosmeticCatalog.AURAS)));
        this.auraAmount = this.add(new NumberSetting("Aura Particles", "How many particles the aura makes.", 3.0D, 0.0D, 10.0D, 1.0D)).visibleWhen(() -> !this.aura.is(CosmeticCatalog.NONE));
        this.auraFirstPerson = this.add(new BooleanSetting("Aura In First Person", "Also show the aura in first person (it can get in your view).", false)).visibleWhen(() -> !this.aura.is(CosmeticCatalog.NONE));
        this.trails = this.add(new BooleanSetting("Weapon Trails", "A trail of light when you swing.", false));
        this.trailColor = this.add(new ModeSetting("Trail Color", "Colour of the weapon trail.", "Purple", ExtrasRenderer.TRAIL_COLORS)).visibleWhen(this.trails::isOn);
        this.runes = this.add(new BooleanSetting("Footstep Runes", "Glowing rune circles under your feet as you walk.", false));
        this.companion = this.add(new BooleanSetting("Rift Companion", "A tiny floating crystal by your shoulder.", false));
        this.victory = this.add(new BooleanSetting("Victory Burst", "A burst of light when you win a duel.", false));
        this.afterimage = this.add(new BooleanSetting("Afterimage", "Fading silhouettes follow you while you move.", false));
        this.spawnEntrance = this.add(new BooleanSetting("Spawn Entrance", "A portal opens around you when you join a world.", false));
        this.emblem = this.add(new BooleanSetting("Signature Emblem", "An animated SYTHIX badge above your head (third person).", false));
        this.emblemText = this.add(new TextSetting("Emblem Text", "The text under the emblem.", "SYTHIX", 16)).visibleWhen(this.emblem::isOn);
        this.emblemColor = this.add(new ColorSetting("Emblem Color", "Colour of the emblem's glow and text.", 0xFFB050FF, false)).visibleWhen(this.emblem::isOn);
        this.emblemStyle = this.add(new ModeSetting("Emblem Style", "How the emblem moves.", "Spin", ExtrasRenderer.EMBLEM_STYLES)).visibleWhen(this.emblem::isOn);
        this.wheelKey = this.add(new KeybindSetting("Emote Wheel Key", "Hold to open the emote wheel, point at an emote and let go to play it.", 50));

        for (int i = 0; i < this.wheelSlots.length; ++i)
        {
            this.wheelSlots[i] = this.add(new ModeSetting("Wheel Slot " + (i + 1), "The emote in this spot of the emote wheel.", Emotes.WHEEL_DEFAULT[i], Emotes.ALL));
        }

        this.emoteLength = this.add(new NumberSetting("Emote Length", "How long an emote plays.", 4.0D, 1.0D, 10.0D, 0.5D, "s"));
        this.emoteCamera = this.add(new BooleanSetting("Emote Camera", "Switch to the front camera while an emote plays, then back.", true));
        this.stopWhenMoving = this.add(new BooleanSetting("Stop When Moving", "Moving, jumping or attacking ends the emote.", true));
        net.sythix.client.combat.CombatTracker.addListener(fight -> {
            if (this.isEnabled())
            {
                this.onFightEnded(fight.isWin());
            }
        });
    }

    public GuiScreen createScreen(GuiScreen parent)
    {
        return new GuiCosmetics(parent);
    }

    // ---- what is equipped ------------------------------------------------------------------------

    public boolean showCape()
    {
        return this.showCape.isOn();
    }

    public String headwear()
    {
        return this.headwear.get();
    }

    public String wings()
    {
        return this.wings.get();
    }

    public float wingFlap()
    {
        return this.wingFlap.getFloat();
    }

    public float wingSize()
    {
        return this.wingSize.getFloat();
    }

    public String aura()
    {
        return this.aura.get();
    }

    public boolean auraFirstPerson()
    {
        return this.auraFirstPerson.isOn();
    }

    public boolean companion()
    {
        return this.companion.isOn();
    }

    public boolean afterimage()
    {
        return this.afterimage.isOn();
    }

    public boolean emblem()
    {
        return this.emblem.isOn();
    }

    public String emblemText()
    {
        return this.emblemText.get();
    }

    public int emblemColor()
    {
        return this.emblemColor.getColor();
    }

    public String emblemStyle()
    {
        return this.emblemStyle.get();
    }

    /** The setting that equips a cosmetic of this category (null for capes and extras). */
    public ModeSetting slot(String category)
    {
        switch (category)
        {
            case CosmeticCatalog.HEADWEAR:
                return this.headwear;
            case CosmeticCatalog.WINGS:
                return this.wings;
            case CosmeticCatalog.AURAS:
                return this.aura;
            default:
                return null;
        }
    }

    /** The on/off setting for an extra, by its name. */
    public BooleanSetting extra(String name)
    {
        switch (name)
        {
            case "Weapon Trails":
                return this.trails;
            case "Footstep Runes":
                return this.runes;
            case "Rift Companion":
                return this.companion;
            case "Victory Burst":
                return this.victory;
            case "Afterimage":
                return this.afterimage;
            case "Spawn Entrance":
                return this.spawnEntrance;
            default:
                return this.emblem;
        }
    }

    /** Settings that belong to an extra (shown under it in the wardrobe). */
    public java.util.List<net.sythix.client.setting.Setting<?>> optionsFor(String name)
    {
        java.util.List<net.sythix.client.setting.Setting<?>> list = new java.util.ArrayList<net.sythix.client.setting.Setting<?>>();

        switch (name)
        {
            case "Weapon Trails":
                list.add(this.trailColor);
                break;
            case "Signature Emblem":
                list.add(this.emblemText);
                list.add(this.emblemColor);
                list.add(this.emblemStyle);
                break;
            case CosmeticCatalog.WINGS:
                list.add(this.wingFlap);
                list.add(this.wingSize);
                break;
            case CosmeticCatalog.AURAS:
                list.add(this.auraAmount);
                list.add(this.auraFirstPerson);
                break;
            case CosmeticCatalog.CAPES:
                list.add(this.showCape);
                break;
            default:
        }

        return list;
    }

    public java.util.List<net.sythix.client.setting.Setting<?>> emoteSettings()
    {
        java.util.List<net.sythix.client.setting.Setting<?>> list = new java.util.ArrayList<net.sythix.client.setting.Setting<?>>();
        list.add(this.wheelKey);
        list.add(this.emoteLength);
        list.add(this.emoteCamera);
        list.add(this.stopWhenMoving);
        return list;
    }

    public String wheelSlot(int i)
    {
        return this.wheelSlots[i].get();
    }

    public void setWheelSlot(int i, String emote)
    {
        this.wheelSlots[i].set(emote);
    }

    public int wheelKey()
    {
        return this.wheelKey.getKey();
    }

    // ---- emotes -----------------------------------------------------------------------------------

    public void onKey(int keyCode)
    {
        if (keyCode != 0 && keyCode == this.wheelKey.getKey() && this.mc.currentScreen == null && this.mc.player != null)
        {
            this.mc.displayGuiScreen(new GuiEmoteWheel(this));
        }
    }

    public void play(String name)
    {
        boolean wasPlaying = this.emote != null;
        this.emote = name;
        this.emoteStart = Minecraft.getSystemTime();

        if (this.emoteCamera.isOn() && !wasPlaying && this.mc.world != null)
        {
            this.cameraBefore = this.mc.gameSettings.thirdPersonView;
            this.mc.gameSettings.thirdPersonView = 2;
        }
    }

    public void stopEmote()
    {
        if (this.emote != null)
        {
            this.emote = null;

            if (this.cameraBefore >= 0)
            {
                this.mc.gameSettings.thirdPersonView = this.cameraBefore;
                this.cameraBefore = -1;
            }
        }
    }

    public String currentEmote()
    {
        if (this.emote != null && Minecraft.getSystemTime() - this.emoteStart > (long)(this.emoteLength.getFloat() * 1000.0F))
        {
            this.stopEmote();
        }

        return this.emote;
    }

    /** Applies the playing emote if the model belongs to the local player. */
    public void applyEmote(ModelBiped model, Entity entity, float ageInTicks)
    {
        String e = this.currentEmote();

        if (e != null && entity == this.mc.player)
        {
            Emotes.pose(model, e, (float)(Minecraft.getSystemTime() - this.emoteStart) / 50.0F);
        }
    }

    // ---- every tick ---------------------------------------------------------------------------------

    public void onTick()
    {
        EntityPlayerSP p = this.mc.player;

        if (p == null || this.mc.world == null)
        {
            this.lastPlayer = null;
            return;
        }

        if (p != this.lastPlayer)
        {
            // a new player entity: you joined a world, respawned or changed dimension
            this.lastPlayer = p;
            ExtrasRenderer.clear();
            this.runeX = Double.NaN;

            if (this.spawnEntrance.isOn())
            {
                ExtrasRenderer.addPortal(this.mc.world, p.posX, p.posY, p.posZ);
            }
        }

        if (this.emote != null && this.stopWhenMoving.isOn() && (p.movementInput.moveForward != 0.0F || p.movementInput.moveStrafe != 0.0F || p.movementInput.jump || p.isSwingInProgress))
        {
            this.stopEmote();
        }

        if (this.mc.isGamePaused() || p.isInvisible())
        {
            return;
        }

        boolean moving = Math.abs(p.posX - p.prevPosX) + Math.abs(p.posZ - p.prevPosZ) > 0.02D;
        boolean thirdPerson = this.mc.gameSettings.thirdPersonView != 0;

        if (!this.aura.is(CosmeticCatalog.NONE) && this.auraAmount.getInt() > 0 && (thirdPerson || this.auraFirstPerson.isOn()))
        {
            AuraRenderer.particles(this.aura.get(), p, this.mc.world, this.auraAmount.getInt(), moving);
        }

        this.wingParticles(p, thirdPerson);

        if (this.afterimage.isOn())
        {
            ExtrasRenderer.recordGhost(p);
        }

        if (this.runes.isOn() && p.onGround)
        {
            double dx = p.posX - this.runeX;
            double dz = p.posZ - this.runeZ;

            if (Double.isNaN(this.runeX) || dx * dx + dz * dz > 1.3D)
            {
                this.runeX = p.posX;
                this.runeZ = p.posZ;
                // alternate left and right foot
                float yaw = p.rotationYaw * 0.017453292F;
                double side = (this.runeCount++ % 2 == 0 ? 0.18D : -0.18D);
                ExtrasRenderer.addRune(p.posX + Math.cos(yaw) * side, p.posY, p.posZ + Math.sin(yaw) * side, this.runeCount % 5, (float)(this.runeCount * 37 % 360));
            }
        }
    }

    private void wingParticles(EntityPlayerSP p, boolean thirdPerson)
    {
        if (!thirdPerson || this.wings.is(CosmeticCatalog.NONE) || p.ticksExisted % 4 != 0)
        {
            return;
        }

        Random rand = p.getRNG();
        float yaw = p.renderYawOffset * 0.017453292F;
        double side = rand.nextBoolean() ? 1.0D : -1.0D;
        // a point out on one wing, behind the shoulders
        double x = p.posX - Math.sin(yaw) * -0.3D + Math.cos(yaw) * side * (0.4D + rand.nextDouble() * 0.5D);
        double z = p.posZ + Math.cos(yaw) * -0.3D + Math.sin(yaw) * side * (0.4D + rand.nextDouble() * 0.5D);
        double y = p.posY + 1.0D + rand.nextDouble() * 0.6D;

        switch (this.wings.get())
        {
            case "Celestial Wings":
                this.mc.world.spawnParticle(EnumParticleTypes.REDSTONE, x, y, z, 1.0D, 0.82D, 0.25D);
                break;
            case "Frost Wings":
                this.mc.world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, x, y, z, 0.0D, -0.01D, 0.0D);
                break;
            case "Void Wings":
            case "Phantom Wings":
                this.mc.world.spawnParticle(EnumParticleTypes.PORTAL, x, y, z, 0.0D, -0.2D, 0.0D);
                break;
            case "Infernal Wings":
                this.mc.world.spawnParticle(EnumParticleTypes.LAVA, x, y, z, 0.0D, 0.0D, 0.0D);
                break;
            case "Astral Wings":
                this.mc.world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, x, y, z, 0.0D, 0.0D, 0.0D);
                break;
            default:
        }
    }

    /** Called by CombatTracker when a fight ends. */
    public void onFightEnded(boolean won)
    {
        if (won && this.victory.isOn() && this.mc.player != null && this.mc.world != null)
        {
            this.victoryBurst();
        }
    }

    public void victoryBurst()
    {
        if (this.mc.player != null && this.mc.world != null)
        {
            ExtrasRenderer.addBurst(this.mc.world, this.mc.player.posX, this.mc.player.posY, this.mc.player.posZ);
        }
    }

    public void spawnPortal()
    {
        if (this.mc.player != null && this.mc.world != null)
        {
            ExtrasRenderer.addPortal(this.mc.world, this.mc.player.posX, this.mc.player.posY, this.mc.player.posZ);
        }
    }

    // ---- every frame ----------------------------------------------------------------------------

    public void onRenderWorld(float partialTicks)
    {
        EntityPlayerSP p = this.mc.player;

        if (p == null || this.mc.world == null)
        {
            return;
        }

        if (this.trails.isOn())
        {
            this.sampleTrail(p, partialTicks);
        }

        RenderManager rm = this.mc.getRenderManager();
        ExtrasRenderer.renderWorld(rm, ExtrasRenderer.trailColor(this.trailColor.get()));

        if (this.emblem.isOn() && this.mc.gameSettings.thirdPersonView != 0 && !p.isInvisible())
        {
            double x = p.prevPosX + (p.posX - p.prevPosX) * (double)partialTicks - rm.viewerPosX;
            double y = p.prevPosY + (p.posY - p.prevPosY) * (double)partialTicks - rm.viewerPosY;
            double z = p.prevPosZ + (p.posZ - p.prevPosZ) * (double)partialTicks - rm.viewerPosZ;
            net.minecraft.client.renderer.GlStateManager.pushMatrix();
            net.minecraft.client.renderer.GlStateManager.translate(x, y + (double)p.height + (p.isSneaking() ? 0.25D : 0.45D), z);
            net.minecraft.client.renderer.GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
            net.minecraft.client.renderer.GlStateManager.rotate((float)(this.mc.gameSettings.thirdPersonView == 2 ? -1 : 1) * rm.playerViewX, 1.0F, 0.0F, 0.0F);
            net.minecraft.client.renderer.GlStateManager.scale(-0.025F, -0.025F, 0.025F);
            ExtrasRenderer.emblem(this.emblemText.get(), this.emblemColor.getColor(), this.emblemStyle.get(), (float)p.ticksExisted + partialTicks);
            net.minecraft.client.renderer.GlStateManager.popMatrix();
        }
    }

    /** While a swing plays, records where the weapon's tip is: a slash from upper right to lower left. */
    private void sampleTrail(EntityPlayerSP p, float partialTicks)
    {
        if (!p.isSwingInProgress)
        {
            if (this.swinging)
            {
                ExtrasRenderer.breakTrail();
            }

            this.swinging = false;
            return;
        }

        this.swinging = true;
        float progress = p.getSwingProgress(partialTicks);
        Vec3d eye = p.getPositionEyes(partialTicks);
        Vec3d forward = p.getLook(partialTicks);
        Vec3d right = forward.crossProduct(new Vec3d(0.0D, 1.0D, 0.0D)).normalize();
        Vec3d up = right.crossProduct(forward).normalize();
        double sx = 0.6D - 1.15D * (double)progress;
        double sy = 0.45D - 0.8D * (double)progress;
        double bulge = 0.25D * (double)MathHelper.sin(progress * (float)Math.PI);
        Vec3d point = eye.add(forward.scale(0.8D + bulge)).add(right.scale(sx)).add(up.scale(sy - 0.15D));
        ExtrasRenderer.addTrailPoint(point);
    }
}
