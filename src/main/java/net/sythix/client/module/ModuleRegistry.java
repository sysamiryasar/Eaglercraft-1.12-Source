package net.sythix.client.module;

import net.sythix.client.module.client.SearchDiscovery;
import net.sythix.client.module.client.Accessibility;
import net.sythix.client.module.client.HelpCenter;
import net.sythix.client.module.client.KeybindManager;
import net.sythix.client.module.client.ProfileManager;
import net.sythix.client.module.client.SettingsManager;
import net.sythix.client.module.advanced.BrowserCompatibility;
import net.sythix.client.module.advanced.DeveloperTools;
import net.sythix.client.module.advanced.Diagnostics;
import net.sythix.client.module.advanced.ErrorHandler;
import net.sythix.client.module.advanced.Localization;
import net.sythix.client.module.advanced.PluginFramework;
import net.sythix.client.module.advanced.SafeMode;
import net.sythix.client.module.advanced.UpdateManager;
import net.sythix.client.module.recording.PracticeTools;
import net.sythix.client.module.recording.RecordingTools;
import net.sythix.client.module.recording.ReplaySystem;
import net.sythix.client.module.recording.ScreenshotManager;
import net.sythix.client.module.recording.StatisticsTracker;
import net.sythix.client.module.hud.ArrayListModule;
import net.sythix.client.module.multiplayer.ConnectionMonitor;
import net.sythix.client.module.multiplayer.FriendsManager;
import net.sythix.client.module.multiplayer.PartyTools;
import net.sythix.client.module.multiplayer.ServerManagerModule;
import net.sythix.client.module.multiplayer.ServerProfiles;
import net.sythix.client.module.advanced.PrivacyControls;
import net.sythix.client.module.visuals.BlockVisuals;
import net.sythix.client.module.visuals.CosmeticsModule;
import net.sythix.client.module.visuals.EnvironmentVisuals;
import net.sythix.client.module.visuals.SoundController;
import net.sythix.client.module.visuals.TexturePackManager;
import net.sythix.client.module.hud.ChatModule;
import net.sythix.client.module.hud.CpsModule;
import net.sythix.client.module.hud.HudEditorModule;
import net.sythix.client.module.hud.InterfaceThemeModule;
import net.sythix.client.module.hud.MainMenuModule;
import net.sythix.client.module.hud.NotificationsModule;
import net.sythix.client.module.hud.ScoreboardModule;
import net.sythix.client.module.hud.TabListModule;
import net.sythix.client.module.multiplayer.ChatUtilities;
import net.sythix.client.module.inventory.InventoryEnhancements;
import net.sythix.client.module.inventory.InventoryHud;
import net.sythix.client.module.inventory.ItemCounters;
import net.sythix.client.module.inventory.ItemInformation;
import net.sythix.client.module.inventory.WorldInformation;
import net.sythix.client.module.movement.CameraTools;
import net.sythix.client.module.movement.CompassHud;
import net.sythix.client.module.movement.CoordinatesHud;
import net.sythix.client.module.movement.MovementHud;
import net.sythix.client.module.movement.WaypointsModule;
import net.sythix.client.module.hud.KeystrokesModule;
import net.sythix.client.module.performance.AnimationController;
import net.sythix.client.module.performance.GraphicsController;
import net.sythix.client.module.performance.ParticleController;
import net.sythix.client.module.performance.PerformanceMonitor;
import net.sythix.client.module.performance.PerformanceOptimizer;
import net.sythix.client.module.performance.ResourceManagerModule;
import net.sythix.client.module.pvp.ArmorHud;
import net.sythix.client.module.pvp.CombatAnimations;
import net.sythix.client.module.pvp.CombatHud;
import net.sythix.client.module.pvp.CombatTimers;
import net.sythix.client.module.pvp.CrosshairModule;
import net.sythix.client.module.pvp.MovementControls;
import net.sythix.client.module.pvp.PotionHud;
import net.sythix.client.module.pvp.ViewmodelModule;

/**
 * The full module list, in the order shown in the mod menu (sections A to J).
 */
final class ModuleRegistry
{
    private ModuleRegistry()
    {
    }

    static void registerAll(ModuleManager m)
    {
        // A. Performance and rendering
        m.register(new PerformanceOptimizer());
        m.register(new AnimationController());
        m.register(new ParticleController());
        m.register(new GraphicsController());
        m.register(new ResourceManagerModule());
        m.register(new PerformanceMonitor());
        // B. PvP and combat
        m.register(new KeystrokesModule());
        m.register(new CpsModule());
        m.register(new CrosshairModule());
        m.register(new CombatHud());
        m.register(new CombatAnimations());
        m.register(new ViewmodelModule());
        m.register(new ArmorHud());
        m.register(new PotionHud());
        m.register(new CombatTimers());
        m.register(new MovementControls());
        m.register(new net.sythix.client.module.pvp.CrystalFlowAnalyzer());
        m.register(new net.sythix.client.module.pvp.TrainingLab());
        // C. Movement and navigation
        m.register(new CameraTools());
        m.register(new MovementHud());
        m.register(new CoordinatesHud());
        m.register(new CompassHud());
        m.register(new WaypointsModule());
        // D. Inventory and items
        m.register(new InventoryHud());
        m.register(new ItemCounters());
        m.register(new ItemInformation());
        m.register(new InventoryEnhancements());
        m.register(new WorldInformation());
        // E. HUD and interface
        m.register(new HudEditorModule());
        m.register(new net.sythix.client.module.hud.SmartHudLayout());
        m.register(new ScoreboardModule());
        m.register(new TabListModule());
        m.register(new ChatModule());
        m.register(new NotificationsModule());
        m.register(new ArrayListModule());
        m.register(new InterfaceThemeModule());
        m.register(new MainMenuModule());
        // F. Visuals and cosmetics
        m.register(new EnvironmentVisuals());
        m.register(new BlockVisuals());
        m.register(new TexturePackManager());
        m.register(new CosmeticsModule());
        m.register(new SoundController());
        // G. Multiplayer and servers
        m.register(new ServerManagerModule());
        m.register(new ConnectionMonitor());
        m.register(new FriendsManager());
        m.register(new PartyTools());
        m.register(new ServerProfiles());
        m.register(new ChatUtilities());
        // H. Recording and statistics
        m.register(new ScreenshotManager());
        m.register(new RecordingTools());
        m.register(new ReplaySystem());
        m.register(new net.sythix.client.module.recording.CombatReplay());
        m.register(new StatisticsTracker());
        m.register(new net.sythix.client.module.recording.DuelAnalytics());
        m.register(new PracticeTools());
        // I. Client configuration
        m.register(new KeybindManager());
        m.register(new ProfileManager());
        m.register(new SettingsManager());
        m.register(new SearchDiscovery());
        m.register(new Accessibility());
        m.register(new HelpCenter());
        // J. Advanced and system
        m.register(new Diagnostics());
        m.register(new ErrorHandler());
        m.register(new SafeMode());
        m.register(new UpdateManager());
        m.register(new PluginFramework());
        m.register(new DeveloperTools());
        m.register(new PrivacyControls());
        m.register(new BrowserCompatibility());
        m.register(new Localization());
        // Content backported from Minecraft 1.13-1.21
        m.register(new net.sythix.modern.ModernContent());
    }
}
