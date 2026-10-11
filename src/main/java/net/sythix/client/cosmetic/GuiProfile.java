package net.sythix.client.cosmetic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;
import net.minecraft.util.math.MathHelper;
import net.sythix.client.SythixClient;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.visuals.CosmeticsModule;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.InfoSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.Setting;
import net.sythix.client.setting.TextSetting;
import net.sythix.client.ui.SythixTheme;
import net.sythix.client.ui.component.SettingsPanel;
import org.lwjgl.input.Mouse;

/**
 * Profile editor: name, skin, skin model, cape, wings and emotes, with a live 3D preview.
 * Skins and capes are client-side: you see them, other players do not.
 */
public class GuiProfile extends GuiScreen
{
    private static final ResourceLocation ALEX = new ResourceLocation("textures/entity/alex.png");
    private final GuiScreen parent;
    private final SettingsPanel panel = new SettingsPanel();
    private final List<Setting<?>> settings = new ArrayList<Setting<?>>();
    private final ModelPlayer classicModel = new ModelPlayer(0.0F, false);
    private final ModelPlayer slimModel = new ModelPlayer(0.0F, true);
    private TextSetting name;
    private ModeSetting skin;
    private ModeSetting model;
    private ModeSetting cape;
    private List<String[]> capes;
    private String previewEmote;
    private long previewEmoteStart;
    private float yaw = 200.0F;
    private boolean dragging;
    private int lastMouseX;
    private String message = "";
    private int px;
    private int py;
    private int pw;
    private int ph;

    public GuiProfile(GuiScreen parent)
    {
        this.parent = parent;
        this.buildSettings();
    }

    private CosmeticsModule cosmetics()
    {
        return SythixClient.get().getModuleManager().get(CosmeticsModule.class);
    }

    private void buildSettings()
    {
        this.settings.clear();
        String currentName = this.name == null ? this.mc().getSession().getUsername() : this.name.get();
        this.name = new TextSetting("Name", "3-16 letters, numbers or _. Used when joining offline (cracked) servers.", currentName, 16);
        List<String> skinFiles = CosmeticTextures.skins();
        String[] skinOptions = new String[skinFiles.size() + 1];
        skinOptions[0] = "Default";

        for (int i = 0; i < skinFiles.size(); ++i)
        {
            skinOptions[i + 1] = skinFiles.get(i);
        }

        String skinValue = ProfileStore.skin.isEmpty() || !skinFiles.contains(ProfileStore.skin) ? "Default" : ProfileStore.skin;
        this.skin = new ModeSetting("Skin", "Skin file from sythix/skins (64x64 or 64x32 PNG). Click to switch.", skinValue, skinOptions);
        this.model = new ModeSetting("Model", "Classic (Steve) or Slim (Alex) arms.", ProfileStore.slim ? "Slim" : "Classic", "Classic", "Slim");
        this.capes = CosmeticTextures.capes();
        String[] capeNames = new String[this.capes.size()];
        String capeValue = "No Cape";

        for (int i = 0; i < this.capes.size(); ++i)
        {
            capeNames[i] = this.capes.get(i)[1];

            if (this.capes.get(i)[0].equals(ProfileStore.cape))
            {
                capeValue = capeNames[i];
            }
        }

        this.cape = new ModeSetting("Cape", "Built-in Sythix capes, or your own PNGs from sythix/capes (64x32).", capeValue, capeNames);
        this.settings.add(this.name);
        this.settings.add(new InfoSetting("Name Note", "Your name can only change while you are not in a world.", () -> Minecraft.getMinecraft().world == null ? "Change it here" : "Leave the world to change"));
        this.settings.add(this.skin);
        this.settings.add(this.model);
        this.settings.add(new ButtonSetting("Skin Folder", "Put skin PNG files here, then press Refresh.", "Open", () -> this.openFolder(ProfileStore.skinsDir())));
        this.settings.add(this.cape);
        this.settings.add(new ButtonSetting("Cape Folder", "Put cape PNG files here, then press Refresh.", "Open", () -> this.openFolder(ProfileStore.capesDir())));
        this.settings.add(new ButtonSetting("Refresh", "Reload the skin and cape lists from their folders.", "Refresh", () -> {
            CosmeticTextures.clearCache();
            this.buildSettings();
            this.panel.setSettings(this.settings);
        }));

        CosmeticsModule cosmetics = this.cosmetics();

        if (cosmetics != null)
        {
            for (Setting<?> s : cosmetics.getSettings())
            {
                if (s.getName().equals("Wings") || s.getName().startsWith("Wing ") || s.getName().equals("Show Cape"))
                {
                    this.settings.add(s);
                }
            }
        }
    }

    private Minecraft mc()
    {
        return Minecraft.getMinecraft();
    }

    private void openFolder(File dir)
    {
        try
        {
            java.awt.Desktop.getDesktop().open(dir);
        }
        catch (Throwable t)
        {
            Notifications.post("Folder", dir.getAbsolutePath(), Notifications.Kind.INFO);
        }
    }

    private String selectedCapeId()
    {
        for (String[] c : this.capes)
        {
            if (c[1].equals(this.cape.get()))
            {
                return c[0];
            }
        }

        return "none";
    }

    public void initGui()
    {
        this.pw = Math.min(this.width - 12, 440);
        this.ph = Math.min(this.height - 12, 280);
        this.px = (this.width - this.pw) / 2;
        this.py = (this.height - this.ph) / 2;
        int left = this.pw * 2 / 5;
        this.panel.setBounds(this.px + left + 6, this.py + 26, this.pw - left - 12, this.ph - 56);
        this.panel.setSettings(this.settings);
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        if (this.mc.world == null)
        {
            SythixTheme.drawBackground(this.width, this.height, mouseX, mouseY, 0.45F);
        }
        else
        {
            SythixTheme.drawRect(0.0F, 0.0F, (float)this.width, (float)this.height, 0x900B0B0E);
        }

        SythixTheme.drawRect((float)this.px, (float)this.py, (float)(this.px + this.pw), (float)(this.py + this.ph), SythixTheme.withAlpha(SythixTheme.CHARCOAL, 0xF0));
        SythixTheme.drawOutline((float)this.px, (float)this.py, (float)(this.px + this.pw), (float)(this.py + this.ph), SythixTheme.BORDER);
        this.fontRenderer.drawString("Profile", (float)(this.px + 8), (float)(this.py + 8), 0xFFFFFFFF, true);
        this.fontRenderer.drawString("Skins and capes are shown on your screen only.", (float)(this.px + 50), (float)(this.py + 8), SythixTheme.TEXT_MUTED, false);
        SythixTheme.drawAccentLine((float)this.px, (float)(this.py + 21), (float)(this.px + this.pw), 1.0F);

        // Preview area
        int left = this.pw * 2 / 5;
        SythixTheme.drawRect((float)(this.px + 6), (float)(this.py + 26), (float)(this.px + left), (float)(this.py + this.ph - 52), 0x40000000);

        if (this.dragging)
        {
            this.yaw += (float)(mouseX - this.lastMouseX) * 1.5F;
            this.lastMouseX = mouseX;
        }

        int previewH = this.ph - 80;
        float scale = Math.min((float)previewH / 2.4F, (float)left / 1.6F);

        try
        {
            this.drawPlayer(this.px + 6 + (left - 6) / 2, this.py + 42 + (int)(scale * 0.5F), scale);
        }
        catch (Throwable t)
        {
            net.sythix.client.core.ModuleErrors.report(this.cosmetics(), "profile preview", t);
        }

        this.fontRenderer.drawString(this.name.get(), (float)(this.px + 6 + (left - 6) / 2 - this.fontRenderer.getStringWidth(this.name.get()) / 2), (float)(this.py + 30), 0xFFFFFFFF, true);
        this.fontRenderer.drawString("Drag to rotate", (float)(this.px + 10), (float)(this.py + this.ph - 62), SythixTheme.TEXT_DISABLED, false);

        // Emote preview buttons
        int bw = (left - 6) / CosmeticsModule.EMOTES.length - 2;

        for (int i = 0; i < CosmeticsModule.EMOTES.length; ++i)
        {
            int bx = this.px + 6 + i * (bw + 2);
            int by = this.py + this.ph - 48;
            boolean hover = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + 14;
            SythixTheme.drawButton((float)bx, (float)by, (float)bw, 14.0F, true, hover ? 1.0F : 0.0F);
            String label = this.fontRenderer.trimStringToWidth(CosmeticsModule.EMOTES[i], bw - 2);
            this.fontRenderer.drawString(label, (float)(bx + bw / 2 - this.fontRenderer.getStringWidth(label) / 2), (float)(by + 3), SythixTheme.TEXT, false);
        }

        this.panel.render(mouseX, mouseY);
        Setting<?> hovered = this.panel.getHoveredSetting();
        String hint = !this.message.isEmpty() ? this.message : (hovered != null ? hovered.getDescription() : "");
        this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(hint, this.pw - left - 12), (float)(this.px + left + 6), (float)(this.py + this.ph - 46), this.message.isEmpty() ? SythixTheme.TEXT_MUTED : 0xFFFF5555, false);
        this.drawBottomButton("Save", this.px + this.pw - 130, mouseX, mouseY);
        this.drawBottomButton("Cancel", this.px + this.pw - 66, mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawBottomButton(String text, int x, int mouseX, int mouseY)
    {
        int y = this.py + this.ph - 24;
        boolean hover = mouseX >= x && mouseX < x + 60 && mouseY >= y && mouseY < y + 18;
        SythixTheme.drawButton((float)x, (float)y, 60.0F, 18.0F, true, hover ? 1.0F : 0.0F);
        this.fontRenderer.drawString(text, (float)(x + 30 - this.fontRenderer.getStringWidth(text) / 2), (float)(y + 5), SythixTheme.TEXT, false);
    }

    /** Draws the player model with the chosen skin, cape and wings. */
    private void drawPlayer(int x, int y, float scale)
    {
        boolean slim = this.model.is("Slim");
        ModelPlayer m = slim ? this.slimModel : this.classicModel;
        ResourceLocation skinTexture = this.skin.is("Default") ? (slim ? ALEX : DefaultPlayerSkin.getDefaultSkinLegacy()) : CosmeticTextures.skin(this.skin.get());

        if (skinTexture == null)
        {
            skinTexture = slim ? ALEX : DefaultPlayerSkin.getDefaultSkinLegacy();
        }

        float age = (float)(Minecraft.getSystemTime() % 100000L) / 50.0F;
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
        GlStateManager.rotate(this.yaw, 0.0F, 1.0F, 0.0F);
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

        if (this.previewEmote != null && Minecraft.getSystemTime() - this.previewEmoteStart < 3000L)
        {
            CosmeticsModule.pose(m, this.previewEmote, age);
        }

        ModelBase.copyModelAngles(m.bipedHead, m.bipedHeadwear);
        ModelBase.copyModelAngles(m.bipedLeftArm, m.bipedLeftArmwear);
        ModelBase.copyModelAngles(m.bipedRightArm, m.bipedRightArmwear);
        ModelBase.copyModelAngles(m.bipedLeftLeg, m.bipedLeftLegwear);
        ModelBase.copyModelAngles(m.bipedRightLeg, m.bipedRightLegwear);
        ModelBase.copyModelAngles(m.bipedBody, m.bipedBodyWear);
        this.mc.getTextureManager().bindTexture(skinTexture);
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
        CosmeticsModule cosmetics = this.cosmetics();
        ResourceLocation capeTexture = CosmeticTextures.cape(this.selectedCapeId());

        if (capeTexture != null && (cosmetics == null || cosmetics.showCape()))
        {
            this.mc.getTextureManager().bindTexture(capeTexture);
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, 0.0F, 0.125F);
            GlStateManager.rotate(6.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            m.renderCape(s);
            GlStateManager.popMatrix();
        }

        if (cosmetics != null && cosmetics.showWings())
        {
            WingRenderer.draw(age, cosmetics.wingColor(), cosmetics.wingFlap(), cosmetics.wingSize(), false);
        }

        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableDepth();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private boolean save()
    {
        String newName = this.name.get().trim();

        if (!ProfileStore.isValidName(newName))
        {
            this.message = "Names must be 3-16 letters, numbers or _.";
            return false;
        }

        if (!newName.equals(this.mc.getSession().getUsername()))
        {
            if (this.mc.world != null)
            {
                this.message = "Leave the world before changing your name.";
                return false;
            }

            this.mc.setSessionSythix(new Session(newName, newName, "0", "legacy"));
        }

        ProfileStore.name = newName;
        ProfileStore.skin = this.skin.is("Default") ? "" : this.skin.get();
        ProfileStore.slim = this.model.is("Slim");
        ProfileStore.cape = this.selectedCapeId();
        ProfileStore.save();
        SythixClient.get().save();
        Notifications.post("Profile saved", "Playing as " + newName + ".", Notifications.Kind.SUCCESS);
        return true;
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.message = "";
        int left = this.pw * 2 / 5;
        int by = this.py + this.ph - 24;

        if (mouseY >= by && mouseY < by + 18)
        {
            if (mouseX >= this.px + this.pw - 130 && mouseX < this.px + this.pw - 70)
            {
                if (this.save())
                {
                    this.mc.displayGuiScreen(this.parent);
                }

                return;
            }

            if (mouseX >= this.px + this.pw - 66 && mouseX < this.px + this.pw - 6)
            {
                this.mc.displayGuiScreen(this.parent);
                return;
            }
        }

        int bw = (left - 6) / CosmeticsModule.EMOTES.length - 2;
        int ey = this.py + this.ph - 48;

        if (mouseY >= ey && mouseY < ey + 14 && mouseX >= this.px + 6 && mouseX < this.px + left)
        {
            int index = (mouseX - this.px - 6) / (bw + 2);

            if (index >= 0 && index < CosmeticsModule.EMOTES.length)
            {
                this.previewEmote = CosmeticsModule.EMOTES[index];
                this.previewEmoteStart = Minecraft.getSystemTime();
            }

            return;
        }

        if (mouseX >= this.px + 6 && mouseX < this.px + left && mouseY >= this.py + 26 && mouseY < ey)
        {
            this.dragging = true;
            this.lastMouseX = mouseX;
            return;
        }

        this.panel.mouseClicked(mouseX, mouseY, mouseButton);
    }

    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        super.mouseReleased(mouseX, mouseY, state);
        this.dragging = false;
        this.panel.mouseReleased();
    }

    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();

        if (wheel != 0)
        {
            this.panel.scrollBy(wheel);
        }
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (this.panel.keyTyped(typedChar, keyCode))
        {
            return;
        }

        if (keyCode == 1)
        {
            this.mc.displayGuiScreen(this.parent);
        }
    }

    public boolean doesGuiPauseGame()
    {
        return false;
    }
}
