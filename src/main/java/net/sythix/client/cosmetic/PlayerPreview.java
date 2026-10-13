package net.sythix.client.cosmetic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.sythix.client.module.visuals.CosmeticsModule;

/**
 * Draws a 3D player in a menu with a skin, cape, cosmetics and an emote: used by the Profile screen,
 * the Wardrobe and the emote wheel.
 */
public final class PlayerPreview
{
    private static final ResourceLocation ALEX = new ResourceLocation("textures/entity/alex.png");
    private static final ModelPlayer CLASSIC = new ModelPlayer(0.0F, false);
    private static final ModelPlayer SLIM = new ModelPlayer(0.0F, true);

    /** Everything the preview shows; change a copy to preview something before equipping it. */
    public static final class Look
    {
        public String skin = "";
        public boolean slim;
        public String cape = "none";
        public boolean showCape = true;
        public String headwear = CosmeticCatalog.NONE;
        public String wings = CosmeticCatalog.NONE;
        public float wingFlap = 1.0F;
        public float wingSize = 1.0F;
        public String aura = CosmeticCatalog.NONE;
        public boolean companion;
        public boolean emblem;
        public String emblemText = "SYTHIX";
        public int emblemColor = 0xFFB050FF;
        public String emblemStyle = "Spin";
        public String emote;
        public long emoteStart;
        /** A world effect to show around the model in a preview (trails, runes, burst, portal, afterimage). */
        public String effect;
        public int trailColor = 0xFFB050FF;

        /** What is equipped right now. */
        public static Look current(CosmeticsModule c)
        {
            Look look = new Look();
            look.skin = ProfileStore.skin;
            look.slim = ProfileStore.slim;
            look.cape = ProfileStore.cape;

            if (c != null)
            {
                look.showCape = c.showCape();
                look.headwear = c.headwear();
                look.wings = c.wings();
                look.wingFlap = c.wingFlap();
                look.wingSize = c.wingSize();
                look.aura = c.aura();
                look.companion = c.companion();
                look.emblem = c.emblem();
                look.emblemText = c.emblemText();
                look.emblemColor = c.emblemColor();
                look.emblemStyle = c.emblemStyle();
            }

            return look;
        }

        public Look copy()
        {
            Look l = new Look();
            l.skin = this.skin;
            l.slim = this.slim;
            l.cape = this.cape;
            l.showCape = this.showCape;
            l.headwear = this.headwear;
            l.wings = this.wings;
            l.wingFlap = this.wingFlap;
            l.wingSize = this.wingSize;
            l.aura = this.aura;
            l.companion = this.companion;
            l.emblem = this.emblem;
            l.emblemText = this.emblemText;
            l.emblemColor = this.emblemColor;
            l.emblemStyle = this.emblemStyle;
            l.emote = this.emote;
            l.emoteStart = this.emoteStart;
            l.effect = this.effect;
            l.trailColor = this.trailColor;
            return l;
        }
    }

    private PlayerPreview()
    {
    }

    /** Draws the player with the neck at (x, y), turned by yaw degrees. */
    public static void draw(int x, int y, float scale, float yaw, Look look)
    {
        draw(x, y, scale, yaw, 0.0F, look);
    }

    /** Same, tilted by pitch degrees (positive looks down on the player, so ground effects show). */
    public static void draw(int x, int y, float scale, float yaw, float pitch, Look look)
    {
        ModelPlayer m = look.slim ? SLIM : CLASSIC;
        ResourceLocation skinTexture = look.skin == null || look.skin.isEmpty() ? null : CosmeticTextures.skin(look.skin);

        if (skinTexture == null)
        {
            skinTexture = look.slim ? ALEX : DefaultPlayerSkin.getDefaultSkinLegacy();
        }

        Minecraft mc = Minecraft.getMinecraft();
        float age = (float)(Minecraft.getSystemTime() % 1000000L) / 50.0F;
        GlStateManager.enableColorMaterial();
        // Menus draw without depth writes; the model needs them so the cape stays behind the body
        GlStateManager.depthMask(true);
        GlStateManager.clear(256);
        GlStateManager.enableDepth();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)x, (float)y, 200.0F);
        // Model space already has +Y pointing down like the screen. The GUI projection is mirrored,
        // so mirror X once to keep face culling and depth correct
        GlStateManager.scale(-scale, scale, scale);
        GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        // Idle pose with a gentle arm sway
        m.bipedHead.rotateAngleX = 0.0F;
        m.bipedHead.rotateAngleY = 0.0F;
        m.bipedRightArm.rotateAngleX = MathHelper.sin(age * 0.05F) * 0.05F;
        m.bipedLeftArm.rotateAngleX = -MathHelper.sin(age * 0.05F) * 0.05F;
        m.bipedRightArm.rotateAngleY = 0.0F;
        m.bipedLeftArm.rotateAngleY = 0.0F;
        m.bipedRightArm.rotateAngleZ = 0.06F;
        m.bipedLeftArm.rotateAngleZ = -0.06F;
        m.bipedRightLeg.rotateAngleX = 0.0F;
        m.bipedLeftLeg.rotateAngleX = 0.0F;
        m.bipedRightLeg.rotateAngleZ = 0.0F;
        m.bipedLeftLeg.rotateAngleZ = 0.0F;
        m.bipedBody.rotateAngleX = 0.0F;
        m.bipedBody.rotateAngleY = 0.0F;
        m.bipedBody.rotateAngleZ = 0.0F;
        m.bipedHead.rotateAngleZ = 0.0F;
        m.bipedHead.rotationPointY = 0.0F;
        m.bipedHead.rotationPointZ = 0.0F;
        m.bipedRightArm.rotationPointX = -5.0F;
        m.bipedRightArm.rotationPointY = 2.0F;
        m.bipedRightArm.rotationPointZ = 0.0F;
        m.bipedLeftArm.rotationPointX = 5.0F;
        m.bipedLeftArm.rotationPointY = 2.0F;
        m.bipedLeftArm.rotationPointZ = 0.0F;

        if (look.emote != null)
        {
            Emotes.pose(m, look.emote, (float)(Minecraft.getSystemTime() - look.emoteStart) / 50.0F);
        }

        ModelBase.copyModelAngles(m.bipedHead, m.bipedHeadwear);
        ModelBase.copyModelAngles(m.bipedLeftArm, m.bipedLeftArmwear);
        ModelBase.copyModelAngles(m.bipedRightArm, m.bipedRightArmwear);
        ModelBase.copyModelAngles(m.bipedLeftLeg, m.bipedLeftLegwear);
        ModelBase.copyModelAngles(m.bipedRightLeg, m.bipedRightLegwear);
        ModelBase.copyModelAngles(m.bipedBody, m.bipedBodyWear);
        mc.getTextureManager().bindTexture(skinTexture);
        float s = 0.0625F;
        m.bipedHead.render(s);
        m.bipedBody.render(s);
        m.bipedRightArm.render(s);
        m.bipedLeftArm.render(s);
        m.bipedRightLeg.render(s);
        m.bipedLeftLeg.render(s);
        GlStateManager.enableBlend();
        m.bipedHeadwear.render(s);
        m.bipedBodyWear.render(s);
        m.bipedRightArmwear.render(s);
        m.bipedLeftArmwear.render(s);
        m.bipedRightLegwear.render(s);
        m.bipedLeftLegwear.render(s);
        GlStateManager.disableBlend();
        final ResourceLocation capeTexture = look.showCape ? CosmeticTextures.cape(look.cape) : null;

        if (capeTexture != null)
        {
            mc.getTextureManager().bindTexture(capeTexture);
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, 0.0F, 0.125F);
            GlStateManager.rotate(6.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            m.renderCape(s);

            if (CapeArt.isArt(look.cape))
            {
                CapeArt.renderGlow(look.cape, () -> m.renderCape(s));
            }

            GlStateManager.popMatrix();
        }

        LayerSythixCosmetics.drawAll(m, look, age, false);

        if (look.effect != null)
        {
            ExtrasRenderer.previewEffect(look.effect, m, age, look.trailColor);
        }

        if (look.emblem)
        {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, -0.62F, 0.0F);
            // face the viewer whichever way the model is turned
            GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
            GlStateManager.scale(-0.025F, 0.025F, 0.025F);
            ExtrasRenderer.emblem(look.emblemText, look.emblemColor, look.emblemStyle, age);
            GlStateManager.popMatrix();
        }

        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableDepth();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
