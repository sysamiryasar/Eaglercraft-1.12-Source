package net.sythix.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Upgrades configs saved by older Sythix versions (renamed modules keep their settings).
 */
public final class ConfigMigration
{
    public static final int CURRENT = 2;
    private static final String[][] RENAMES = new String[][] {{"FPS", "Performance Monitor"}, {"CPS", "CPS Counter"}, {"Ping", "Connection Monitor"}};

    private ConfigMigration()
    {
    }

    public static JsonObject migrate(JsonObject root)
    {
        int version = root.has("version") ? root.get("version").getAsInt() : 1;

        if (version < 2 && root.has("modules") && root.get("modules").isJsonObject())
        {
            JsonObject modules = root.getAsJsonObject("modules");

            for (String[] rename : RENAMES)
            {
                JsonElement old = modules.get(rename[0]);

                if (old != null && !modules.has(rename[1]))
                {
                    modules.add(rename[1], old);
                    modules.remove(rename[0]);
                }
            }
        }

        root.addProperty("version", Integer.valueOf(CURRENT));
        return root;
    }
}
