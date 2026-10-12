package net.sythix.client.module;

/**
 * Module groups, matching the Sythix module list (sections A to J).
 */
public enum Category
{
    PERFORMANCE("Performance"),
    PVP("PvP & Combat"),
    MOVEMENT("Movement"),
    INVENTORY("Inventory"),
    INTERFACE("HUD & Interface"),
    VISUALS("Visuals"),
    MULTIPLAYER("Multiplayer"),
    RECORDING("Rec & Stats"),
    CLIENT("Client"),
    ADVANCED("Advanced");

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
