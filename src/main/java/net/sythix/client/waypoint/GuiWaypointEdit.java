package net.sythix.client.waypoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.ColorSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.Setting;
import net.sythix.client.setting.TextSetting;
import net.sythix.client.ui.SythixTheme;
import net.sythix.client.ui.component.SettingsPanel;
import org.lwjgl.input.Mouse;

/**
 * Edits one waypoint: name, position, dimension, colour, group and visibility.
 */
public class GuiWaypointEdit extends GuiScreen
{
    private final GuiScreen parent;
    private final Waypoint waypoint;
    private final boolean isNew;
    private final SettingsPanel panel = new SettingsPanel();
    private final TextSetting name;
    private final TextSetting x;
    private final TextSetting y;
    private final TextSetting z;
    private final ModeSetting dimension;
    private final ColorSetting color;
    private final TextSetting group;
    private final BooleanSetting visible;
    private final List<Setting<?>> settings = new ArrayList<Setting<?>>();
    private String error = "";
    private int px;
    private int py;
    private int pw;
    private int ph;

    public GuiWaypointEdit(GuiScreen parent, Waypoint waypoint, boolean isNew)
    {
        this.parent = parent;
        this.waypoint = waypoint;
        this.isNew = isNew;
        this.name = new TextSetting("Name", "Label shown in the world.", waypoint.name, 32);
        this.x = new TextSetting("X", "X coordinate.", String.valueOf(waypoint.x), 9);
        this.y = new TextSetting("Y", "Y coordinate (height).", String.valueOf(waypoint.y), 4);
        this.z = new TextSetting("Z", "Z coordinate.", String.valueOf(waypoint.z), 9);
        this.dimension = new ModeSetting("Dimension", "Which dimension this waypoint is in.", waypoint.dimension == -1 ? "Nether" : (waypoint.dimension == 1 ? "End" : "Overworld"), "Overworld", "Nether", "End");
        this.color = new ColorSetting("Color", "Colour of the beam and label.", 0xFF000000 | waypoint.color, false);
        this.group = new TextSetting("Group", "Optional group name for organising.", waypoint.group, 24);
        this.visible = new BooleanSetting("Visible", "Show this waypoint in the world.", waypoint.visible);
        this.settings.add(this.name);
        this.settings.add(new ButtonSetting("Position", "Fill X, Y and Z with where you stand.", "Use My Position", this::useCurrentPosition));
        this.settings.add(this.x);
        this.settings.add(this.y);
        this.settings.add(this.z);
        this.settings.add(this.dimension);
        this.settings.add(this.color);
        this.settings.add(this.group);
        this.settings.add(this.visible);
    }

    private void useCurrentPosition()
    {
        Entity e = this.mc.getRenderViewEntity();

        if (e != null)
        {
            this.x.set(String.valueOf((int)Math.floor(e.posX)));
            this.y.set(String.valueOf((int)Math.floor(e.posY)));
            this.z.set(String.valueOf((int)Math.floor(e.posZ)));
        }
    }

    public void initGui()
    {
        this.pw = Math.min(this.width - 12, 300);
        this.ph = Math.min(this.height - 12, 250);
        this.px = (this.width - this.pw) / 2;
        this.py = (this.height - this.ph) / 2;
        this.panel.setBounds(this.px + 6, this.py + 26, this.pw - 12, this.ph - 56);
        this.panel.setSettings(this.settings);
    }

    private boolean save()
    {
        try
        {
            int nx = Integer.parseInt(this.x.get().trim());
            int ny = Integer.parseInt(this.y.get().trim());
            int nz = Integer.parseInt(this.z.get().trim());
            this.waypoint.name = this.name.get().trim().isEmpty() ? "Waypoint" : this.name.get().trim();
            this.waypoint.x = nx;
            this.waypoint.y = ny;
            this.waypoint.z = nz;
            this.waypoint.dimension = this.dimension.is("Nether") ? -1 : (this.dimension.is("End") ? 1 : 0);
            this.waypoint.color = this.color.getArgb() & 0xFFFFFF;
            this.waypoint.group = this.group.get().trim();
            this.waypoint.visible = this.visible.isOn();

            if (this.isNew)
            {
                WaypointStore.current().add(this.waypoint);
            }

            WaypointStore.save();
            return true;
        }
        catch (NumberFormatException e)
        {
            this.error = "X, Y and Z must be whole numbers.";
            return false;
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        SythixTheme.drawBackground(this.width, this.height, -1, -1, 0.35F);
        SythixTheme.drawGlass((float)this.px, (float)this.py, (float)(this.px + this.pw), (float)(this.py + this.ph), 8.0F, 1.0F);
        this.fontRenderer.drawString(this.isNew ? "New Waypoint" : "Edit Waypoint", (float)(this.px + 8), (float)(this.py + 8), 0xFFFFFFFF, true);
        SythixTheme.drawAccentLine((float)this.px, (float)(this.py + 21), (float)(this.px + this.pw), 1.0F);
        this.panel.render(mouseX, mouseY);

        if (!this.error.isEmpty())
        {
            this.fontRenderer.drawString(this.error, (float)(this.px + 8), (float)(this.py + this.ph - 40), 0xFFFF5555, false);
        }

        this.drawButton("Save", this.px + this.pw - 130, mouseX, mouseY);
        this.drawButton("Cancel", this.px + this.pw - 66, mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawButton(String text, int x, int mouseX, int mouseY)
    {
        int y = this.py + this.ph - 24;
        boolean hover = mouseX >= x && mouseX < x + 60 && mouseY >= y && mouseY < y + 18;
        SythixTheme.drawButton((float)x, (float)y, 60.0F, 18.0F, true, hover ? 1.0F : 0.0F);
        this.fontRenderer.drawString(text, (float)(x + 30 - this.fontRenderer.getStringWidth(text) / 2), (float)(y + 5), SythixTheme.TEXT, false);
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        int y = this.py + this.ph - 24;

        if (mouseY >= y && mouseY < y + 18)
        {
            if (mouseX >= this.px + this.pw - 130 && mouseX < this.px + this.pw - 70)
            {
                if (this.save())
                {
                    this.mc.displayGuiScreen(this.parent);
                }

                return;
            }

            if (mouseX >= this.px + this.pw - 66 && mouseX < this.px + this.pw - 6)
            {
                this.mc.displayGuiScreen(this.parent);
                return;
            }
        }

        this.panel.mouseClicked(mouseX, mouseY, mouseButton);
    }

    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        super.mouseReleased(mouseX, mouseY, state);
        this.panel.mouseReleased();
    }

    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();

        if (wheel != 0)
        {
            this.panel.scrollBy(wheel);
        }
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (this.panel.keyTyped(typedChar, keyCode))
        {
            return;
        }

        if (keyCode == 1)
        {
            this.mc.displayGuiScreen(this.parent);
        }
        else if (keyCode == 28 && this.save())
        {
            this.mc.displayGuiScreen(this.parent);
        }
    }

    public boolean doesGuiPauseGame()
    {
        return false;
    }
}
