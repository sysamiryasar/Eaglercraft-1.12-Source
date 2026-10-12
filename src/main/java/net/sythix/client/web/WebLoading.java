package net.sythix.client.web;

/**
 * Browser only: tells the page's loading screen (web/sythix-web.js) how far the game's startup is,
 * so the Sythix loading screen shows real progress from the Play button to the main menu.
 */
public final class WebLoading
{
    private static boolean broken;

    private WebLoading()
    {
    }

    private static native void step(int percent, String text);

    /** percent 0-100 (100 hides the page's loading screen); text is shown under the bar. */
    public static void report(int percent, String text)
    {
        if (broken)
        {
            return;
        }

        try
        {
            step(percent, text);
        }
        catch (Throwable e)
        {
            // an older page without the loading screen: the game itself still starts
            broken = true;
        }
    }
}
