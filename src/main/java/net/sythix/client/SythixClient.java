package net.sythix.client;

import java.io.File;
import net.minecraft.client.Minecraft;
import net.sythix.client.config.ConfigManager;
import net.sythix.client.config.FileConfigStorage;
import net.sythix.client.input.ClickCounter;
import net.sythix.client.module.ModuleManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry point for all Sythix Client features. Minecraft calls into this class from a few small
 * hooks (startup, input, HUD rendering, shutdown).
 */
public final class SythixClient
{
    public static final String NAME = "Sythix";
    public static final String FULL_NAME = NAME + " Client";
    public static final String VERSION = "0.2.0";
    public static final String MINECRAFT_VERSION = "1.12";

    private static final Logger LOGGER = LogManager.getLogger();
    private static SythixClient instance;

    private final Minecraft mc;
    private final ModuleManager moduleManager;
    private final ClickCounter clickCounter;
    private final ConfigManager configManager;

    private SythixClient(Minecraft mc)
    {
        this.mc = mc;
        this.clickCounter = new ClickCounter();
        this.moduleManager = new ModuleManager();
        this.configManager = new ConfigManager(new FileConfigStorage(new File(new File(mc.mcDataDir, "sythix"), "config.json")), this.moduleManager);
    }

    /** Called once during game startup, after the game window and HUD are created. */
    public static void init(Minecraft mc)
    {
        if (instance == null)
        {
            instance = new SythixClient(mc);
            instance.configManager.load();
            LOGGER.info("[Sythix] {} {} started", FULL_NAME, VERSION);
        }
    }

    public static SythixClient get()
    {
        return instance;
    }

    public static String getWindowTitle()
    {
        return FULL_NAME + " " + VERSION;
    }

    public static String getBrandingLine()
    {
        return FULL_NAME + " v" + VERSION + " | Minecraft " + MINECRAFT_VERSION;
    }

    public ModuleManager getModuleManager()
    {
        return this.moduleManager;
    }

    public ClickCounter getClickCounter()
    {
        return this.clickCounter;
    }

    public ConfigManager getConfigManager()
    {
        return this.configManager;
    }


    public void save()
    {
        this.configManager.save();
    }
}
