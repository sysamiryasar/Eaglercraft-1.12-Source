package net.sythix.client.module;

import net.sythix.client.module.client.SearchDiscovery;
import net.sythix.client.module.hud.ArrayListModule;
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
        m.register(new ScoreboardModule());
        m.register(new TabListModule());
        m.register(new ChatModule());
        m.register(new NotificationsModule());
        m.register(new ArrayListModule());
        m.register(new InterfaceThemeModule());
        m.register(new MainMenuModule());
        // G. Multiplayer (partial, more below)
        m.register(new ChatUtilities());
        // I. Client configuration
        m.register(new SearchDiscovery());
    }
}
