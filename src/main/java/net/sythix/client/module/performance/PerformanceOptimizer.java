package net.sythix.client.module.performance;

import net.minecraft.client.settings.GameSettings;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.InfoSetting;
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
    private final NumberSetting benchSeconds;
    private final BooleanSetting benchSpin;
    private final BooleanSetting adaptive;
    private final NumberSetting targetFps;
    private final NumberSetting minDistance;
    private int adaptiveBase = -1;
    private int adaptiveSet = -1;
    private int ticks;

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
        this.add(new GameOptionSetting("Do more chunk updates while you stand still.", GameSettings.Options.CHUNK_UPDATES_DYNAMIC));
        this.adaptive = this.add(new BooleanSetting("Adaptive Render Distance", "Lower the render distance when FPS drops below the target, and raise it again when there is room.", false));
        this.targetFps = this.add(new NumberSetting("Target FPS", "FPS the adaptive render distance tries to keep.", 60.0D, 30.0D, 240.0D, 10.0D, " fps")).visibleWhen(this.adaptive::isOn);
        this.minDistance = this.add(new NumberSetting("Lowest Distance", "Adaptive render distance never goes below this.", 4.0D, 2.0D, 16.0D, 1.0D, " chunks")).visibleWhen(this.adaptive::isOn);
        this.add(new InfoSetting("Adaptive Now", "Render distance chosen by adaptive mode.", () -> this.adaptiveSet < 0 ? "Not active" : this.adaptiveSet + " of " + this.adaptiveBase + " chunks")).visibleWhen(this.adaptive::isOn);
        this.benchSeconds = this.add(new NumberSetting("Benchmark Length", "How long the benchmark measures.", 20.0D, 10.0D, 60.0D, 5.0D, " s"));
        this.benchSpin = this.add(new BooleanSetting("Benchmark Camera Turn", "Turn the camera once around during the benchmark for a fair result (singleplayer only).", true));
        this.add(new ButtonSetting("Run Benchmark", "Measure your FPS with the current settings (in a world).", "Run", () -> Benchmark.start(this.benchSeconds.getInt(), this.benchSpin.isOn(), this.mc.gameSettings.renderDistanceChunks + " chunks")));
        this.add(new ButtonSetting("Benchmark Results", "Compare your benchmark results.", "Open", () -> this.mc.displayGuiScreen(new GuiBenchmarks(this.mc.currentScreen))));
        this.add(new ButtonSetting("Free Memory", "Ask Java to clean up unused memory now.", "Clean", () -> {
            Runtime rt = Runtime.getRuntime();
            long before = rt.totalMemory() - rt.freeMemory();
            System.gc();
            long freed = Math.max(0L, before - (rt.totalMemory() - rt.freeMemory())) / 1048576L;
            Notifications.post("Memory cleaned", freed + " MB freed.", Notifications.Kind.SUCCESS);
        }));
    }

    public net.minecraft.client.gui.GuiScreen createScreen(net.minecraft.client.gui.GuiScreen parent)
    {
        return new GuiBenchmarks(parent);
    }

    public void onTick()
    {
        if (!this.adaptive.isOn() || this.mc.world == null || Benchmark.isRunning())
        {
            this.restoreAdaptive();
            return;
        }

        GameSettings gs = this.mc.gameSettings;

        // The player changed the render distance themselves: that becomes the new maximum
        if (this.adaptiveSet < 0 || gs.renderDistanceChunks != this.adaptiveSet)
        {
            this.adaptiveBase = gs.renderDistanceChunks;
            this.adaptiveSet = gs.renderDistanceChunks;
        }

        if (++this.ticks < 60)
        {
            return;
        }

        this.ticks = 0;
        int fps = net.minecraft.client.Minecraft.getDebugFPS();
        int target = this.targetFps.getInt();
        int rd = this.adaptiveSet;

        if (fps < target * 9 / 10 && rd > this.minDistance.getInt())
        {
            --rd;
        }
        else if (fps > target * 13 / 10 && rd < this.adaptiveBase)
        {
            ++rd;
        }

        if (rd != this.adaptiveSet)
        {
            this.adaptiveSet = rd;
            gs.setOptionFloatValue(GameSettings.Options.RENDER_DISTANCE, (float)rd);
        }
    }

    protected void onDisable()
    {
        this.restoreAdaptive();
    }

    /** Puts the player's own render distance back. */
    private void restoreAdaptive()
    {
        if (this.adaptiveSet >= 0)
        {
            if (this.mc.gameSettings.renderDistanceChunks == this.adaptiveSet && this.adaptiveBase > 0 && this.adaptiveSet != this.adaptiveBase)
            {
                this.mc.gameSettings.setOptionFloatValue(GameSettings.Options.RENDER_DISTANCE, (float)this.adaptiveBase);
            }

            this.adaptiveSet = -1;
        }
    }

    /** Render distance to save in options.txt (the player's own value, not the adaptive one). */
    public int savedRenderDistance(int current)
    {
        return this.adaptiveSet >= 0 && current == this.adaptiveSet && this.adaptiveBase > 0 ? this.adaptiveBase : current;
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
