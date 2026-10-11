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

        // Layout: logo, then three full-width buttons, then the Options/Quit row
        int buttonBlockHeight = 24 * 3 + 12 + 20;
        MainMenuModule menu = this.menuOptions();
        java.util.List<String[]> shortcuts = menu == null ? java.util.Collections.<String[]>emptyList() : menu.shortcuts();

        if (!shortcuts.isEmpty())
        {
            buttonBlockHeight += 22;
        }

        this.logoSize = Math.max(48, Math.min(130, this.height - buttonBlockHeight - 40));

        if (menu != null)
        {
            this.logoSize = Math.round((float)this.logoSize * menu.logoScale());
        }

        int contentHeight = this.logoSize + 8 + buttonBlockHeight;
        this.logoY = Math.max(6, (this.height - contentHeight) / 2 - 6);
        int j = this.logoY + this.logoSize + 8;

        if (this.mc.isDemo())
        {
            this.addDemoButtons(j, 24);
        }
        else
        {
            this.addSingleplayerMultiplayerButtons(j, 24);
        }

        this.buttonList.add(new GuiButton(0, this.width / 2 - 100, j + 72 + 12, 98, 20, I18n.format("menu.options")));
        this.buttonList.add(new GuiButton(4, this.width / 2 + 2, j + 72 + 12, 98, 20, I18n.format("menu.quit")));
        this.buttonList.add(new GuiButtonLanguage(5, this.width / 2 - 124, j + 72 + 12));

        // Quick-join server shortcuts from the Main Menu module
        for (int s = 0; s < shortcuts.size(); ++s)
        {
            int w = (200 - (shortcuts.size() - 1) * 4) / shortcuts.size();
            this.buttonList.add(new GuiButton(100 + s, this.width / 2 - 100 + s * (w + 4), j + 72 + 12 + 24, w, 20, "\u25b6 " + shortcuts.get(s)[0]));
        }

        if (menu != null && menu.leftButtons())
        {
            // Left layout: move every button to the left side of the screen
            for (GuiButton button : this.buttonList)
            {
                button.x = button.x - (this.width / 2 - 100) + 20;
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

        this.buttonList.add(new GuiButton(6, this.width / 2 - 100, p_73969_1_ + p_73969_2_ * 2, 98, 20, SythixClient.NAME + " Mods"));
        this.buttonList.add(new GuiButton(7, this.width / 2 + 2, p_73969_1_ + p_73969_2_ * 2, 98, 20, "Profile"));
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

        // Background with gentle mouse parallax, darkened at the top and bottom
        MainMenuModule menu = this.menuOptions();
        String background = menu == null ? "Sythix" : menu.getBackground();
        boolean parallax = (menu == null || menu.parallax()) && !SythixTheme.reducedMotion;

        if (background.equals("Dark"))
        {
            SythixTheme.drawRect(0.0F, 0.0F, (float)this.width, (float)this.height, SythixTheme.BLACK);
        }
        else if (background.equals("Gradient"))
        {
            SythixTheme.drawQuad(0.0F, 0.0F, (float)this.width, (float)this.height, SythixTheme.lerpColor(SythixTheme.BLACK, SythixTheme.BLUE, 0.35F), SythixTheme.BLACK, SythixTheme.lerpColor(SythixTheme.BLACK, SythixTheme.RED, 0.45F), SythixTheme.BLACK);
        }
        else
        {
            SythixTheme.drawBackground(this.width, this.height, parallax ? mouseX : -1, parallax ? mouseY : -1, 0.0F);
        }

        SythixTheme.drawGradientV(0.0F, 0.0F, (float)this.width, (float)this.height * 0.35F, 0x900B0B0E, 0x000B0B0E);
        SythixTheme.drawGradientV(0.0F, (float)this.height * 0.7F, (float)this.width, (float)this.height, 0x000B0B0E, 0xC00B0B0E);

        // Logo, gently bobbing; the texture is square with the artwork centred
        boolean animate = (menu == null || menu.logoAnimation()) && !SythixTheme.reducedMotion;
        float bob = animate ? (float)Math.sin((double)(Minecraft.getSystemTime() % 4000L) / 4000.0D * Math.PI * 2.0D) * 1.5F : 0.0F;
        float logoX = menu != null && menu.leftButtons() ? 20.0F + (200.0F - (float)this.logoSize) / 2.0F : (float)(this.width - this.logoSize) / 2.0F;
        SythixTheme.drawTexture(SythixTheme.LOGO, logoX, (float)this.logoY + bob, (float)this.logoSize, (float)this.logoSize, fadeIn);

        // Footer bar
        if (menu == null || menu.footer())
        {
        SythixTheme.drawRect(0.0F, (float)(this.height - 12), (float)this.width, (float)this.height, 0xB00B0B0E);
        SythixTheme.drawAccentLine(0.0F, (float)(this.height - 12), (float)this.width, 0.9F);
        String branding = SythixClient.getBrandingLine();

        if (this.mc.isDemo())
        {
            branding = branding + " Demo";
        }

        this.drawString(this.fontRenderer, branding, 2, this.height - 10, SythixTheme.TEXT_MUTED);
        boolean copyrightHovered = mouseX > this.widthCopyrightRest && mouseX < this.widthCopyrightRest + this.widthCopyright && mouseY > this.height - 10 && mouseY < this.height && Mouse.isInsideWindow();
        this.drawString(this.fontRenderer, COPYRIGHT_TEXT, this.widthCopyrightRest, this.height - 10, copyrightHovered ? SythixTheme.LIGHT_BLUE : SythixTheme.TEXT_MUTED);
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

        if (mouseX > this.widthCopyrightRest && mouseX < this.widthCopyrightRest + this.widthCopyright && mouseY > this.height - 10 && mouseY < this.height)
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

    private MainMenuModule menuOptions()
    {
        return net.sythix.client.SythixClient.get() == null ? null : net.sythix.client.SythixClient.get().getModuleManager().get(MainMenuModule.class);
    }
}
