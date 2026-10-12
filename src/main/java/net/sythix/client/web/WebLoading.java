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

    private static native void layout(int logoX, int logoY, int logoSize, int barY, int barWidth, int barHeight);

    /**
     * Where the game's own loading screen draws the logo and bar, as fractions of the window (x and
     * widths of its width, y and heights of its height), so the page's loading screen can move to
     * exactly the same spot before it hands over to the game.
     */
    public static void reportLayout(float logoX, float logoY, float logoSize, float barY, float barWidth, float barHeight)
    {
        if (broken)
        {
            return;
        }

        try
        {
            layout(Math.round(logoX * 10000.0F), Math.round(logoY * 10000.0F), Math.round(logoSize * 10000.0F), Math.round(barY * 10000.0F), Math.round(barWidth * 10000.0F), Math.round(barHeight * 10000.0F));
        }
        catch (Throwable e)
        {
            // an older page: it just keeps its own layout
        }
    }

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
