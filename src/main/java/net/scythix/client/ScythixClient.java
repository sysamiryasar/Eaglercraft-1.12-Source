package net.scythix.client;

/**
 * Central place for ScythiX Client branding. Change the name or version here and it updates everywhere.
 */
public final class ScythixClient
{
    public static final String NAME = "ScythiX";
    public static final String FULL_NAME = NAME + " Client";
    public static final String VERSION = "0.1.0";
    public static final String MINECRAFT_VERSION = "1.12";

    private ScythixClient()
    {
    }

    public static String getWindowTitle()
    {
        return FULL_NAME + " " + VERSION;
    }

    public static String getBrandingLine()
    {
        return FULL_NAME + " v" + VERSION + " | Minecraft " + MINECRAFT_VERSION;
    }
}
