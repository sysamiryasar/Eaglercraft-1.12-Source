package net.sythix.client.module.performance;

import net.minecraft.client.settings.GameSettings;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.GameOptionSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;

/**
 * #1 FPS presets, render distance and optimisation toggles. The game options shown here are the
 * real Minecraft/OptiFine options.
 */
public class PerformanceOptimizer extends Module
{
    private final ModeSetting preset;
    private final NumberSetting unfocusedFps;
    private final NumberSetting menuFps;
    private final NumberSetting entityDistance;

    public PerformanceOptimizer()
    {
        super("Performance Optimizer", "FPS presets, render distance and optimisation toggles.", Category.PERFORMANCE, true);
        this.preset = this.add(new ModeSetting("Preset", "Choose a preset, then press Apply.", "Balanced", "Max FPS", "Balanced", "Quality"));
        this.add(new ButtonSetting("Apply Preset", "Changes your video settings to the chosen preset.", "Apply", this::applyPreset));
        this.add(new GameOptionSetting("How many chunks are drawn around you. Lower = more FPS.", GameSettings.Options.RENDER_DISTANCE));
        this.add(new GameOptionSetting("Highest frame rate the game may run at.", GameSettings.Options.FRAMERATE_LIMIT));
        this.add(new GameOptionSetting("Fast graphics are quicker; Fancy looks nicer.", GameSettings.Options.GRAPHICS));
        this.add(new GameOptionSetting("OptiFine render optimisation (big FPS gain on most GPUs).", GameSettings.Options.FAST_RENDER));
        this.add(new GameOptionSetting("Faster maths for lighting and terrain.", GameSettings.Options.FAST_MATH));
        this.add(new GameOptionSetting("How many chunk updates are done per frame.", GameSettings.Options.CHUNK_UPDATES));
        this.add(new GameOptionSetting("Load chunks gradually for smoother frame times.", GameSettings.Options.LAZY_CHUNK_LOADING));
        this.add(new GameOptionSetting("Even out frame times by waiting for the GPU.", GameSettings.Options.SMOOTH_FPS));
        this.add(new GameOptionSetting("Shadows under mobs and players.", GameSettings.Options.ENTITY_SHADOWS));
        this.unfocusedFps = this.add(new NumberSetting("Background FPS", "Frame rate when the game window is not focused. 0 = no limit.", 15.0D, 0.0D, 60.0D, 5.0D, " fps"));
        this.menuFps = this.add(new NumberSetting("Menu FPS", "Frame rate while a menu is open in a world. 0 = no limit.", 0.0D, 0.0D, 120.0D, 10.0D, " fps"));
        this.entityDistance = this.add(new NumberSetting("Entity Distance", "Hide mobs and items further than this (players are always drawn). 0 = vanilla.", 0.0D, 0.0D, 128.0D, 8.0D, " blocks"));
    }

    /** Frame-rate cap from this module, or -1 to keep the game's own limit. */
    public int getFramerateLimit(boolean focused, boolean menuOpenInWorld)
    {
        if (!focused && this.unfocusedFps.getInt() > 0)
        {
            return this.unfocusedFps.getInt();
        }

        return menuOpenInWorld && this.menuFps.getInt() > 0 ? this.menuFps.getInt() : -1;
    }

    /** Max distance in blocks for non-player entities, 0 for no limit. */
    public int getEntityDistance()
    {
        return this.entityDistance.getInt();
    }

    private void applyPreset()
    {
        GameSettings gs = this.mc.gameSettings;
        boolean max = this.preset.is("Max FPS");
        boolean quality = this.preset.is("Quality");
        gs.renderDistanceChunks = max ? 6 : (quality ? 16 : 10);
        gs.fancyGraphics = quality;
        gs.ambientOcclusion = max ? 0 : 2;
        gs.clouds = max ? 0 : (quality ? 2 : 1);
        gs.ofClouds = max ? 3 : (quality ? 2 : 1);
        gs.particleSetting = max ? 2 : (quality ? 0 : 1);
        gs.entityShadows = !max;
        gs.ofFastRender = !quality;
        gs.ofFastMath = !quality;
        gs.ofSmoothFps = false;
        gs.ofChunkUpdates = max ? 1 : 2;
        gs.ofTrees = max ? 1 : (quality ? 2 : 0);
        gs.ofBetterGrass = max ? 3 : 1;
        gs.ofDynamicLights = max ? 3 : (quality ? 2 : 1);
        gs.saveOptions();

        if (this.mc.renderGlobal != null && this.mc.world != null)
        {
            this.mc.renderGlobal.loadRenderers();
        }

        Notifications.post("Preset applied", this.preset.get() + " video settings are now active.", Notifications.Kind.SUCCESS);
    }
}
