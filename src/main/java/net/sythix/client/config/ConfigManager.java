package net.sythix.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.sythix.client.module.Module;
import net.sythix.client.module.ModuleManager;
import net.sythix.client.ui.SythixTheme;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Saves and loads all Sythix settings as JSON.
 */
public class ConfigManager
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int VERSION = 1;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ConfigStorage storage;
    private final ModuleManager modules;

    public ConfigManager(ConfigStorage storage, ModuleManager modules)
    {
        this.storage = storage;
        this.modules = modules;
    }

    public void load()
    {
        try
        {
            String text = this.storage.read();

            if (text == null)
            {
                LOGGER.info("[Sythix] No config yet, using defaults");
                return;
            }

            JsonObject root = new JsonParser().parse(text).getAsJsonObject();

            if (root.has("theme") && root.get("theme").isJsonObject())
            {
                JsonObject theme = root.getAsJsonObject("theme");

                if (theme.has("enabled"))
                {
                    SythixTheme.enabled = theme.get("enabled").getAsBoolean();
                }
            }

            if (root.has("modules") && root.get("modules").isJsonObject())
            {
                JsonObject saved = root.getAsJsonObject("modules");

                for (Module module : this.modules.getModules())
                {
                    JsonElement element = saved.get(module.getName());

                    if (element != null && element.isJsonObject())
                    {
                        module.fromJson(element.getAsJsonObject());
                    }
                }
            }

            LOGGER.info("[Sythix] Config loaded");
        }
        catch (Exception e)
        {
            // A broken file must never stop the game from starting; defaults are used instead
            LOGGER.error("[Sythix] Could not read config, using defaults", e);
        }
    }

    public void save()
    {
        JsonObject root = new JsonObject();
        root.addProperty("version", Integer.valueOf(VERSION));
        JsonObject theme = new JsonObject();
        theme.addProperty("enabled", Boolean.valueOf(SythixTheme.enabled));
        root.add("theme", theme);
        JsonObject saved = new JsonObject();

        for (Module module : this.modules.getModules())
        {
            saved.add(module.getName(), module.toJson());
        }

        root.add("modules", saved);

        try
        {
            this.storage.write(this.gson.toJson(root));
        }
        catch (Exception e)
        {
            LOGGER.error("[Sythix] Could not save config", e);
        }
    }
}
