package net.sythix.client.module.client;

import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.InfoSetting;

/**
 * #54 How the mod menu searches and lists modules (categories, favourites, descriptions).
 */
public class SearchDiscovery extends Module
{
    private final BooleanSetting searchDescriptions;
    private final BooleanSetting favoritesFirst;

    public SearchDiscovery()
    {
        super("Search and Discovery", "Search modules, categories, favourites and descriptions in the mod menu.", Category.CLIENT, true);
        this.setCore();
        this.searchDescriptions = this.add(new BooleanSetting("Search Descriptions", "Match your search against module descriptions too.", true));
        this.favoritesFirst = this.add(new BooleanSetting("Favourites First", "List favourite modules before the others.", true));
        this.add(new InfoSetting("Tip", "How to favourite a module.", () -> "Click the star on a module card"));
    }

    public boolean searchDescriptions()
    {
        return this.searchDescriptions.isOn();
    }

    public boolean favoritesFirst()
    {
        return this.favoritesFirst.isOn();
    }
}
