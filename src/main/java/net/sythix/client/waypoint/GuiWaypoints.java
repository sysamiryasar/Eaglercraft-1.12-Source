package net.sythix.client.waypoint;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.entity.Entity;
import net.sythix.client.core.Notifications;
import net.sythix.client.ui.SythixTheme;
import org.apache.commons.io.FileUtils;
import org.lwjgl.input.Mouse;

/**
 * Waypoint manager: add, edit, remove, organise, toggle, import and export waypoints.
 */
public class GuiWaypoints extends GuiScreen
{
    private static final int ROW = 20;
    private static String sort = "Custom";
    private static String groupFilter = "All";

    private final GuiScreen parent;
    private Waypoint selected;
    private float scroll;
    private long lastClick;
    private int px;
    private int py;
    private int pw;
    private int ph;

    public GuiWaypoints(GuiScreen parent)
    {
        this.parent = parent;
    }

    public void initGui()
    {
        this.pw = Math.min(this.width - 12, 440);
        this.ph = Math.min(this.height - 12, 280);
        this.px = (this.width - this.pw) / 2;
        this.py = (this.height - this.ph) / 2;
    }

    private List<Waypoint> visibleList()
    {
        List<Waypoint> list = new ArrayList<Waypoint>();

        for (Waypoint w : WaypointStore.current())
        {
            if (groupFilter.equals("All") || w.group.equals(groupFilter) || (groupFilter.equals("Deaths") && w.death))
            {
                list.add(w);
            }
        }

        if (sort.equals("Name"))
        {
            list.sort(Comparator.comparing(w -> w.name.toLowerCase()));
        }
        else if (sort.equals("Distance"))
        {
            list.sort(Comparator.comparingDouble(this::distance));
        }
        else if (sort.equals("Newest"))
        {
            list.sort(Comparator.comparingLong((Waypoint w) -> w.created).reversed());
        }

        return list;
    }

    private double distance(Waypoint w)
    {
        Entity e = this.mc.getRenderViewEntity();
        return e == null ? 0.0D : Math.sqrt(e.getDistanceSq((double)w.x + 0.5D, (double)w.y, (double)w.z + 0.5D));
    }

    private List<String> groups()
    {
        Set<String> set = new LinkedHashSet<String>();
        set.add("All");
        set.add("Deaths");

        for (Waypoint w : WaypointStore.current())
        {
            if (!w.group.isEmpty())
            {
                set.add(w.group);
            }
        }

        return new ArrayList<String>(set);
    }

    private String[] buttons()
    {
        return new String[] {"Add Here", "Edit", "Delete", "Show/Hide", "Up", "Down", "Sort: " + sort, "Group: " + groupFilter, "Import", "Export", "Done"};
    }

    private int listTop()
    {
        return this.py + 26;
    }

    private int listBottom()
    {
        return this.py + this.ph - 46;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        if (this.mc.world == null)
        {
            SythixTheme.drawBackground(this.width, this.height, -1, -1, 0.45F);
        }
        else
        {
            SythixTheme.drawBackground(this.width, this.height, -1, -1, 0.35F);
        }

        SythixTheme.drawGlass((float)this.px, (float)this.py, (float)(this.px + this.pw), (float)(this.py + this.ph), 8.0F, 1.0F);
        this.fontRenderer.drawString("Waypoints", (float)(this.px + 8), (float)(this.py + 8), 0xFFFFFFFF, true);
        String where = WaypointStore.currentKey().replace("world:", "World: ").replace("server:", "Server: ");
        this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(where, this.pw / 2), (float)(this.px + this.pw - 8 - Math.min(this.pw / 2, this.fontRenderer.getStringWidth(where))), (float)(this.py + 8), SythixTheme.TEXT_MUTED, false);
        SythixTheme.drawAccentLine((float)this.px, (float)(this.py + 21), (float)(this.px + this.pw), 1.0F);

        List<Waypoint> list = this.visibleList();
        int maxScroll = Math.max(0, list.size() * ROW - (this.listBottom() - this.listTop()));
        this.scroll = Math.max(0.0F, Math.min((float)maxScroll, this.scroll));
        SythixTheme.beginClip(this.px, this.listTop(), this.px + this.pw, this.listBottom());
        int y = this.listTop() - Math.round(this.scroll);

        for (Waypoint w : list)
        {
            if (y + ROW > this.listTop() && y < this.listBottom())
            {
                boolean hovered = mouseY >= y && mouseY < y + ROW && mouseY >= this.listTop() && mouseY < this.listBottom() && mouseX >= this.px && mouseX < this.px + this.pw;
                SythixTheme.drawRect((float)(this.px + 4), (float)y, (float)(this.px + this.pw - 4), (float)(y + ROW - 2), w == this.selected ? 0x40D81820 : (hovered ? 0x20FFFFFF : 0x10FFFFFF));
                SythixTheme.drawRect((float)(this.px + 8), (float)(y + 5), (float)(this.px + 16), (float)(y + 13), 0xFF000000 | w.color);
                int textColor = w.visible ? 0xFFFFFFFF : SythixTheme.TEXT_DISABLED;
                this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth((w.death ? "☠ " : "") + w.name, 120), (float)(this.px + 22), (float)(y + 5), textColor, false);
                this.fontRenderer.drawString(w.x + ", " + w.y + ", " + w.z, (float)(this.px + 150), (float)(y + 5), SythixTheme.TEXT_MUTED, false);
                String dim = w.dimension == -1 ? "Nether" : (w.dimension == 1 ? "End" : "Overworld");
                this.fontRenderer.drawString(dim, (float)(this.px + 250), (float)(y + 5), SythixTheme.TEXT_MUTED, false);
                String dist = Math.round(this.distance(w)) + "m";
                this.fontRenderer.drawString(dist, (float)(this.px + this.pw - 36 - this.fontRenderer.getStringWidth(dist)), (float)(y + 5), SythixTheme.LIGHT_BLUE, false);
                this.fontRenderer.drawString(w.visible ? "◉" : "○", (float)(this.px + this.pw - 20), (float)(y + 5), w.visible ? SythixTheme.RED : SythixTheme.TEXT_DISABLED, false);
            }

            y += ROW;
        }

        SythixTheme.endClip();

        if (list.isEmpty())
        {
            String none = "No waypoints yet. Press \"Add Here\" to save where you stand.";
            this.fontRenderer.drawString(none, (float)(this.px + this.pw / 2 - this.fontRenderer.getStringWidth(none) / 2), (float)(this.listTop() + 30), SythixTheme.TEXT_MUTED, false);
        }

        // Buttons in two rows
        String[] buttons = this.buttons();
        int perRow = 6;
        int bw = (this.pw - 8 - (perRow - 1) * 3) / perRow;

        for (int i = 0; i < buttons.length; ++i)
        {
            int bx = this.px + 4 + (i % perRow) * (bw + 3);
            int by = this.listBottom() + 4 + (i / perRow) * 19;
            boolean hover = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + 16;
            boolean enabled = i == 0 || i >= 6 || this.selected != null;
            SythixTheme.drawButton((float)bx, (float)by, (float)bw, 16.0F, enabled, hover && enabled ? 1.0F : 0.0F);
            String label = this.fontRenderer.trimStringToWidth(buttons[i], bw - 4);
            this.fontRenderer.drawString(label, (float)(bx + bw / 2 - this.fontRenderer.getStringWidth(label) / 2), (float)(by + 4), SythixTheme.getButtonTextColor(enabled, hover ? 1.0F : 0.0F), false);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseY >= this.listTop() && mouseY < this.listBottom())
        {
            List<Waypoint> list = this.visibleList();
            int index = (mouseY - this.listTop() + Math.round(this.scroll)) / ROW;

            if (index >= 0 && index < list.size())
            {
                Waypoint w = list.get(index);

                if (mouseX >= this.px + this.pw - 24)
                {
                    w.visible = !w.visible;
                    WaypointStore.save();
                }
                else if (w == this.selected && Minecraft.getSystemTime() - this.lastClick < 400L)
                {
                    this.mc.displayGuiScreen(new GuiWaypointEdit(this, w, false));
                }

                this.selected = w;
                this.lastClick = Minecraft.getSystemTime();
            }

            return;
        }

        String[] buttons = this.buttons();
        int perRow = 6;
        int bw = (this.pw - 8 - (perRow - 1) * 3) / perRow;

        for (int i = 0; i < buttons.length; ++i)
        {
            int bx = this.px + 4 + (i % perRow) * (bw + 3);
            int by = this.listBottom() + 4 + (i / perRow) * 19;

            if (mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + 16)
            {
                this.press(i);
                return;
            }
        }
    }

    private void press(int button)
    {
        List<Waypoint> all = WaypointStore.current();

        switch (button)
        {
            case 0:
                Entity e = this.mc.getRenderViewEntity();
                Waypoint w = new Waypoint("Waypoint " + (all.size() + 1), e == null ? 0 : (int)Math.floor(e.posX), e == null ? 64 : (int)Math.floor(e.posY), e == null ? 0 : (int)Math.floor(e.posZ), this.mc.world == null ? 0 : this.mc.world.provider.getDimensionType().getId(), WaypointColors.next(all.size()));

                if (!groupFilter.equals("All") && !groupFilter.equals("Deaths"))
                {
                    w.group = groupFilter;
                }

                this.mc.displayGuiScreen(new GuiWaypointEdit(this, w, true));
                break;

            case 1:
                if (this.selected != null)
                {
                    this.mc.displayGuiScreen(new GuiWaypointEdit(this, this.selected, false));
                }

                break;

            case 2:
                if (this.selected != null)
                {
                    this.mc.displayGuiScreen(new GuiYesNo(this, "Delete \"" + this.selected.name + "\"?", "This cannot be undone.", 0));
                }

                break;

            case 3:
                if (this.selected != null)
                {
                    this.selected.visible = !this.selected.visible;
                    WaypointStore.save();
                }

                break;

            case 4:
            case 5:
                if (this.selected != null)
                {
                    int i = all.indexOf(this.selected);
                    int j = button == 4 ? i - 1 : i + 1;

                    if (i >= 0 && j >= 0 && j < all.size())
                    {
                        all.set(i, all.get(j));
                        all.set(j, this.selected);
                        sort = "Custom";
                        WaypointStore.save();
                    }
                }

                break;

            case 6:
                String[] sorts = new String[] {"Custom", "Name", "Distance", "Newest"};
                int s = 0;

                for (int k = 0; k < sorts.length; ++k)
                {
                    if (sorts[k].equals(sort))
                    {
                        s = k;
                    }
                }

                sort = sorts[(s + 1) % sorts.length];
                break;

            case 7:
                List<String> groups = this.groups();
                int g = groups.indexOf(groupFilter);
                groupFilter = groups.get((g + 1) % groups.size());
                break;

            case 8:
                this.importFromClipboard();
                break;

            case 9:
                this.export();
                break;

            default:
                this.mc.displayGuiScreen(this.parent);
        }
    }

    private void importFromClipboard()
    {
        try
        {
            List<Waypoint> imported = WaypointStore.parseList(new JsonParser().parse(GuiScreen.getClipboardString()).getAsJsonArray());
            WaypointStore.current().addAll(imported);
            WaypointStore.save();
            Notifications.post("Waypoints imported", imported.size() + " waypoints added from the clipboard.", Notifications.Kind.SUCCESS);
        }
        catch (Exception e)
        {
            Notifications.post("Import failed", "Copy a waypoint export (JSON) to the clipboard first.", Notifications.Kind.ERROR);
        }
    }

    private void export()
    {
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(WaypointStore.toArray(WaypointStore.current()));
        GuiScreen.setClipboardString(json);

        try
        {
            File file = new File(new File(this.mc.mcDataDir, "sythix"), "waypoints-export-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".json");
            FileUtils.writeStringToFile(file, json, StandardCharsets.UTF_8);
            Notifications.post("Waypoints exported", "Copied to clipboard and saved to sythix/" + file.getName(), Notifications.Kind.SUCCESS);
        }
        catch (IOException e)
        {
            Notifications.post("Waypoints exported", "Copied to clipboard (file could not be written).", Notifications.Kind.WARNING);
        }
    }

    public void confirmClicked(boolean result, int id)
    {
        if (result && id == 0 && this.selected != null)
        {
            WaypointStore.current().remove(this.selected);
            WaypointStore.save();
            this.selected = null;
        }

        this.mc.displayGuiScreen(this);
    }

    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();

        if (wheel != 0)
        {
            this.scroll -= (float)wheel * 0.25F;
        }
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (keyCode == 1)
        {
            this.mc.displayGuiScreen(this.parent);
        }
        else if (keyCode == 211 && this.selected != null)
        {
            this.press(2);
        }
    }

    public boolean doesGuiPauseGame()
    {
        return false;
    }
}
