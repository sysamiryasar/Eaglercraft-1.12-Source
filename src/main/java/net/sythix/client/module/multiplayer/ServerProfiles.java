package net.sythix.client.module.multiplayer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ServerData;
import net.sythix.client.config.ConfigProfiles;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.InfoSetting;

/**
 * #44 Use a different settings profile (and HUD layout) on each server.
 */
public class ServerProfiles extends Module
{
    private final Map<String, String> mapping = new LinkedHashMap<String, String>();
    private String applied;

    public ServerProfiles()
    {
        super("Server Profiles", "Automatically switch to a saved settings profile for each server.", Category.MULTIPLAYER, true);
        this.add(new InfoSetting("This Server", "The server you are on now.", () -> this.address() == null ? "Not on a server" : this.address()));
        this.add(new InfoSetting("Its Profile", "Profile used on this server.", () -> this.address() == null || !this.mapping.containsKey(this.address()) ? "None" : this.mapping.get(this.address())));
        this.add(new ButtonSetting("Save For This Server", "Save your current settings as a profile and use it whenever you join this server.", "Save", this::saveForServer));
        this.add(new ButtonSetting("Remove For This Server", "Stop using a special profile here.", "Remove", () -> {
            if (this.address() != null)
            {
                this.mapping.remove(this.address());
            }
        }));
        this.add(new InfoSetting("Mapped Servers", "How many servers have their own profile.", () -> String.valueOf(this.mapping.size())));
    }

    private String address()
    {
        ServerData data = this.mc.getCurrentServerData();
        return this.mc.isSingleplayer() || data == null ? null : data.serverIP.toLowerCase();
    }

    private void saveForServer()
    {
        String address = this.address();

        if (address == null)
        {
            Notifications.post("Not on a server", "Join a server first.", Notifications.Kind.WARNING);
            return;
        }

        String profile = ConfigProfiles.clean("server-" + address.replace(':', '_'));

        try
        {
            ConfigProfiles.save(profile);
            this.mapping.put(address, profile);
            Notifications.post("Server profile saved", "Used automatically on " + address + ".", Notifications.Kind.SUCCESS);
        }
        catch (Exception e)
        {
            Notifications.post("Could not save profile", String.valueOf(e.getMessage()), Notifications.Kind.ERROR);
        }
    }

    public void onWorldJoin()
    {
        String address = this.address();

        if (address != null && this.mapping.containsKey(address) && !address.equals(this.applied))
        {
            this.applied = address;
            String profile = this.mapping.get(address);

            try
            {
                Map<String, String> keep = new LinkedHashMap<String, String>(this.mapping);
                ConfigProfiles.load(profile);
                this.mapping.clear();
                this.mapping.putAll(keep);
                Notifications.post("Server profile", "Loaded \"" + profile + "\" for " + address + ".", Notifications.Kind.INFO);
            }
            catch (Exception e)
            {
                Notifications.post("Server profile missing", "Profile \"" + profile + "\" could not be loaded.", Notifications.Kind.WARNING);
            }
        }
    }

    public JsonObject toJson()
    {
        JsonObject json = super.toJson();
        JsonObject map = new JsonObject();

        for (Map.Entry<String, String> e : this.mapping.entrySet())
        {
            map.addProperty(e.getKey(), e.getValue());
        }

        json.add("servers", map);
        return json;
    }

    public void fromJson(JsonObject json)
    {
        super.fromJson(json);

        if (json.has("servers") && json.get("servers").isJsonObject())
        {
            this.mapping.clear();

            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("servers").entrySet())
            {
                this.mapping.put(e.getKey(), e.getValue().getAsString());
            }
        }
    }
}
