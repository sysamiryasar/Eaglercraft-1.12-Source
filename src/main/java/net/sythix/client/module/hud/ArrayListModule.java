package net.sythix.client.module.hud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.sythix.client.SythixClient;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ColorSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.ui.SythixTheme;

/**
 * #32 A list of the modules you have switched on.
 */
public class ArrayListModule extends HudModule
{
    private final ModeSetting sort;
    private final ModeSetting colorMode;
    private final ColorSetting color;
    private final BooleanSetting showCategory;
    private final BooleanSetting background;
    private final BooleanSetting sideBar;
    private final BooleanSetting animations;
    private final BooleanSetting includeHud;
    private final ModeSetting align;
    private final Map<Module, Float> slide = new HashMap<Module, Float>();
    private long lastFrame;

    public ArrayListModule()
    {
        super("Array List", "A list of the modules that are switched on.", Category.INTERFACE, false, 1.0D, 0.0D);
        this.sort = this.add(new ModeSetting("Sort", "Order of the list.", "Length", "Length", "Alphabetical", "Category"));
        this.colorMode = this.add(new ModeSetting("Colors", "How entries are coloured.", "Gradient", "Gradient", "Chroma", "Single"));
        this.color = this.add(new ColorSetting("Color", "Colour for Single mode.", 0xFFD81820, false));
        this.showCategory = this.add(new BooleanSetting("Show Category", "Add the category after each name.", false));
        this.background = this.add(new BooleanSetting("Background", "Dark box behind each entry.", true));
        this.sideBar = this.add(new BooleanSetting("Side Bar", "Coloured bar beside each entry.", true));
        this.animations = this.add(new BooleanSetting("Animations", "Slide entries in and out.", true));
        this.includeHud = this.add(new BooleanSetting("Include HUD Elements", "Also list HUD elements such as Keystrokes.", false));
        this.align = this.add(new ModeSetting("Align", "Which side the text lines up on.", "Right", "Right", "Left"));
    }

    private List<Module> entries()
    {
        List<Module> list = new ArrayList<Module>();

        for (Module module : SythixClient.get().getModuleManager().getModules())
        {
            if (module.isEnabled() && !module.isCore() && module != this && (this.includeHud.isOn() || !(module instanceof HudModule)))
            {
                list.add(module);
            }
        }

        FontRenderer font = this.mc.fontRenderer;

        if (this.sort.is("Alphabetical"))
        {
            list.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        }
        else if (this.sort.is("Category"))
        {
            list.sort((a, b) -> a.getCategory() == b.getCategory() ? a.getName().compareToIgnoreCase(b.getName()) : a.getCategory().compareTo(b.getCategory()));
        }
        else
        {
            list.sort((a, b) -> font.getStringWidth(this.label(b)) - font.getStringWidth(this.label(a)));
        }

        return list;
    }

    private String label(Module module)
    {
        return this.showCategory.isOn() ? module.getName() + " §7" + module.getCategory().getDisplayName() : module.getName();
    }

    public int getWidth()
    {
        int w = 40;

        for (Module module : this.entries())
        {
            w = Math.max(w, this.mc.fontRenderer.getStringWidth(this.label(module)) + 6);
        }

        return w;
    }

    public int getHeight()
    {
        return Math.max(1, this.entries().size()) * 10;
    }

    protected void renderContent(boolean preview)
    {
        long now = Minecraft.getSystemTime();
        float seconds = this.lastFrame == 0L ? 1.0F : Math.min(0.1F, (float)(now - this.lastFrame) / 1000.0F);
        this.lastFrame = now;
        FontRenderer font = this.mc.fontRenderer;
        List<Module> list = this.entries();
        int width = this.getWidth();
        int y = 0;

        for (int i = 0; i < list.size(); ++i)
        {
            Module module = list.get(i);
            float s = this.slide.containsKey(module) ? this.slide.get(module).floatValue() : 0.0F;
            s = this.animations.isOn() && !SythixTheme.reducedMotion ? SythixTheme.approach(s, 1.0F, 6.0F, seconds) : 1.0F;
            this.slide.put(module, Float.valueOf(s));
            String text = this.label(module);
            int tw = font.getStringWidth(text) + 6;
            boolean right = this.align.is("Right");
            float x = right ? (float)(width - tw) + (1.0F - s) * (float)tw : -(1.0F - s) * (float)tw;
            int c;

            if (this.colorMode.is("Chroma"))
            {
                c = 0xFF000000 | net.sythix.client.ui.ColorUtil.hsbToRgb((float)(now % 4000L) / 4000.0F + (float)i * 0.06F, 0.7F, 1.0F);
            }
            else if (this.colorMode.is("Gradient"))
            {
                c = SythixTheme.lerpColor(SythixTheme.RED, SythixTheme.LIGHT_BLUE, list.size() <= 1 ? 0.0F : (float)i / (float)(list.size() - 1));
            }
            else
            {
                c = this.color.getColor();
            }

            if (this.background.isOn())
            {
                SythixTheme.drawRect(x, (float)y, x + (float)tw, (float)(y + 10), 0x9016161B);
            }

            if (this.sideBar.isOn())
            {
                float bx = right ? x + (float)tw - 1.0F : x;
                SythixTheme.drawRect(bx, (float)y, bx + 1.0F, (float)(y + 10), c);
            }

            font.drawString(text, x + 3.0F, (float)(y + 1), c, true);
            y += 10;
        }

        this.slide.keySet().retainAll(list);
    }
}
