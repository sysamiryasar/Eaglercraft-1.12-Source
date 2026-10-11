package net.sythix.client.module;

import net.sythix.client.module.client.SearchDiscovery;
import net.sythix.client.module.hud.CpsModule;
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
        // I. Client configuration
        m.register(new SearchDiscovery());
    }
}
