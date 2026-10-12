package net.sythix.client.module.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.sythix.client.SythixClient;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.ui.GuiListScreen;
import net.sythix.client.ui.GuiTextPage;
import net.sythix.client.ui.SythixTheme;

/**
 * #56 Built-in guides: getting started, every major feature, and troubleshooting.
 */
public class HelpCenter extends Module
{
    static final String[][] PAGES = new String[][] {
        {"Getting started", "The basics in two minutes",
            "# Welcome to " + SythixClient.FULL_NAME + "\n"
            + "Everything in Sythix is a module. Modules can be switched on and off and almost all of them have settings.\n\n"
            + "# Opening the mod menu\n"
            + "Press Right Shift in game (change it in Controls or the Keybind Manager), or click \"Sythix Mods\" on the main menu.\n\n"
            + "# Finding things\n"
            + "Use the search box at the top of the mod menu, the category tabs on the left, or the Favourites tab. Click the star on a card to favourite it.\n\n"
            + "# Changing settings\n"
            + "Click the gear on a card. Every setting shows a description when you hover it, and the arrow next to it resets just that setting."},
        {"Moving the HUD", "HUD editor, snapping and layouts",
            "# HUD editor\n"
            + "Open it from the mod menu (HUD Editor button). Drag elements to move them, scroll over one to resize it, and click one to edit its settings in the side panel.\n\n"
            + "# Snapping\n"
            + "Elements snap to the screen edges, the centre and each other. Hold Shift while dragging to move freely. Snapping, the grid and guides are set in the HUD Editor module.\n\n"
            + "# Layouts\n"
            + "Save HUD layouts in the HUD Editor module, or whole setups with the Profile Manager.\n\n"
            + "# Bigger text\n"
            + "Accessibility > HUD Size scales every element at once."},
        {"Keybinds", "Change keys and fix conflicts",
            "# Keybind Manager\n"
            + "Lists every Minecraft and Sythix key. Red rows share a key with something else. Double-click a row and press a key to rebind it; Esc clears it.\n\n"
            + "# Module keys\n"
            + "Every module has a Toggle Key in its settings. In Keybind Manager you can choose Toggle (press = on/off) or Hold (on while held)."},
        {"Profiles and backups", "Save, share and restore setups",
            "# Profiles\n"
            + "Profile Manager saves every setting and HUD position under a name. Load one with a double-click. Export copies it to the clipboard so you can share it; Import reads it back. Profiles never contain account details or passwords.\n\n"
            + "# Server profiles\n"
            + "Server Profiles can load a profile automatically when you join a specific server.\n\n"
            + "# Backups\n"
            + "Settings Manager makes a backup when the game starts and keeps the newest ones. Restore a backup if something goes wrong."},
        {"Texture packs and modern textures", "Resource packs and the 1.21 look",
            "# Using packs\n"
            + "Texture Pack Manager lists your packs and lets you turn them on and off, change their order and mark favourites. Put .zip packs in the resourcepacks folder.\n\n"
            + "# Modern (1.21) textures\n"
            + "Put a resource pack made for Minecraft 1.13-1.21 (for example the 1.21 default textures, or any modern pack you are allowed to use) in the resourcepacks folder. Open Texture Pack Manager > Modern Pack Converter, pick the pack and press Convert. Sythix renames its block, item, mob and interface textures to the 1.12 names and saves \"NAME (1.12).zip\". Turn that pack on and blocks, items and mobs that exist in 1.12 get the modern look.\n\n"
            + "Textures whose layout changed since 1.12 are left out, so nothing shows up broken. The textures for Sythix's own 1.21 blocks and items are converted too; 1.21 Content > Real 1.21 Textures can also read them straight from a Minecraft version you have installed.\n\n"
            + "# Compatibility\n"
            + "Unconverted packs made for newer versions show missing (purple/black) textures. Convert them first, or use packs made for pack_format 3 (1.11-1.12)."},
        {"Shaders", "Shader packs and moving water",
            "# Turning shaders on\n"
            + "Press Shaders on the main menu, in the pause menu or in Sythix Settings and click a pack. Sythix Vibrant is fast and adds colour, glow, swaying plants and moving water. Sythix Shadows adds real shadows and needs a stronger graphics card. Off turns shaders off again.\n\n"
            + "# Your own packs\n"
            + "Press Add Pack and type the path of a shader pack (.zip or folder), or press Open Folder and copy packs into the shaderpacks folder. Packs made for OptiFine 1.8-1.12 work best. Options opens the pack's own settings.\n\n"
            + "# If something looks wrong\n"
            + "Shaders need a graphics card with OpenGL 2.0 or newer. Sythix switches off anti-aliasing, anisotropic filtering, Fast Render and 3D anaglyph for you, because they do not work with shaders. If the game gets slow, try Sythix Vibrant or lower Render Distance."},
        {"1.21 blocks, items and mobs", "New content in singleplayer and LAN",
            "# What it is\n"
            + "Sythix adds blocks, items, effects, enchantments, world generation and 35 mobs from Minecraft 1.13-1.21. They work in singleplayer and LAN worlds; normal 1.12 servers do not have them, and joining them is not affected.\n\n"
            + "# Getting them\n"
            + "In Creative the new blocks and items are in the normal tabs and the spawn eggs are in Miscellaneous. In Survival, new chunks generate deepslate, copper, amethyst geodes, cherry and pale oak trees and more, and the new mobs spawn in their biomes. Crafting and smelting recipes work like in 1.21 (netherite: put a netherite ingot next to a diamond tool or armour piece).\n\n"
            + "# Mobs worth knowing\n"
            + "Give a piglin a gold ingot to barter. Give an allay an item and it collects matching items for you. Step on a sculk shrieker four times and a warden comes (careful!). Saddle a camel, strider (steer with a warped fungus on a stick) or happy ghast to ride it. Phantoms come if you do not sleep for three nights. Creakings only move while nobody looks at them.\n\n"
            + "# Settings\n"
            + "The 1.21 Content module (Advanced) turns the new world generation and natural mob spawning on or off. The textures are placeholders; use Real 1.21 Textures for the real look of blocks and items."},
        {"Recording and replays", "Videos, replays and screenshots",
            "# Video\n"
            + "Turn on Recording Tools and press its Recording Mode key (or button). With Built-in Recorder on, Sythix saves an .avi video in the recordings folder - no OBS needed. Sound is not recorded; use OBS if you need it.\n\n"
            + "# Replays\n"
            + "In Replay System set Record Replays (for example Everything). Each session is then saved from the moment you join. Open Replays to watch one: you fly around freely while everything happens again. P pauses, Left/Right jump 10 seconds, [ and ] change the speed, M jumps to the next marker (your bookmarks). Esc > Disconnect leaves the replay.\n\n"
            + "# Screenshots\n"
            + "Screenshot Manager names your screenshots and has a gallery."},
        {"Performance tips", "More FPS",
            "# Quick wins\n"
            + "Turn on Performance Optimizer, lower render distance, set graphics to Fast and turn off clouds. Particle Controller and Animation Controller can remove effects you do not need.\n\n"
            + "# Sythix Settings\n"
            + "Options > Sythix Settings has every video option in one place, including the OptiFine details and quality options.\n\n"
            + "# Measuring\n"
            + "Performance Optimizer > Run Benchmark measures your average FPS and 1% low FPS. Change one setting, run it again and compare in Benchmark Results. Performance Monitor shows FPS, frame time and memory live.\n\n"
            + "# Adaptive render distance\n"
            + "Turn it on in Performance Optimizer and set a Target FPS: the render distance drops when the game struggles and goes back up when it can."},
        {"Multiplayer", "Servers, friends and parties",
            "# Server Manager\n"
            + "Favourites, folders, notes and icons for your servers. Open it from the Multiplayer screen.\n\n"
            + "# Friends and parties\n"
            + "Friends Manager highlights friends in chat, the tab list and nametags. Party Tools tracks your party members. Both are client-side only; nothing is sent to other players.\n\n"
            + "# Server rules\n"
            + "Some servers do not allow certain client features (for example Freelook). Always follow the rules of the server you play on. Sythix has no cheats."},
        {"Troubleshooting", "When something goes wrong",
            "# A module switched itself off\n"
            + "If a module hits an error it is switched off so the game keeps running and you get a notification. Error Handler lists what happened and can turn modules back on.\n\n"
            + "# The game looks wrong or crashes\n"
            + "Use Safe Mode: it turns off every optional module and keeps a snapshot so you can restore everything later. You can also tick \"Start In Safe Mode\" so the next start is clean.\n\n"
            + "# Settings are broken\n"
            + "Settings Manager > Backups lets you go back to an earlier copy. Reset Everything returns to defaults (after making a backup).\n\n"
            + "# Missing textures\n"
            + "Disable resource packs one by one in Texture Pack Manager.\n\n"
            + "# Getting help\n"
            + "Diagnostics > Copy puts a report on your clipboard that you can share. It contains no account details or passwords."},
        {"Privacy", "What Sythix stores and sends",
            "# Stored on your computer\n"
            + "Settings (sythix/config.json), profiles, backups, waypoints, statistics, screenshots, videos (recordings), replays and your local profile (name, skin and cape files). Replays contain everything the server sent during that session, including chat - be careful who you share them with.\n\n"
            + "# Sent over the internet\n"
            + "Nothing by default. Update Manager only checks a URL if you turn it on and enter one. Cosmetics, friends and parties are visible only to you.\n\n"
            + "# Streamer mode\n"
            + "Privacy Controls can hide your name, the server address and coordinates."}
    };

    private final BooleanSetting showTips;

    public HelpCenter()
    {
        super("Help Center", "Tutorials, feature guides and troubleshooting.", Category.CLIENT, true);
        this.setCore();
        this.add(new ButtonSetting("Help", "Open the help pages.", "Open", () -> this.mc.displayGuiScreen(new Screen(this.mc.currentScreen))));
        this.showTips = this.add(new BooleanSetting("Welcome Tip", "Show how to open the mod menu when you join a world.", true));
    }

    public GuiScreen createScreen(GuiScreen parent)
    {
        return new Screen(parent);
    }

    private boolean tipShown;

    public void onWorldJoin()
    {
        if (this.showTips.isOn() && !this.tipShown)
        {
            this.tipShown = true;
            String key = net.minecraft.client.settings.GameSettings.getKeyDisplayString(this.mc.gameSettings.keyBindSythixMenu.getKeyCode());
            net.sythix.client.core.Notifications.post(SythixClient.FULL_NAME, "Press " + key + " to open the mod menu.", net.sythix.client.core.Notifications.Kind.INFO);
        }
    }

    static class Screen extends GuiListScreen
    {
        Screen(GuiScreen parent)
        {
            super(parent, "Help Center");
        }

        protected String header()
        {
            return "Double-click a topic to read it.";
        }

        protected List<Row> rows()
        {
            List<Row> rows = new ArrayList<Row>();

            for (int i = 0; i < PAGES.length; ++i)
            {
                rows.add(new Row(Integer.valueOf(i), PAGES[i][0], PAGES[i][1], "", i % 2 == 0 ? SythixTheme.RED : SythixTheme.BLUE));
            }

            return rows;
        }

        protected String[] buttons()
        {
            return new String[] {"Read", "Done"};
        }

        protected boolean enabled(int index)
        {
            return index == 1 || this.selected != null;
        }

        protected void open(Object key)
        {
            int i = ((Integer)key).intValue();
            this.mc.displayGuiScreen(new GuiTextPage(this, PAGES[i][0], () -> PAGES[i][2]));
        }

        protected void press(int index)
        {
            if (index == 0)
            {
                this.open(this.selected);
            }
            else
            {
                this.mc.displayGuiScreen(this.parent);
            }
        }
    }
}
