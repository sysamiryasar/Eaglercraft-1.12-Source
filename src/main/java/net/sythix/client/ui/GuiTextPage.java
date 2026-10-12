package net.sythix.client.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

/**
 * A scrollable page of wrapped text (help pages, logs, diagnostics). Lines starting with "# "
 * are headings. Optional Copy button copies the raw text.
 */
public class GuiTextPage extends GuiScreen
{
    private final GuiScreen parent;
    private final String title;
    private final Supplier<String> text;
    private List<String> lines = new ArrayList<String>();
    private float scroll;
    private boolean copied;
    private int px;
    private int py;
    private int pw;
    private int ph;

    public GuiTextPage(GuiScreen parent, String title, Supplier<String> text)
    {
        this.parent = parent;
        this.title = title;
        this.text = text;
    }

    public void initGui()
    {
        this.pw = Math.min(this.width - 12, 440);
        this.ph = Math.min(this.height - 12, 290);
        this.px = (this.width - this.pw) / 2;
        this.py = (this.height - this.ph) / 2;
        this.refresh();
    }

    private void refresh()
    {
        this.lines = new ArrayList<String>();

        for (String raw : this.text.get().split("\n", -1))
        {
            if (raw.startsWith("# "))
            {
                this.lines.add("§l" + raw.substring(2));
            }
            else if (raw.isEmpty())
            {
                this.lines.add("");
            }
            else
            {
                this.lines.addAll(this.fontRenderer.listFormattedStringToWidth(raw, this.pw - 24));
            }
        }
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
        int top = this.py + 26;
        int bottom = this.py + this.ph - 26;
        int max = Math.max(0, this.lines.size() * 10 - (bottom - top));
        this.scroll = Math.max(0.0F, Math.min((float)max, this.scroll));
        SythixTheme.beginClip(this.px, top, this.px + this.pw, bottom);
        int y = top - Math.round(this.scroll);

        for (String line : this.lines)
        {
            if (y > top - 10 && y < bottom)
            {
                boolean heading = line.startsWith("§l");
                this.fontRenderer.drawString(line, (float)(this.px + 12), (float)y, heading ? SythixTheme.LIGHT_BLUE : SythixTheme.TEXT, false);
            }

            y += 10;
        }

        SythixTheme.endClip();
        String[] labels = new String[] {this.copied ? "Copied!" : "Copy", "Refresh", "Back"};
        int bw = (this.pw - 8 - 6) / 3;

        for (int i = 0; i < 3; ++i)
        {
            int bx = this.px + 4 + i * (bw + 3);
            int by = this.py + this.ph - 20;
            boolean hover = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + 16;
            SythixTheme.drawButton((float)bx, (float)by, (float)bw, 16.0F, true, hover ? 1.0F : 0.0F);
            this.fontRenderer.drawString(labels[i], (float)(bx + bw / 2 - this.fontRenderer.getStringWidth(labels[i]) / 2), (float)(by + 4), SythixTheme.getButtonTextColor(true, hover ? 1.0F : 0.0F), false);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        int bw = (this.pw - 8 - 6) / 3;
        int by = this.py + this.ph - 20;

        for (int i = 0; i < 3; ++i)
        {
            int bx = this.px + 4 + i * (bw + 3);

            if (mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + 16)
            {
                if (i == 0)
                {
                    setClipboardString(this.text.get().replaceAll("§.", ""));
                    this.copied = true;
                }
                else if (i == 1)
                {
                    this.refresh();
                }
                else
                {
                    this.mc.displayGuiScreen(this.parent);
                }
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
