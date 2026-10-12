package net.minecraft.client.gui;

import com.google.common.util.concurrent.Runnables;
import java.io.IOException;
import java.net.URI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.realms.RealmsBridge;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServerDemo;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.WorldInfo;
import net.sythix.client.SythixClient;
import net.sythix.client.ui.SythixTheme;
import net.sythix.client.core.Hooks;
import net.sythix.client.module.hud.MainMenuModule;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GLContext;
import net.sythix.client.ui.GuiHudEditor;

/**
 * Sythix Client main menu: themed background with parallax, the Sythix logo and themed buttons.
 */
public class GuiMainMenu extends GuiScreen
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String COPYRIGHT_TEXT = "Resources Copyright Mojang AB";

    /**
     * Not drawn by Sythix. It must stay the first String field: OptiFine's Reflector writes the
     * splash text into the first String field of this class.
     */
    private String splashText;

    private GuiButton buttonResetDemo;
    private final Object threadLock = new Object();
    public static final String MORE_INFO_TEXT = "Please click " + TextFormatting.UNDERLINE + "here" + TextFormatting.RESET + " for more information.";

    /** OpenGL graphics card warning. */
    private String openGLWarning1;
    private String openGLWarning2;
    private String openGLWarningLink;
    private int openGLWarning1Width;
    private int openGLWarning2Width;
    private int openGLWarningX1;
    private int openGLWarningY1;
    private int openGLWarningX2;
    private int openGLWarningY2;

    /** Has the check for a realms notification screen been performed? */
    private boolean hasCheckedForRealmsNotification;
    private GuiScreen realmsNotification;
    private int widthCopyright;
    private int widthCopyrightRest;

    /** Logo position and size, computed in initGui so the buttons sit right below it. */
    private int logoY;
    private int logoSize;

    /** Fades the menu in when it is first opened. */
    private long openedTime;

    public GuiMainMenu()
    {
        this.splashText = "";
        this.openGLWarning1 = "";
        this.openGLWarning2 = MORE_INFO_TEXT;

        if (!GLContext.getCapabilities().OpenGL20 && !OpenGlHelper.areShadersSupported())
        {
            this.openGLWarning1 = I18n.format("title.oldgl1");
            this.openGLWarning2 = I18n.format("title.oldgl2");
            this.openGLWarningLink = "https://help.mojang.com/customer/portal/articles/325948?ref=game";
        }
    }

    /**
     * Is there currently a realms notification screen, and are realms notifications enabled?
     */
    private boolean areRealmsNotificationsEnabled()
    {
        return Minecraft.getMinecraft().gameSettings.getOptionOrdinalValue(GameSettings.Options.REALMS_NOTIFICATIONS) && this.realmsNotification != null;
    }

    /**
     * Called from the main game loop to update the screen.
     */
    public void updateScreen()
    {
        if (this.areRealmsNotificationsEnabled())
        {
            this.realmsNotification.updateScreen();
        }
    }

    /**
     * Returns true if this GUI should pause the game when it is displayed in single-player
     */
    public boolean doesGuiPauseGame()
    {
        return false;
    }

    /**
     * Fired when a key is typed (except F11 which toggles full screen). This is the equivalent of
     * KeyListener.keyTyped(KeyEvent e). Args : character (character on the key), keyCode (lwjgl Keyboard key code)
     */
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
    }

    /**
     * Adds the buttons (and other controls) to the screen in question. Called when the GUI is displayed and when the
     * window resizes, the buttonList is cleared beforehand.
     */
    public void initGui()
    {
        if (this.openedTime == 0L)
        {
            this.openedTime = Minecraft.getSystemTime();
        }

        this.widthCopyright = this.fontRenderer.getStringWidth(COPYRIGHT_TEXT);
        this.widthCopyrightRest = this.width - this.widthCopyright - 2;

        // Layout: logo, then Singleplayer / Multiplayer / Mods+Shaders / Profile+Settings
        MainMenuModule menu = this.menuOptions();
        java.util.List<String[]> shortcuts = menu == null ? java.util.Collections.<String[]>emptyList() : menu.shortcuts();
        int rowH = 25;
        int buttonBlockHeight = rowH * 4 - 5 + (shortcuts.isEmpty() ? 0 : rowH);
        this.logoSize = Math.max(44, Math.min(108, this.height - buttonBlockHeight - 46));

        if (menu != null)
        {
            this.logoSize = Math.round((float)this.logoSize * menu.logoScale());
        }

        int contentHeight = this.logoSize + 10 + buttonBlockHeight;
        this.logoY = Math.max(16, (this.height - contentHeight) / 2 - 4);
        int j = this.logoY + this.logoSize + 10;
        int cx = this.width / 2;

        if (this.mc.isDemo())
        {
            this.addDemoButtons(j, rowH);
        }
        else
        {
            this.addSingleplayerMultiplayerButtons(j, rowH);
        }

        this.buttonList.add(new GuiButton(7, cx - 100, j + rowH * 3, 98, 20, "Profile"));
        this.buttonList.add(new GuiButton(0, cx + 2, j + rowH * 3, 98, 20, "Settings"));

        // small pills in the top-left corner
        String quit = I18n.format("menu.quit");
        int quitW = this.fontRenderer.getStringWidth(quit) + 14;
        this.buttonList.add(new GuiButton(4, 6, 6, quitW, 14, quit));
        this.buttonList.add(new GuiButton(5, 10 + quitW, 6, this.fontRenderer.getStringWidth("Language") + 14, 14, "Language"));

        // Quick-join server shortcuts from the Main Menu module
        for (int s = 0; s < shortcuts.size(); ++s)
        {
            int w = (200 - (shortcuts.size() - 1) * 4) / shortcuts.size();
            this.buttonList.add(new GuiButton(100 + s, cx - 100 + s * (w + 4), j + rowH * 4, w, 20, "\u25b6 " + shortcuts.get(s)[0]));
        }

        if (menu != null && menu.leftButtons())
        {
            // Left layout: move the main buttons to the left side of the screen
            for (GuiButton button : this.buttonList)
            {
                if (button.id != 4 && button.id != 5)
                {
                    button.x = button.x - (cx - 100) + 24;
                }
            }
        }

        synchronized (this.threadLock)
        {
            this.openGLWarning1Width = this.fontRenderer.getStringWidth(this.openGLWarning1);
            this.openGLWarning2Width = this.fontRenderer.getStringWidth(this.openGLWarning2);
            int k = Math.max(this.openGLWarning1Width, this.openGLWarning2Width);
            this.openGLWarningX1 = (this.width - k) / 2;
            this.openGLWarningY1 = (this.buttonList.get(0)).y - 24;
            this.openGLWarningX2 = this.openGLWarningX1 + k;
            this.openGLWarningY2 = this.openGLWarningY1 + 24;
        }

        this.mc.setConnectedToRealms(false);

        if (Minecraft.getMinecraft().gameSettings.getOptionOrdinalValue(GameSettings.Options.REALMS_NOTIFICATIONS) && !this.hasCheckedForRealmsNotification)
        {
            RealmsBridge realmsbridge = new RealmsBridge();
            this.realmsNotification = realmsbridge.getNotificationScreen(this);
            this.hasCheckedForRealmsNotification = true;
        }

        if (this.areRealmsNotificationsEnabled())
        {
            this.realmsNotification.setGuiSize(this.width, this.height);
            this.realmsNotification.initGui();
        }
    }

    /**
     * Adds Singleplayer, Multiplayer and Mods buttons on the main menu.
     */
    private void addSingleplayerMultiplayerButtons(int p_73969_1_, int p_73969_2_)
    {
        this.buttonList.add(new GuiButton(1, this.width / 2 - 100, p_73969_1_, I18n.format("menu.singleplayer")));
        this.buttonList.add(new GuiButton(2, this.width / 2 - 100, p_73969_1_ + p_73969_2_ * 1, I18n.format("menu.multiplayer")));

        this.buttonList.add(new GuiButton(6, this.width / 2 - 100, p_73969_1_ + p_73969_2_ * 2, 98, 20, "Mods"));
        this.buttonList.add(new GuiButton(8, this.width / 2 + 2, p_73969_1_ + p_73969_2_ * 2, 98, 20, "Shaders"));
    }

    /**
     * Adds Demo buttons on Main Menu for players who are playing Demo.
     */
    private void addDemoButtons(int p_73972_1_, int p_73972_2_)
    {
        this.buttonList.add(new GuiButton(11, this.width / 2 - 100, p_73972_1_, I18n.format("menu.playdemo")));
        this.buttonResetDemo = this.addButton(new GuiButton(12, this.width / 2 - 100, p_73972_1_ + p_73972_2_ * 1, I18n.format("menu.resetdemo")));
        ISaveFormat isaveformat = this.mc.getSaveLoader();
        WorldInfo worldinfo = isaveformat.getWorldInfo("Demo_World");

        if (worldinfo == null)
        {
            this.buttonResetDemo.enabled = false;
        }
    }

    /**
     * Called by the controls from the buttonList when activated. (Mouse pressed for buttons)
     */
    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button.id == 0)
        {
            this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
        }

        if (button.id == 5)
        {
            this.mc.displayGuiScreen(new GuiLanguage(this, this.mc.gameSettings, this.mc.getLanguageManager()));
        }

        if (button.id == 1)
        {
            this.mc.displayGuiScreen(new GuiWorldSelection(this));
        }

        if (button.id == 2)
        {
            this.mc.displayGuiScreen(new GuiMultiplayer(this));
        }

        if (button.id == 4)
        {
            this.mc.shutdown();
        }

        if (button.id == 6)
        {
            this.mc.displayGuiScreen(new net.sythix.client.ui.GuiModMenu(this));
        }

        if (button.id == 7)
        {
            this.mc.displayGuiScreen(new net.sythix.client.cosmetic.GuiProfile(this));
        }

        if (button.id == 8)
        {
            this.mc.displayGuiScreen(new net.sythix.client.shader.GuiShaderPacks(this));
        }

        if (button.id >= 100 && button.id < 103)
        {
            MainMenuModule menu = this.menuOptions();

            if (menu != null && button.id - 100 < menu.shortcuts().size())
            {
                String[] shortcut = menu.shortcuts().get(button.id - 100);
                this.mc.displayGuiScreen(new GuiConnecting(this, this.mc, new ServerData(shortcut[0], shortcut[1], false)));
            }
        }

        if (button.id == 11)
        {
            this.mc.launchIntegratedServer("Demo_World", "Demo_World", WorldServerDemo.DEMO_WORLD_SETTINGS);
        }

        if (button.id == 12)
        {
            ISaveFormat isaveformat = this.mc.getSaveLoader();
            WorldInfo worldinfo = isaveformat.getWorldInfo("Demo_World");

            if (worldinfo != null)
            {
                this.mc.displayGuiScreen(new GuiYesNo(this, I18n.format("selectWorld.deleteQuestion"), "'" + worldinfo.getWorldName() + "' " + I18n.format("selectWorld.deleteWarning"), I18n.format("selectWorld.deleteButton"), I18n.format("gui.cancel"), 12));
            }
        }
    }

    public void confirmClicked(boolean result, int id)
    {
        if (result && id == 12)
        {
            ISaveFormat isaveformat = this.mc.getSaveLoader();
            isaveformat.flushCache();
            isaveformat.deleteWorldDirectory("Demo_World");
            this.mc.displayGuiScreen(this);
        }
        else if (id == 12)
        {
            this.mc.displayGuiScreen(this);
        }
        else if (id == 13)
        {
            if (result)
            {
                try
                {
                    Class<?> oclass = Class.forName("java.awt.Desktop");
                    Object object = oclass.getMethod("getDesktop").invoke((Object)null);
                    oclass.getMethod("browse", URI.class).invoke(object, new URI(this.openGLWarningLink));
                }
                catch (Throwable throwable1)
                {
                    LOGGER.error("Couldn't open link", throwable1);
                }
            }

            this.mc.displayGuiScreen(this);
        }
    }

    /**
     * Draws the screen and all the components in it.
     */
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        float fadeIn = Math.min(1.0F, (float)(Minecraft.getSystemTime() - this.openedTime) / 600.0F);

        // Blurred, slowly turning panorama (or the artwork) with a soft red glow behind the logo
        MainMenuModule menu = this.menuOptions();
        SythixTheme.drawBackground(this.width, this.height, mouseX, mouseY, 0.5F);
        boolean animate = (menu == null || menu.logoAnimation()) && !SythixTheme.reducedMotion;
        float bob = animate ? (float)Math.sin((double)(Minecraft.getSystemTime() % 5000L) / 5000.0D * Math.PI * 2.0D) * 1.5F : 0.0F;
        float logoX = menu != null && menu.leftButtons() ? 24.0F + (200.0F - (float)this.logoSize) / 2.0F : (float)(this.width - this.logoSize) / 2.0F;
        float glowX = logoX + this.logoSize / 2.0F;
        float glowY = this.logoY + this.logoSize / 2.0F;

        for (int g = 6; g >= 1; --g)
        {
            float r = this.logoSize * (0.18F + g * 0.07F);
            SythixTheme.drawRounded(glowX - r, glowY - r, glowX + r, glowY + r, r, SythixTheme.withAlpha(SythixTheme.RED, (int)(7 * fadeIn)));
        }

        float rise = (1.0F - fadeIn) * 8.0F;
        SythixTheme.drawTexture(SythixTheme.LOGO, logoX, (float)this.logoY + bob + rise, (float)this.logoSize, (float)this.logoSize, fadeIn);

        // Account chip (top right): your skin's face and name; click it to open the profile
        net.sythix.client.ui.font.SmoothFont font = net.sythix.client.ui.font.SmoothFont.REGULAR;
        String name = this.mc.getSession().getUsername();
        float chipW = font.width(name, 0.95F) + 30.0F;
        float chipX = this.width - 6 - chipW;
        boolean chipHover = mouseX >= chipX && mouseX <= this.width - 6 && mouseY >= 6 && mouseY <= 24;
        SythixTheme.drawGlass(chipX, 6.0F, this.width - 6.0F, 24.0F, 5.0F, chipHover ? 1.3F : 1.0F);
        this.drawHead(chipX + 5.0F, 9.0F, 12.0F, name);
        font.draw(name, chipX + 22.0F, 10.5F, 0.95F, chipHover ? 0xFFFFFFFF : SythixTheme.TEXT);
        this.chipLeft = (int)chipX;

        // Footer: quiet text in the corners
        if (menu == null || menu.footer())
        {
            String branding = SythixClient.getBrandingLine() + (this.mc.isDemo() ? " Demo" : "");
            font.draw(branding, 6.0F, this.height - 11.0F, 0.75F, SythixTheme.withAlpha(SythixTheme.TEXT_MUTED, 0xB0));
            boolean copyrightHovered = mouseX > this.widthCopyrightRest && mouseY > this.height - 12 && Mouse.isInsideWindow();
            float cw = font.width(COPYRIGHT_TEXT, 0.75F);
            this.widthCopyrightRest = (int)(this.width - 6 - cw);
            this.widthCopyright = (int)cw;
            font.draw(COPYRIGHT_TEXT, this.width - 6 - cw, this.height - 11.0F, 0.75F, copyrightHovered ? SythixTheme.TEXT : SythixTheme.withAlpha(SythixTheme.TEXT_MUTED, 0xB0));
        }

        if (this.openGLWarning1 != null && !this.openGLWarning1.isEmpty())
        {
            drawRect(this.openGLWarningX1 - 2, this.openGLWarningY1 - 2, this.openGLWarningX2 + 2, this.openGLWarningY2 - 1, 1428160512);
            this.drawString(this.fontRenderer, this.openGLWarning1, this.openGLWarningX1, this.openGLWarningY1, -1);
            this.drawString(this.fontRenderer, this.openGLWarning2, (this.width - this.openGLWarning2Width) / 2, (this.buttonList.get(0)).y - 12, -1);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        super.drawScreen(mouseX, mouseY, partialTicks);

        if (this.areRealmsNotificationsEnabled())
        {
            this.realmsNotification.drawScreen(mouseX, mouseY, partialTicks);
        }
    }

    /**
     * Called when the mouse is clicked. Args : mouseX, mouseY, clickedButton
     */
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        synchronized (this.threadLock)
        {
            if (!this.openGLWarning1.isEmpty() && !StringUtils.isNullOrEmpty(this.openGLWarningLink) && mouseX >= this.openGLWarningX1 && mouseX <= this.openGLWarningX2 && mouseY >= this.openGLWarningY1 && mouseY <= this.openGLWarningY2)
            {
                GuiConfirmOpenLink guiconfirmopenlink = new GuiConfirmOpenLink(this, this.openGLWarningLink, 13, true);
                guiconfirmopenlink.disableSecurityWarning();
                this.mc.displayGuiScreen(guiconfirmopenlink);
            }
        }

        if (this.areRealmsNotificationsEnabled())
        {
            this.realmsNotification.mouseClicked(mouseX, mouseY, mouseButton);
        }

        if (mouseButton == 0 && mouseX >= this.chipLeft && mouseX <= this.width - 6 && mouseY >= 6 && mouseY <= 24)
        {
            this.mc.displayGuiScreen(new net.sythix.client.cosmetic.GuiProfile(this));
        }

        if (mouseX > this.widthCopyrightRest && mouseX < this.widthCopyrightRest + this.widthCopyright && mouseY > this.height - 12 && mouseY < this.height)
        {
            this.mc.displayGuiScreen(new GuiWinGame(false, Runnables.doNothing()));
        }
    }

    /**
     * Called when the screen is unloaded. Used to disable keyboard repeat events
     */
    public void onGuiClosed()
    {
        if (this.realmsNotification != null)
        {
            this.realmsNotification.onGuiClosed();
        }
    }

    private int chipLeft = Integer.MAX_VALUE;

    /** The face (and hat layer) of the skin the player uses. */
    private void drawHead(float x, float y, float size, String name)
    {
        net.minecraft.util.ResourceLocation skin = null;

        try
        {
            skin = net.sythix.client.cosmetic.CosmeticTextures.skin(net.sythix.client.cosmetic.ProfileStore.skin);
        }
        catch (Throwable ignored)
        {
        }

        if (skin == null)
        {
            skin = net.minecraft.client.resources.DefaultPlayerSkin.getDefaultSkin(net.minecraft.entity.player.EntityPlayer.getOfflineUUID(name));
        }

        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(skin);
        Gui.drawScaledCustomSizeModalRect((int)x, (int)y, 8.0F, 8.0F, 8, 8, (int)size, (int)size, 64.0F, 64.0F);
        Gui.drawScaledCustomSizeModalRect((int)x, (int)y, 40.0F, 8.0F, 8, 8, (int)size, (int)size, 64.0F, 64.0F);
    }

    private MainMenuModule menuOptions()
    {
        return net.sythix.client.SythixClient.get() == null ? null : net.sythix.client.SythixClient.get().getModuleManager().get(MainMenuModule.class);
    }
}
