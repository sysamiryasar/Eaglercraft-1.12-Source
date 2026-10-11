package net.sythix.client.plugin;

import net.sythix.client.module.ModuleManager;

/**
 * Interface for Sythix plugins (desktop only).
 *
 * A plugin is a .jar in .minecraft/sythix/plugins with a file
 * META-INF/services/net.sythix.client.plugin.SythixPlugin naming its implementation class.
 * Plugins run with the same rights as the game, so only install plugins you trust and that
 * follow the rules of the servers you play on.
 */
public interface SythixPlugin
{
    /** Shown in the plugin list. */
    String getName();

    String getVersion();

    /** Called once at startup. Register your modules here with modules.register(...). */
    void onLoad(ModuleManager modules);
}
