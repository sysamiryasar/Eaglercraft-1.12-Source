package net.sythix.client.ui;

import java.io.IOException;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

/**
 * A themed list screen with selectable rows and a row of buttons. Subclasses supply the rows and
 * what each button does.
 */
public abstract class GuiListScreen extends GuiScreen
{
    /** One row: title, subtitle, right-hand text, accent colour and optional icon. */
    public static class Row
    {
        public final Object key;
        public final String title;
        public final String subtitle;
        public final String right;
        public final int accent;
        public ResourceLocation icon;

        public Row(Object key, String title, String subtitle, String right, int accent)
        {
            this.key = key;
            this.title = title;
            this.subtitle = subtitle;
            this.right = right;
            this.accent = accent;
        }
    }

    protected final GuiScreen parent;
    private final String title;
    protected Object selected;
    private float scroll;
    private long lastClick;
    protected String status = "";
    protected int px;
    protected int py;
    protected int pw;
    protected int ph;

    protected GuiListScreen(GuiScreen parent, String title)
    {
        this.parent = parent;
        this.title = title;
    }

    protected abstract List<Row> rows();

    /** Button labels, left to right. */
    protected abstract String[] buttons();

    /** Called when button i is pressed. */
    protected abstract void press(int index);

    /** Whether button i can be pressed right now (default: always). */
    protected boolean enabled(int index)
    {
        return true;
    }

    /** Called when a row is double-clicked. */
    protected void open(Object key)
    {
    }

    /** Optional extra line under the title. */
    protected String header()
    {
        return "";
    }

    protected int rowHeight()
    {
        return 22;
    }

    public void initGui()
    {
        this.pw = Math.min(this.width - 12, 460);
        this.ph = Math.min(this.height - 12, 290);
        this.px = (this.width - this.pw) / 2;
        this.py = (this.height - this.ph) / 2;
    }

    private int perRow()
    {
        return Math.min(6, this.buttons().length);
    }

    private int buttonRows()
    {
        return (this.buttons().length + this.perRow() - 1) / this.perRow();
    }

    protected int listTop()
    {
        return this.py + (this.header().isEmpty() ? 26 : 36);
    }

    protected int listBottom()
    {
        return this.py + this.ph - 8 - this.buttonRows() * 19 - (this.status.isEmpty() ? 0 : 10);
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
        this.fontRenderer.drawString(this.title, (float)(this.px + 8), (float)(this.py + 8), 0xFFFFFFFF, true);
        SythixTheme.drawAccentLine((float)this.px, (float)(this.py + 21), (float)(this.px + this.pw), 1.0F);

        if (!this.header().isEmpty())
        {
            this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(this.header(), this.pw - 16), (float)(this.px + 8), (float)(this.py + 25), SythixTheme.TEXT_MUTED, false);
        }

        List<Row> rows = this.rows();
        int rh = this.rowHeight();
        int max = Math.max(0, rows.size() * rh - (this.listBottom() - this.listTop()));
        this.scroll = Math.max(0.0F, Math.min((float)max, this.scroll));
        SythixTheme.beginClip(this.px, this.listTop(), this.px + this.pw, this.listBottom());
        int y = this.listTop() - Math.round(this.scroll);

        for (Row row : rows)
        {
            if (y + rh > this.listTop() && y < this.listBottom())
            {
                boolean hover = mouseX >= this.px && mouseX < this.px + this.pw && mouseY >= y && mouseY < y + rh && mouseY >= this.listTop() && mouseY < this.listBottom();
                boolean sel = row.key != null && row.key.equals(this.selected);
                SythixTheme.drawRect((float)(this.px + 4), (float)y, (float)(this.px + this.pw - 4), (float)(y + rh - 2), sel ? 0x40D81820 : (hover ? 0x20FFFFFF : 0x10FFFFFF));
                SythixTheme.drawRect((float)(this.px + 4), (float)y, (float)(this.px + 6), (float)(y + rh - 2), row.accent);
                int tx = this.px + 10;

                if (row.icon != null)
                {
                    SythixTheme.drawTexture(row.icon, (float)tx, (float)(y + 1), (float)(rh - 4), (float)(rh - 4), 1.0F);
                    tx += rh;
                }

                int rightW = row.right == null ? 0 : this.fontRenderer.getStringWidth(row.right);
                this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(row.title, this.pw - (tx - this.px) - rightW - 16), (float)tx, (float)(y + 3), 0xFFFFFFFF, false);

                if (row.subtitle != null && !row.subtitle.isEmpty() && rh >= 20)
                {
                    this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(row.subtitle, this.pw - (tx - this.px) - 12), (float)tx, (float)(y + 12), SythixTheme.TEXT_MUTED, false);
                }

                if (row.right != null)
                {
                    this.fontRenderer.drawString(row.right, (float)(this.px + this.pw - 10 - rightW), (float)(y + 3), SythixTheme.LIGHT_BLUE, false);
                }
            }

            y += rh;
        }

        SythixTheme.endClip();

        if (rows.isEmpty())
        {
            String none = this.emptyText();
            this.fontRenderer.drawString(none, (float)(this.px + this.pw / 2 - this.fontRenderer.getStringWidth(none) / 2), (float)(this.listTop() + 30), SythixTheme.TEXT_MUTED, false);
        }

        if (!this.status.isEmpty())
        {
            this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(this.status, this.pw - 16), (float)(this.px + 8), (float)(this.listBottom() + 2), SythixTheme.LIGHT_BLUE, false);
        }

        String[] buttons = this.buttons();
        int perRow = this.perRow();
        int bw = (this.pw - 8 - (perRow - 1) * 3) / perRow;

        for (int i = 0; i < buttons.length; ++i)
        {
            int bx = this.px + 4 + (i % perRow) * (bw + 3);
            int by = this.py + this.ph - 4 - (this.buttonRows() - i / perRow) * 19;
            boolean enabled = this.enabled(i);
            boolean hover = enabled && mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + 16;
            SythixTheme.drawButton((float)bx, (float)by, (float)bw, 16.0F, enabled, hover ? 1.0F : 0.0F);
            String label = this.fontRenderer.trimStringToWidth(buttons[i], bw - 4);
            this.fontRenderer.drawString(label, (float)(bx + bw / 2 - this.fontRenderer.getStringWidth(label) / 2), (float)(by + 4), SythixTheme.getButtonTextColor(enabled, hover ? 1.0F : 0.0F), false);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    protected String emptyText()
    {
        return "Nothing here yet.";
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseY >= this.listTop() && mouseY < this.listBottom() && mouseX >= this.px && mouseX < this.px + this.pw)
        {
            List<Row> rows = this.rows();
            int index = (mouseY - this.listTop() + Math.round(this.scroll)) / this.rowHeight();

            if (index >= 0 && index < rows.size())
            {
                Object key = rows.get(index).key;

                if (key != null && key.equals(this.selected) && Minecraft.getSystemTime() - this.lastClick < 400L)
                {
                    this.open(key);
                }

                this.selected = key;
                this.lastClick = Minecraft.getSystemTime();
            }

            return;
        }

        String[] buttons = this.buttons();
        int perRow = this.perRow();
        int bw = (this.pw - 8 - (perRow - 1) * 3) / perRow;

        for (int i = 0; i < buttons.length; ++i)
        {
            int bx = this.px + 4 + (i % perRow) * (bw + 3);
            int by = this.py + this.ph - 4 - (this.buttonRows() - i / perRow) * 19;

            if (mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + 16 && this.enabled(i))
            {
                this.status = "";
                this.press(i);
                return;
            }
        }
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
    }

    public boolean doesGuiPauseGame()
    {
        return false;
    }
}
