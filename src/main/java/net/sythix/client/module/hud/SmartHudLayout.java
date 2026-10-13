package net.sythix.client.module.hud;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.ScaledResolution;
import net.sythix.client.SythixClient;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.module.ModuleManager;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;

/**
 * Arranges the HUD for you: presets for crystal PvP, cart PvP and a minimal screen, and an Auto
 * Arrange button that moves every HUD element into a free spot (never over the hotbar, health,
 * crosshair, chat or boss bar, and never on top of each other). Undo restores the last layout.
 */
public class SmartHudLayout extends Module
{
    /** Screen anchors: {x, y} as 0 = left/top, 1 = middle, 2 = right/bottom. */
    private static final int[][] ANCHORS = {{0, 0}, {2, 0}, {0, 1}, {2, 1}, {2, 2}, {1, 0}, {0, 2}, {1, 2}};

    /** Where each known element goes in the presets (index into ANCHORS). */
    private static final Map<String, Integer> PLACES = new LinkedHashMap<String, Integer>();

    static
    {
        PLACES.put("Performance Monitor", 0);
        PLACES.put("CPS Counter", 0);
        PLACES.put("Connection Monitor", 0);
        PLACES.put("Combat Timers", 5);
        PLACES.put("Compass HUD", 5);
        PLACES.put("CPvP Training Lab", 5);
        PLACES.put("Potion HUD", 1);
        PLACES.put("Crystal Flow Analyzer", 1);
        PLACES.put("Item Counters", 1);
        PLACES.put("Keystrokes", 2);
        PLACES.put("Combat HUD", 3);
        PLACES.put("Scoreboard", 3);
        PLACES.put("Armor HUD", 4);
        PLACES.put("Duel Analytics", 4);
        PLACES.put("Statistics Tracker", 4);
        PLACES.put("Coordinates HUD", 2);
        PLACES.put("Movement HUD", 2);
        PLACES.put("Practice Tools", 5);
        PLACES.put("Array List", 1);
    }

    private final ModeSetting preset;
    private final NumberSetting margin;
    private final BooleanSetting keepTidy;
    private final Map<Module, JsonObject> undo = new HashMap<Module, JsonObject>();
    /** The biggest size each element has had (text elements grow and shrink with what they show). */
    private final Map<HudModule, float[]> biggest = new HashMap<HudModule, float[]>();
    private int tidyTimer;

    public SmartHudLayout()
    {
        super("Smart HUD Layout", "One-click HUD presets for crystal and cart PvP, and auto-arranging without overlaps.", Category.INTERFACE, true);
        this.preset = this.add(new ModeSetting("Preset", "Crystal PvP and Cart PvP switch on the useful elements and place them (Cart PvP also turns on the Harlu Cart Pack); Minimal keeps only FPS, CPS and armour; Current Elements only rearranges what is on.", "Crystal PvP", "Crystal PvP", "Cart PvP", "Minimal", "Current Elements"));
        this.add(new ButtonSetting("Apply Preset", "Switch on and arrange the elements of the chosen preset.", "Apply", this::applyPreset));
        this.add(new ButtonSetting("Auto Arrange", "Move the elements that are on into free spots, keeping each near its side of the screen.", "Arrange", () -> this.arrange(false)));
        this.add(new ButtonSetting("Undo", "Put the HUD back as it was before the last preset or arrange.", "Undo", this::undo));
        this.margin = this.add(new NumberSetting("Margin", "Space between elements and the screen edges.", 3.0D, 0.0D, 12.0D, 1.0D, "px"));
        this.keepTidy = this.add(new BooleanSetting("Keep Tidy", "Every few seconds, move elements that overlap each other or the hotbar, chat or crosshair.", false));
    }

    private static ModuleManager manager()
    {
        return SythixClient.get() == null ? null : SythixClient.get().getModuleManager();
    }

    public void onTick()
    {
        if (this.mc.player != null && manager() != null)
        {
            for (HudModule h : this.visible())
            {
                float[] b = this.biggest.computeIfAbsent(h, k -> new float[2]);
                b[0] = Math.max(b[0], h.getScaledWidth());
                b[1] = Math.max(b[1], h.getScaledHeight());
            }
        }

        if (this.keepTidy.isOn() && this.mc.player != null && this.mc.currentScreen == null && ++this.tidyTimer >= 60)
        {
            this.tidyTimer = 0;

            if (this.hasOverlap())
            {
                this.arrange(true);
            }
        }
    }

    private void snapshot()
    {
        this.undo.clear();
        ModuleManager m = manager();

        for (HudModule h : m.getHudModules())
        {
            this.undo.put(h, h.toJson());
        }
    }

    private void undo()
    {
        if (this.undo.isEmpty())
        {
            Notifications.post("Smart HUD Layout", "Nothing to undo.", Notifications.Kind.INFO);
            return;
        }

        for (Map.Entry<Module, JsonObject> e : this.undo.entrySet())
        {
            e.getKey().fromJson(e.getValue());
        }

        this.undo.clear();
        Notifications.post("Smart HUD Layout", "Restored your previous HUD.", Notifications.Kind.SUCCESS);
    }

    private void applyPreset()
    {
        ModuleManager m = manager();

        if (m == null)
        {
            return;
        }

        this.snapshot();
        String p = this.preset.get();
        List<String> on = new ArrayList<String>();

        switch (p)
        {
            case "Crystal PvP":
            case "Cart PvP":
                on.add("Crystal Flow Analyzer");
                on.add("Armor HUD");
                on.add("Potion HUD");
                on.add("Combat HUD");
                on.add("CPS Counter");
                on.add("Performance Monitor");
                break;
            case "Minimal":
                on.add("Performance Monitor");
                on.add("CPS Counter");
                on.add("Armor HUD");
                break;
            default:
                break;
        }

        if (!on.isEmpty())
        {
            for (HudModule h : m.getHudModules())
            {
                // Minimal hides everything else; the PvP presets only add
                if (on.contains(h.getName()))
                {
                    h.setEnabled(true);
                }
                else if ("Minimal".equals(p) && !h.isCore() && !"Scoreboard".equals(h.getName()))
                {
                    h.setEnabled(false);
                }
            }
        }

        net.sythix.client.module.visuals.TexturePackManager packs = m.get(net.sythix.client.module.visuals.TexturePackManager.class);

        if (packs != null && "Cart PvP".equals(p))
        {
            // cart PvP: clear rails and low fire from the built-in Harlu Cart Pack
            packs.setBuiltInPack(net.sythix.client.pack.BuiltInPacks.CART_CHOICE);
        }

        Module flow = m.getByName("Crystal Flow Analyzer");

        if (flow != null && !"Minimal".equals(p) && !"Current Elements".equals(p))
        {
            for (net.sythix.client.setting.Setting<?> s : flow.getSettings())
            {
                if (s instanceof ModeSetting && s.getName().equals("Show"))
                {
                    ((ModeSetting)s).set("Cart PvP".equals(p) ? "Cart" : "Crystal");
                }
            }
        }

        this.place(true);
        Notifications.post("Smart HUD Layout", p + " layout applied. Use Undo to go back.", Notifications.Kind.SUCCESS);
    }

    /** Rearranges the visible elements; quiet = no notification (Keep Tidy). */
    private void arrange(boolean quiet)
    {
        if (manager() == null)
        {
            return;
        }

        if (!quiet)
        {
            this.snapshot();
        }

        int moved = this.place(false);

        if (!quiet)
        {
            Notifications.post("Smart HUD Layout", "Arranged " + moved + " element" + (moved == 1 ? "" : "s") + ". Use Undo to go back.", Notifications.Kind.SUCCESS);
        }
    }

    private List<HudModule> visible()
    {
        List<HudModule> list = new ArrayList<HudModule>();

        for (HudModule h : manager().getHudModules())
        {
            if (h.isEnabled() && h.shouldRender())
            {
                list.add(h);
            }
        }

        return list;
    }

    /** Areas the HUD should keep clear, in scaled GUI pixels: {x0, y0, x1, y1}. */
    private List<float[]> reserved(ScaledResolution res)
    {
        int w = res.getScaledWidth();
        int h = res.getScaledHeight();
        List<float[]> r = new ArrayList<float[]>();
        // hotbar, health, hunger, armour and air rows
        r.add(new float[] {(float)(w / 2 - 93), (float)(h - 62), (float)(w / 2 + 93), (float)h});
        // crosshair
        r.add(new float[] {(float)(w / 2 - 16), (float)(h / 2 - 16), (float)(w / 2 + 16), (float)(h / 2 + 16)});
        // chat (bottom left, about ten lines)
        int chatW = Math.min(w / 2 - 95, Math.round(320.0F * this.mc.gameSettings.chatWidth) + 6);
        r.add(new float[] {0.0F, (float)(h - 140), (float)Math.max(0, chatW), (float)(h - 30)});
        // boss bar
        r.add(new float[] {(float)(w / 2 - 95), 0.0F, (float)(w / 2 + 95), 20.0F});
        return r;
    }

    private static boolean overlaps(float[] a, float[] b)
    {
        return a[0] < b[2] && a[2] > b[0] && a[1] < b[3] && a[3] > b[1];
    }

    private static float[] box(HudModule h, ScaledResolution res)
    {
        float x = h.getScreenX(res);
        float y = h.getScreenY(res);
        return new float[] {x, y, x + h.getScaledWidth(), y + h.getScaledHeight()};
    }

    /** Whether visible elements overlap each other or the hotbar, chat, crosshair or boss bar. */
    public boolean hasOverlap()
    {
        return this.overlapReport() != null;
    }

    /** Which elements overlap (for testing), or null when the HUD is tidy. */
    public String overlapReport()
    {
        ScaledResolution res = new ScaledResolution(this.mc);
        List<float[]> boxes = new ArrayList<float[]>();
        List<String> names = new ArrayList<String>();
        String[] zones = {"the hotbar", "the crosshair", "the chat", "the boss bar"};

        for (HudModule h : this.visible())
        {
            float[] b = box(h, res);

            for (int i = 0; i < boxes.size(); ++i)
            {
                if (overlaps(b, boxes.get(i)))
                {
                    StringBuilder all = new StringBuilder();

                    for (HudModule v : this.visible())
                    {
                        float[] vb = box(v, res);
                        all.append(String.format(" [%s %.0f,%.0f %.0fx%.0f]", v.getName(), vb[0], vb[1], vb[2] - vb[0], vb[3] - vb[1]));
                    }

                    return h.getName() + " overlaps " + names.get(i) + " in " + res.getScaledWidth() + "x" + res.getScaledHeight() + ":" + all;
                }
            }

            List<float[]> reserved = this.reserved(res);

            for (int i = 0; i < reserved.size(); ++i)
            {
                if (overlaps(b, reserved.get(i)))
                {
                    return h.getName() + " overlaps " + zones[i] + String.format(" at %.0f,%.0f %.0fx%.0f", b[0], b[1], b[2] - b[0], b[3] - b[1]);
                }
            }

            boxes.add(b);
            names.add(h.getName());
        }

        return null;
    }

    /** The anchor an element belongs to: from the preset table, or the nearest one to where it is now. */
    private int anchorFor(HudModule h, ScaledResolution res, boolean preset)
    {
        if (preset && PLACES.containsKey(h.getName()))
        {
            return PLACES.get(h.getName()).intValue();
        }

        float cx = (h.getScreenX(res) + h.getScaledWidth() / 2.0F) / (float)res.getScaledWidth();
        float cy = (h.getScreenY(res) + h.getScaledHeight() / 2.0F) / (float)res.getScaledHeight();
        int ax = cx < 0.33F ? 0 : (cx > 0.67F ? 2 : 1);
        int ay = cy < 0.33F ? 0 : (cy > 0.67F ? 2 : 1);

        if (ax == 1 && ay == 1)
        {
            // nothing goes in the middle of the screen
            ax = cx < 0.5F ? 0 : 2;
        }

        for (int i = 0; i < ANCHORS.length; ++i)
        {
            if (ANCHORS[i][0] == ax && ANCHORS[i][1] == ay)
            {
                return i;
            }
        }

        return 0;
    }

    /** Places every visible element; returns how many moved. */
    private int place(boolean preset)
    {
        ScaledResolution res = new ScaledResolution(this.mc);
        // each element's side of the screen is decided once, before anything moves
        Map<HudModule, Integer> anchors = new HashMap<HudModule, Integer>();

        for (HudModule h : this.visible())
        {
            anchors.put(h, Integer.valueOf(this.anchorFor(h, res, preset)));
        }

        // elements with no free spot stay where they are; then everything else is placed again around them
        List<HudModule> fixed = new ArrayList<HudModule>();
        int moved = this.place(res, anchors, fixed, true);

        if (!fixed.isEmpty())
        {
            moved = this.place(res, anchors, fixed, false);
        }

        return moved;
    }

    private boolean big(HudModule h)
    {
        float[] size = this.size(h);
        return size[0] * size[1] >= 5000.0F;
    }

    private int place(ScaledResolution res, Map<HudModule, Integer> anchors, List<HudModule> fixed, boolean firstPass)
    {
        float sw = (float)res.getScaledWidth();
        float sh = (float)res.getScaledHeight();
        float m = this.margin.getFloat();
        List<float[]> taken = this.reserved(res);

        for (HudModule h : fixed)
        {
            float[] b = box(h, res);
            taken.add(new float[] {b[0] - m / 2.0F, b[1] - m / 2.0F, b[2] + m / 2.0F, b[3] + m / 2.0F});
        }

        // big elements first (they are the hardest to fit), then side by side in the preset order
        List<String> names = new ArrayList<String>(PLACES.keySet());
        List<HudModule> order = new ArrayList<HudModule>(anchors.keySet());
        order.removeAll(fixed);
        order.sort((x, y) -> {
            boolean bx = this.big(x);
            boolean by = this.big(y);

            if (bx != by)
            {
                return bx ? -1 : 1;
            }

            if (bx)
            {
                float[] sx = this.size(x);
                float[] sy = this.size(y);
                return Float.compare(sy[0] * sy[1], sx[0] * sx[1]);
            }

            int ax = anchors.get(x).intValue();
            int ay = anchors.get(y).intValue();

            if (ax != ay)
            {
                return ax - ay;
            }

            int px = names.contains(x.getName()) ? names.indexOf(x.getName()) : 99;
            int py = names.contains(y.getName()) ? names.indexOf(y.getName()) : 99;
            return px != py ? px - py : Float.compare(x.getScreenY(res), y.getScreenY(res));
        });
        int moved = 0;

        for (HudModule h : order)
        {
            int a = anchors.get(h).intValue();
            float[] size = this.size(h);
            float[] spot = this.findSpot(size, ANCHORS[a], sw, sh, m, taken);
            int used = a;

            // try every other anchor, then anywhere near where it is, then at its current (smaller) size
            for (int b = 0; b < ANCHORS.length && spot == null; ++b)
            {
                spot = this.findSpot(size, ANCHORS[b], sw, sh, m, taken);
                used = b;
            }

            if (spot == null)
            {
                spot = this.nearestFree(size, h.getScreenX(res), h.getScreenY(res), sw, sh, m, taken);
                used = a;
            }

            if (spot == null)
            {
                size = new float[] {h.getScaledWidth(), h.getScaledHeight()};
                spot = this.nearestFree(size, h.getScreenX(res), h.getScreenY(res), sw, sh, m, taken);
            }

            if (spot == null)
            {
                if (firstPass)
                {
                    fixed.add(h);
                }

                continue;
            }

            float oldX = h.getScreenX(res);
            float oldY = h.getScreenY(res);
            // right and bottom anchored elements grow left and up: keep their far edge on the box edge
            float x = ANCHORS[used][0] == 2 ? spot[0] + size[0] - h.getScaledWidth() : (ANCHORS[used][0] == 1 ? spot[0] + (size[0] - h.getScaledWidth()) / 2.0F : spot[0]);
            float y = ANCHORS[used][1] == 2 ? spot[1] + size[1] - h.getScaledHeight() : spot[1];
            h.setScreenPosition(x, y, res);
            taken.add(new float[] {spot[0] - m / 2.0F, spot[1] - m / 2.0F, spot[0] + size[0] + m / 2.0F, spot[1] + size[1] + m / 2.0F});

            if (Math.abs(oldX - x) > 0.5F || Math.abs(oldY - y) > 0.5F)
            {
                ++moved;
            }
        }

        return moved;
    }

    private float[] nearestFree(float[] size, float fromX, float fromY, float sw, float sh, float m, List<float[]> taken)
    {
        float[] best = null;
        float bestDist = Float.MAX_VALUE;

        for (float y = m; y + size[1] + m <= sh; y += 2.0F)
        {
            for (float x = m; x + size[0] + m <= sw; x += 2.0F)
            {
                float d = (x - fromX) * (x - fromX) + (y - fromY) * (y - fromY);

                if (d >= bestDist)
                {
                    continue;
                }

                float[] b = {x, y, x + size[0], y + size[1]};
                boolean free = true;

                for (float[] t : taken)
                {
                    if (overlaps(b, t))
                    {
                        free = false;
                        break;
                    }
                }

                if (free)
                {
                    best = new float[] {x, y};
                    bestDist = d;
                }
            }
        }

        return best;
    }

    /** The space to keep for an element: the biggest it has been (at least its size now). */
    private float[] size(HudModule h)
    {
        float[] b = this.biggest.get(h);
        float w = h.getScaledWidth();
        float hh = h.getScaledHeight();
        return b == null ? new float[] {w, hh} : new float[] {Math.max(w, b[0]), Math.max(hh, b[1])};
    }

    /** Scans outward from an anchor for a free spot: {x, y} or null. */
    private float[] findSpot(float[] size, int[] anchor, float sw, float sh, float m, List<float[]> taken)
    {
        float w = size[0];
        float hh = size[1];

        if (w + 2.0F * m > sw || hh + 2.0F * m > sh)
        {
            return null;
        }

        for (int column = 0; column < 4; ++column)
        {
            float x;

            if (anchor[0] == 0)
            {
                x = m + (float)column * (sw * 0.18F);
            }
            else if (anchor[0] == 2)
            {
                x = sw - w - m - (float)column * (sw * 0.18F);
            }
            else
            {
                x = sw / 2.0F - w / 2.0F + (column % 2 == 0 ? 1.0F : -1.0F) * (float)((column + 1) / 2) * (w + m);
            }

            if (x < 0.0F || x + w > sw)
            {
                continue;
            }

            float start;
            float dir;

            if (anchor[1] == 0)
            {
                start = m;
                dir = 1.0F;
            }
            else if (anchor[1] == 2)
            {
                start = sh - hh - m;
                dir = -1.0F;
            }
            else
            {
                // middle: search outwards from the centre, alternating down and up
                start = sh / 2.0F - hh / 2.0F;
                dir = 0.0F;
            }

            for (int step = 0; step < (int)sh; ++step)
            {
                float y = dir != 0.0F ? start + dir * 2.0F * (float)step : start + (step % 2 == 0 ? 2.0F : -2.0F) * (float)((step + 1) / 2);

                if (y < 0.0F || y + hh > sh)
                {
                    if (dir != 0.0F)
                    {
                        break;
                    }

                    continue;
                }

                float[] b = {x, y, x + w, y + hh};
                boolean free = true;

                for (float[] t : taken)
                {
                    if (overlaps(b, t))
                    {
                        free = false;
                        break;
                    }
                }

                if (free)
                {
                    return new float[] {x, y};
                }
            }
        }

        return null;
    }
}
