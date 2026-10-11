package net.sythix.client.module;

public enum Category
{
    HUD("HUD"),
    PVP("PvP"),
    VISUAL("Visual"),
    PERFORMANCE("Performance"),
    COSMETIC("Cosmetics");

    private final String displayName;

    Category(String displayName)
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return this.displayName;
    }
}
