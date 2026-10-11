package net.sythix.client.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;
import net.sythix.client.SythixClient;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.GameOptionSetting;
import net.sythix.client.setting.Setting;
import net.sythix.client.ui.component.SettingsPanel;
import org.lwjgl.input.Mouse;

/**
 * Sythix Settings: every video option from Minecraft and OptiFine, organised into tabs, plus
 * Sythix extras. All options here are the real game options and take effect immediately.
 */
public class GuiSythixSettings extends GuiScreen
{
    private static final int SIDEBAR = 96;
    private static String selectedTab = "Video";

    private final GuiScreen parent;
    private final SettingsPanel panel = new SettingsPanel();
    private final Map<String, List<Setting<?>>> tabs = new LinkedHashMap<String, List<Setting<?>>>();
    private boolean changed;
    private int px;
    private int py;
    private int pw;
    private int ph;

    public GuiSythixSettings(GuiScreen parent)
    {
        this.parent = parent;
        this.tabs.put("Video", options(new Object[][] {
            {GameSettings.Options.RENDER_DISTANCE, "How many chunks are drawn around you."},
            {GameSettings.Options.FRAMERATE_LIMIT, "Highest frame rate."},
            {GameSettings.Options.ENABLE_VSYNC, "Sync frames to your monitor (no tearing, may add input delay)."},
            {GameSettings.Options.GRAPHICS, "Fast or Fancy graphics."},
            {GameSettings.Options.AMBIENT_OCCLUSION, "Smooth lighting."},
            {GameSettings.Options.AO_LEVEL, "Smooth lighting strength."},
            {GameSettings.Options.GUI_SCALE, "Size of menus and the HUD."},
            {GameSettings.Options.GAMMA, "Brightness."},
            {GameSettings.Options.FOV, "Field of view."},
            {GameSettings.Options.VIEW_BOBBING, "Camera bobbing while walking."},
            {GameSettings.Options.ATTACK_INDICATOR, "Where the attack cooldown indicator is shown."},
            {GameSettings.Options.USE_FULLSCREEN, "Fullscreen mode."},
            {GameSettings.Options.FULLSCREEN_MODE, "Fullscreen resolution."},
            {GameSettings.Options.USE_VBO, "Vertex buffer objects (usually faster)."},
            {GameSettings.Options.DYNAMIC_FOV, "FOV changes with speed."},
            {GameSettings.Options.DYNAMIC_LIGHTS, "Held torches light up the area."}
        }));
        this.tabs.put("Details", options(new Object[][] {
            {GameSettings.Options.CLOUDS, "Cloud quality."},
            {GameSettings.Options.CLOUD_HEIGHT, "Cloud height."},
            {GameSettings.Options.TREES, "Leaves quality."},
            {GameSettings.Options.RAIN, "Rain and snow quality."},
            {GameSettings.Options.SKY, "Draw the sky."},
            {GameSettings.Options.STARS, "Draw stars."},
            {GameSettings.Options.SUN_MOON, "Draw the sun and moon."},
            {GameSettings.Options.FOG_FANCY, "Fog quality."},
            {GameSettings.Options.FOG_START, "Fog start distance."},
            {GameSettings.Options.VIGNETTE, "Dark screen edges."},
            {GameSettings.Options.TRANSLUCENT_BLOCKS, "Translucent block quality."},
            {GameSettings.Options.HELD_ITEM_TOOLTIPS, "Show the held item name."},
            {GameSettings.Options.DROPPED_ITEMS, "Dropped item quality."},
            {GameSettings.Options.ENTITY_SHADOWS, "Shadows under entities."},
            {GameSettings.Options.SWAMP_COLORS, "Swamp colours."},
            {GameSettings.Options.SMOOTH_BIOMES, "Smooth colour blending between biomes."}
        }));
        this.tabs.put("Quality", options(new Object[][] {
            {GameSettings.Options.MIPMAP_LEVELS, "Mipmap levels."},
            {GameSettings.Options.MIPMAP_TYPE, "Mipmap filtering."},
            {GameSettings.Options.AF_LEVEL, "Anisotropic filtering."},
            {GameSettings.Options.AA_LEVEL, "Anti-aliasing (needs a restart)."},
            {GameSettings.Options.BETTER_GRASS, "Grass on block sides."},
            {GameSettings.Options.BETTER_SNOW, "Snow under tall grass."},
            {GameSettings.Options.CONNECTED_TEXTURES, "Connected textures."},
            {GameSettings.Options.NATURAL_TEXTURES, "Randomly rotated textures."},
            {GameSettings.Options.CUSTOM_SKY, "Custom skies from packs."},
            {GameSettings.Options.CUSTOM_ITEMS, "Custom item textures from packs."},
            {GameSettings.Options.CUSTOM_COLORS, "Custom colours from packs."},
            {GameSettings.Options.CUSTOM_FONTS, "Custom fonts from packs."},
            {GameSettings.Options.RANDOM_MOBS, "Random mob textures from packs."},
            {GameSettings.Options.CUSTOM_ENTITY_MODELS, "Custom entity models from packs."},
            {GameSettings.Options.ALTERNATE_BLOCKS, "Alternate block models."},
            {GameSettings.Options.SCREENSHOT_SIZE, "Screenshot size multiplier."}
        }));
        this.tabs.put("Animations", options(new Object[][] {
            {GameSettings.Options.ANIMATED_WATER, "Water."}, {GameSettings.Options.ANIMATED_LAVA, "Lava."},
            {GameSettings.Options.ANIMATED_FIRE, "Fire."}, {GameSettings.Options.ANIMATED_PORTAL, "Portals."},
            {GameSettings.Options.ANIMATED_REDSTONE, "Redstone."}, {GameSettings.Options.ANIMATED_EXPLOSION, "Explosions."},
            {GameSettings.Options.ANIMATED_FLAME, "Flames."}, {GameSettings.Options.ANIMATED_SMOKE, "Smoke."},
            {GameSettings.Options.ANIMATED_TERRAIN, "Terrain."}, {GameSettings.Options.ANIMATED_TEXTURES, "All animated textures."},
            {GameSettings.Options.PARTICLES, "Particle amount."}, {GameSettings.Options.VOID_PARTICLES, "Void particles."},
            {GameSettings.Options.WATER_PARTICLES, "Water particles."}, {GameSettings.Options.RAIN_SPLASH, "Rain splash."},
            {GameSettings.Options.PORTAL_PARTICLES, "Portal particles."}, {GameSettings.Options.POTION_PARTICLES, "Potion particles."},
            {GameSettings.Options.FIREWORK_PARTICLES, "Firework particles."}, {GameSettings.Options.DRIPPING_WATER_LAVA, "Dripping water/lava."}
        }));
        this.tabs.put("Performance", options(new Object[][] {
            {GameSettings.Options.SMOOTH_FPS, "Even out frame times."},
            {GameSettings.Options.SMOOTH_WORLD, "Spread world loading over time (singleplayer)."},
            {GameSettings.Options.FAST_RENDER, "Faster rendering."},
            {GameSettings.Options.FAST_MATH, "Faster maths."},
            {GameSettings.Options.CHUNK_UPDATES, "Chunk updates per frame."},
            {GameSettings.Options.CHUNK_UPDATES_DYNAMIC, "More chunk updates while standing still."},
            {GameSettings.Options.LAZY_CHUNK_LOADING, "Load chunks gradually."}
        }));
        List<Setting<?>> other = options(new Object[][] {
            {GameSettings.Options.SHOW_FPS, "OptiFine's compact FPS line (Sythix has its own Performance Monitor)."},
            {GameSettings.Options.LAGOMETER, "OptiFine lag graph."},
            {GameSettings.Options.PROFILER, "Show the profiler graph."},
            {GameSettings.Options.ADVANCED_TOOLTIPS, "Item IDs and durability in tooltips."},
            {GameSettings.Options.SHOW_CAPES, "Show capes."},
            {GameSettings.Options.AUTOSAVE_TICKS, "How often singleplayer worlds autosave."},
            {GameSettings.Options.TIME, "Time of day (creative)."},
            {GameSettings.Options.WEATHER, "Weather (singleplayer)."},
            {GameSettings.Options.CLEAR_WATER, "Clear water."}
        });
        this.tabs.put("Other", other);
        List<Setting<?>> sythix = new ArrayList<Setting<?>>();
        BooleanSetting theme = new BooleanSetting("Sythix Theme", "Use the Sythix look for menus and buttons (off = vanilla look).", true)
        {
            public Boolean get()
            {
                return Boolean.valueOf(SythixTheme.enabled);
            }

            public void set(Boolean v)
            {
                SythixTheme.enabled = v.booleanValue();
            }
        };
        BooleanSetting motion = new BooleanSetting("Reduced Motion", "Turn off decorative animations in menus.", false)
        {
            public Boolean get()
            {
                return Boolean.valueOf(SythixTheme.reducedMotion);
            }

            public void set(Boolean v)
            {
                SythixTheme.reducedMotion = v.booleanValue();
            }
        };
        sythix.add(theme);
        sythix.add(motion);
        sythix.add(new ButtonSetting("Mod Menu", "Open the Sythix mod menu.", "Open", () -> this.mc.displayGuiScreen(new GuiModMenu(this))));
        sythix.add(new ButtonSetting("HUD Editor", "Move and resize HUD elements.", "Open", () -> this.mc.displayGuiScreen(new GuiHudEditor(this))));
        this.tabs.put(SythixClient.NAME, sythix);
    }

    private static List<Setting<?>> options(Object[][] entries)
    {
        List<Setting<?>> list = new ArrayList<Setting<?>>();

        for (Object[] entry : entries)
        {
            list.add(new GameOptionSetting((String)entry[1], (GameSettings.Options)entry[0]));
        }

        return list;
    }

    public void initGui()
    {
        this.pw = Math.min(this.width - 12, 440);
        this.ph = Math.min(this.height - 12, 290);
        this.px = (this.width - this.pw) / 2;
        this.py = (this.height - this.ph) / 2;
        this.panel.setBounds(this.px + SIDEBAR + 6, this.py + 26, this.pw - SIDEBAR - 12, this.ph - 50);
        this.panel.setSettings(this.tabs.get(selectedTab));
        this.panel.setOnChange(() -> this.changed = true);
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
        SythixTheme.drawRect((float)this.px, (float)this.py, (float)(this.px + SIDEBAR), (float)(this.py + this.ph), SythixTheme.withAlpha(SythixTheme.BLACK, 0xC0));
        SythixTheme.drawOutline((float)this.px, (float)this.py, (float)(this.px + this.pw), (float)(this.py + this.ph), SythixTheme.BORDER);
        this.fontRenderer.drawString(SythixClient.NAME, (float)(this.px + 6), (float)(this.py + 8), SythixTheme.RED, true);
        this.fontRenderer.drawString("Settings", (float)(this.px + 8 + this.fontRenderer.getStringWidth(SythixClient.NAME)), (float)(this.py + 8), SythixTheme.TEXT, true);
        SythixTheme.drawAccentLine((float)this.px, (float)(this.py + 22), (float)(this.px + SIDEBAR), 1.0F);
        int y = this.py + 26;

        for (String tab : this.tabs.keySet())
        {
            boolean selected = tab.equals(selectedTab);
            boolean hovered = mouseX >= this.px && mouseX < this.px + SIDEBAR && mouseY >= y && mouseY < y + 16;

            if (selected)
            {
                SythixTheme.drawRect((float)this.px, (float)y, (float)(this.px + SIDEBAR), (float)(y + 16), 0x30D81820);
                SythixTheme.drawRect((float)this.px, (float)y, (float)(this.px + 2), (float)(y + 16), SythixTheme.RED);
            }
            else if (hovered)
            {
                SythixTheme.drawRect((float)this.px, (float)y, (float)(this.px + SIDEBAR), (float)(y + 16), 0x18FFFFFF);
            }

            this.fontRenderer.drawString(tab, (float)(this.px + 8), (float)(y + 4), selected ? SythixTheme.TEXT : SythixTheme.TEXT_MUTED, false);
            y += 16;
        }

        boolean doneHover = mouseX >= this.px + 4 && mouseX < this.px + SIDEBAR - 4 && mouseY >= this.py + this.ph - 20 && mouseY < this.py + this.ph - 5;
        SythixTheme.drawButton((float)(this.px + 4), (float)(this.py + this.ph - 20), (float)(SIDEBAR - 8), 15.0F, true, doneHover ? 1.0F : 0.0F);
        this.fontRenderer.drawString("Done", (float)(this.px + SIDEBAR / 2 - this.fontRenderer.getStringWidth("Done") / 2), (float)(this.py + this.ph - 16), SythixTheme.TEXT, false);
        this.fontRenderer.drawString(selectedTab, (float)(this.px + SIDEBAR + 6), (float)(this.py + 9), 0xFFFFFFFF, true);
        SythixTheme.drawAccentLine((float)(this.px + SIDEBAR + 6), (float)(this.py + 22), (float)(this.px + this.pw - 6), 0.8F);
        this.panel.render(mouseX, mouseY);
        Setting<?> hovered = this.panel.getHoveredSetting();
        String hint = hovered != null ? hovered.getDescription() : "Changes apply immediately and are saved with your game options.";
        this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(hint, this.pw - SIDEBAR - 12), (float)(this.px + SIDEBAR + 6), (float)(this.py + this.ph - 18), SythixTheme.TEXT_MUTED, false);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseX >= this.px && mouseX < this.px + SIDEBAR)
        {
            int y = this.py + 26;

            for (String tab : this.tabs.keySet())
            {
                if (mouseY >= y && mouseY < y + 16)
                {
                    selectedTab = tab;
                    this.panel.setSettings(this.tabs.get(tab));
                    return;
                }

                y += 16;
            }

            if (mouseY >= this.py + this.ph - 20 && mouseY < this.py + this.ph - 5)
            {
                this.mc.displayGuiScreen(this.parent);
            }

            return;
        }

        this.panel.mouseClicked(mouseX, mouseY, mouseButton);
    }

    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        super.mouseReleased(mouseX, mouseY, state);
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

    public void onGuiClosed()
    {
        this.mc.gameSettings.saveOptions();
        SythixClient.get().save();

        if (this.changed && this.mc.world != null)
        {
            this.mc.renderGlobal.loadRenderers();
        }
    }
}
