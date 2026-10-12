package net.sythix.client.cosmetic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.Session;
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
 * Profile editor: name, skin, skin model and cape, with a live 3D preview (the Wardrobe button
 * opens the cosmetics and emotes).
 * Skins and capes are client-side: you see them, other players do not.
 */
public class GuiProfile extends GuiScreen
{
    private final GuiScreen parent;
    private final SettingsPanel panel = new SettingsPanel();
    private final List<Setting<?>> settings = new ArrayList<Setting<?>>();
    private TextSetting name;
    private ModeSetting skin;
    private ModeSetting model;
    private ModeSetting cape;
    private List<String[]> capes;
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

        this.cape = new ModeSetting("Cape", "The ten animated Sythix capes, or your own PNGs from sythix/capes (64x32).", capeValue, capeNames);
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

        this.settings.add(new ButtonSetting("Wardrobe", "Headwear, wings, auras, extras and emotes, with a live preview.", "Open", () -> {
            if (this.save())
            {
                this.mc().displayGuiScreen(new GuiCosmetics(this));
            }
        }));
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
            SythixTheme.drawBackground(this.width, this.height, -1, -1, 0.35F);
        }

        SythixTheme.drawGlass((float)this.px, (float)this.py, (float)(this.px + this.pw), (float)(this.py + this.ph), 8.0F, 1.0F);
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
        this.fontRenderer.drawString("Drag to rotate", (float)(this.px + 10), (float)(this.py + this.ph - 64), SythixTheme.TEXT_DISABLED, false);

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

    /** Draws the player with the chosen skin and cape and the equipped cosmetics. */
    private void drawPlayer(int x, int y, float scale)
    {
        PlayerPreview.Look look = PlayerPreview.Look.current(this.cosmetics());
        look.skin = this.skin.is("Default") ? "" : this.skin.get();
        look.slim = this.model.is("Slim");
        look.cape = this.selectedCapeId();
        PlayerPreview.draw(x, y, scale, this.yaw, look);
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

        int ey = this.py + this.ph - 52;

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
