package net.sythix.client.ui;

/**
 * HSB/RGB conversion without java.awt, so it also works in a browser build.
 */
public final class ColorUtil
{
    private ColorUtil()
    {
    }

    /** Returns 0xRRGGBB for hue, saturation, brightness in [0, 1]. */
    public static int hsbToRgb(float hue, float saturation, float brightness)
    {
        float h = (hue - (float)Math.floor(hue)) * 6.0F;
        int sector = (int)h;
        float f = h - sector;
        float p = brightness * (1.0F - saturation);
        float q = brightness * (1.0F - saturation * f);
        float t = brightness * (1.0F - saturation * (1.0F - f));
        float r;
        float g;
        float b;

        switch (sector)
        {
            case 0: r = brightness; g = t; b = p; break;
            case 1: r = q; g = brightness; b = p; break;
            case 2: r = p; g = brightness; b = t; break;
            case 3: r = p; g = q; b = brightness; break;
            case 4: r = t; g = p; b = brightness; break;
            default: r = brightness; g = p; b = q; break;
        }

        return (int)(r * 255.0F + 0.5F) << 16 | (int)(g * 255.0F + 0.5F) << 8 | (int)(b * 255.0F + 0.5F);
    }

    /** Returns {hue, saturation, brightness} for a 0xRRGGBB colour. */
    public static float[] rgbToHsb(int rgb)
    {
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        float brightness = (float)max / 255.0F;
        float saturation = max == 0 ? 0.0F : (float)(max - min) / (float)max;
        float hue = 0.0F;

        if (max != min)
        {
            float d = (float)(max - min);

            if (max == r)
            {
                hue = (float)(g - b) / d;
            }
            else if (max == g)
            {
                hue = 2.0F + (float)(b - r) / d;
            }
            else
            {
                hue = 4.0F + (float)(r - g) / d;
            }

            hue /= 6.0F;

            if (hue < 0.0F)
            {
                hue += 1.0F;
            }
        }

        return new float[] {hue, saturation, brightness};
    }
}
