package net.sythix.client.module.visuals;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.ResourcePackRepository;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.KeybindSetting;
import net.sythix.client.ui.GuiTexturePacks;

/**
 * #37 Resource pack switching, favourites, previews and compatibility checks.
 */
public class TexturePackManager extends Module
{
    private final Set<String> favorites = new LinkedHashSet<String>();
    private final KeybindSetting cycleKey;
    private final BooleanSetting warnIncompatible;
    private int cycleIndex = -1;

    public TexturePackManager()
    {
        super("Texture Pack Manager", "Switch packs, mark favourites, preview them and check compatibility.", Category.VISUALS, true);
        this.add(new ButtonSetting("Pack Manager", "Open the texture pack manager.", "Open", () -> this.mc.displayGuiScreen(new GuiTexturePacks(this.mc.currentScreen))));
        this.cycleKey = this.add(new KeybindSetting("Cycle Favourites Key", "Switch between your favourite packs in game (and no pack).", 0));
        this.warnIncompatible = this.add(new BooleanSetting("Compatibility Warnings", "Mark packs made for other Minecraft versions.", true));
    }

    public GuiScreen createScreen(GuiScreen parent)
    {
        return new GuiTexturePacks(parent);
    }

    public boolean warnIncompatible()
    {
        return this.warnIncompatible.isOn();
    }

    public boolean isFavorite(String pack)
    {
        return this.favorites.contains(pack);
    }

    public void toggleFavorite(String pack)
    {
        if (!this.favorites.remove(pack))
        {
            this.favorites.add(pack);
        }
    }

    /** Short compatibility note for a pack format, or null when it is made for this version. */
    public static String compatibility(int packFormat)
    {
        if (packFormat == 3)
        {
            return null;
        }

        if (packFormat < 3)
        {
            return "Made for an older version (usually works)";
        }

        return "Made for a newer version (1.13+ file names differ - may look wrong)";
    }

    /** Makes exactly these packs active (first = lowest priority) and reloads resources. */
    public void apply(List<ResourcePackRepository.Entry> active)
    {
        ResourcePackRepository repo = this.mc.getResourcePackRepository();
        repo.setRepositories(active);
        this.mc.gameSettings.resourcePacks.clear();
        this.mc.gameSettings.incompatibleResourcePacks.clear();

        for (ResourcePackRepository.Entry entry : active)
        {
            this.mc.gameSettings.resourcePacks.add(entry.getResourcePackName());

            if (entry.getPackFormat() != 3)
            {
                this.mc.gameSettings.incompatibleResourcePacks.add(entry.getResourcePackName());
            }
        }

        this.mc.gameSettings.saveOptions();
        this.mc.refreshResources();
    }

    public void onKey(int keyCode)
    {
        if (keyCode == 0 || keyCode != this.cycleKey.getKey())
        {
            return;
        }

        ResourcePackRepository repo = this.mc.getResourcePackRepository();
        repo.updateRepositoryEntriesAll();
        List<ResourcePackRepository.Entry> favs = new ArrayList<ResourcePackRepository.Entry>();

        for (ResourcePackRepository.Entry entry : repo.getRepositoryEntriesAll())
        {
            if (this.favorites.contains(entry.getResourcePackName()))
            {
                favs.add(entry);
            }
        }

        if (favs.isEmpty())
        {
            Notifications.post("No favourite packs", "Star packs in the Texture Pack Manager first.", Notifications.Kind.WARNING);
            return;
        }

        this.cycleIndex = this.cycleIndex + 1 > favs.size() - 1 ? -1 : this.cycleIndex + 1;
        List<ResourcePackRepository.Entry> active = new ArrayList<ResourcePackRepository.Entry>();

        if (this.cycleIndex >= 0)
        {
            active.add(favs.get(this.cycleIndex));
        }

        this.apply(active);
        Notifications.post("Texture pack", this.cycleIndex >= 0 ? favs.get(this.cycleIndex).getResourcePackName() : "Default textures", Notifications.Kind.INFO);
    }

    public JsonObject toJson()
    {
        JsonObject json = super.toJson();
        JsonArray array = new JsonArray();

        for (String f : this.favorites)
        {
            array.add(f);
        }

        json.add("favoritePacks", array);
        return json;
    }

    public void fromJson(JsonObject json)
    {
        super.fromJson(json);

        if (json.has("favoritePacks") && json.get("favoritePacks").isJsonArray())
        {
            this.favorites.clear();

            for (JsonElement e : json.getAsJsonArray("favoritePacks"))
            {
                this.favorites.add(e.getAsString());
            }
        }
    }
}
