package net.sythix.client.module.recording;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.sythix.client.core.Notifications;
import net.sythix.client.ui.SythixTheme;
import org.lwjgl.input.Mouse;

/**
 * Screenshot gallery: thumbnails, large preview, open, copy path and delete.
 */
public class GuiScreenshots extends GuiScreen
{
    private static final Map<String, ResourceLocation> THUMBS = new HashMap<String, ResourceLocation>();
    private final GuiScreen parent;
    private final List<File> files = new ArrayList<File>();
    private File selected;
    private boolean preview;
    private float scroll;
    private int px;
    private int py;
    private int pw;
    private int ph;

    public GuiScreenshots(GuiScreen parent)
    {
        this.parent = parent;
    }

    private File dir()
    {
        File dir = new File(this.mc.mcDataDir, "screenshots");
        dir.mkdirs();
        return dir;
    }

    public void initGui()
    {
        this.pw = Math.min(this.width - 12, 460);
        this.ph = Math.min(this.height - 12, 290);
        this.px = (this.width - this.pw) / 2;
        this.py = (this.height - this.ph) / 2;
        this.files.clear();
        File[] list = this.dir().listFiles((d, n) -> n.toLowerCase().endsWith(".png"));

        if (list != null)
        {
            Arrays.sort(list, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            this.files.addAll(Arrays.asList(list));
        }
    }

    private ResourceLocation thumb(File file)
    {
        String key = file.getAbsolutePath() + file.lastModified();

        if (!THUMBS.containsKey(key))
        {
            ResourceLocation location = null;

            try
            {
                BufferedImage full = ImageIO.read(file);

                if (full != null)
                {
                    int w = 192;
                    int h = Math.max(1, full.getHeight() * w / Math.max(1, full.getWidth()));
                    BufferedImage small = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = small.createGraphics();
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.drawImage(full, 0, 0, w, h, null);
                    g.dispose();
                    location = this.mc.getTextureManager().getDynamicTextureLocation("sythix_shot", new DynamicTexture(small));
                }
            }
            catch (Exception ignored)
            {
                location = null;
            }

            THUMBS.put(key, location);
        }

        return THUMBS.get(key);
    }

    private int cols()
    {
        return Math.max(2, (this.pw - 12) / 110);
    }

    private int cellW()
    {
        return (this.pw - 12 - (this.cols() - 1) * 4) / this.cols();
    }

    private int cellH()
    {
        return this.cellW() * 9 / 16 + 12;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        SythixTheme.drawRect(0.0F, 0.0F, (float)this.width, (float)this.height, this.mc.world == null ? SythixTheme.BLACK : 0x900B0B0E);
        SythixTheme.drawRect((float)this.px, (float)this.py, (float)(this.px + this.pw), (float)(this.py + this.ph), SythixTheme.withAlpha(SythixTheme.CHARCOAL, 0xF0));
        SythixTheme.drawOutline((float)this.px, (float)this.py, (float)(this.px + this.pw), (float)(this.py + this.ph), SythixTheme.BORDER);
        this.fontRenderer.drawString("Screenshots (" + this.files.size() + ")", (float)(this.px + 8), (float)(this.py + 8), 0xFFFFFFFF, true);
        SythixTheme.drawAccentLine((float)this.px, (float)(this.py + 21), (float)(this.px + this.pw), 1.0F);
        int top = this.py + 26;
        int bottom = this.py + this.ph - 26;

        if (this.preview && this.selected != null)
        {
            ResourceLocation t = this.thumb(this.selected);
            int w = this.pw - 12;
            int h = Math.min(bottom - top - 12, w * 9 / 16);
            int pwid = h * 16 / 9;

            if (t != null)
            {
                SythixTheme.drawTexture(t, (float)(this.px + (this.pw - pwid) / 2), (float)top, (float)pwid, (float)h, 1.0F);
            }

            this.fontRenderer.drawString(this.selected.getName(), (float)(this.px + 8), (float)(top + h + 2), SythixTheme.TEXT_MUTED, false);
        }
        else
        {
            int rows = (this.files.size() + this.cols() - 1) / this.cols();
            this.scroll = Math.max(0.0F, Math.min((float)Math.max(0, rows * (this.cellH() + 4) - (bottom - top)), this.scroll));
            SythixTheme.beginClip(this.px, top, this.px + this.pw, bottom);

            for (int i = 0; i < this.files.size(); ++i)
            {
                int x = this.px + 6 + (i % this.cols()) * (this.cellW() + 4);
                int y = top + (i / this.cols()) * (this.cellH() + 4) - Math.round(this.scroll);

                if (y + this.cellH() < top || y > bottom)
                {
                    continue;
                }

                File f = this.files.get(i);
                SythixTheme.drawRect((float)x, (float)y, (float)(x + this.cellW()), (float)(y + this.cellH()), f.equals(this.selected) ? 0x60D81820 : 0x30FFFFFF);
                ResourceLocation t = this.thumb(f);

                if (t != null)
                {
                    SythixTheme.drawTexture(t, (float)(x + 1), (float)(y + 1), (float)(this.cellW() - 2), (float)(this.cellH() - 13), 1.0F);
                }

                this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(f.getName(), this.cellW() - 4), (float)(x + 2), (float)(y + this.cellH() - 10), SythixTheme.TEXT_MUTED, false);
            }

            SythixTheme.endClip();

            if (this.files.isEmpty())
            {
                String none = "No screenshots yet. Press F2 in game.";
                this.fontRenderer.drawString(none, (float)(this.px + this.pw / 2 - this.fontRenderer.getStringWidth(none) / 2), (float)(top + 40), SythixTheme.TEXT_MUTED, false);
            }
        }

        String[] buttons = this.buttons();
        int bw = (this.pw - 8 - (buttons.length - 1) * 3) / buttons.length;

        for (int i = 0; i < buttons.length; ++i)
        {
            int bx = this.px + 4 + i * (bw + 3);
            int by = this.py + this.ph - 22;
            boolean enabled = i >= 4 || this.selected != null;
            boolean hover = enabled && mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + 16;
            SythixTheme.drawButton((float)bx, (float)by, (float)bw, 16.0F, enabled, hover ? 1.0F : 0.0F);
            this.fontRenderer.drawString(buttons[i], (float)(bx + bw / 2 - this.fontRenderer.getStringWidth(buttons[i]) / 2), (float)(by + 4), SythixTheme.getButtonTextColor(enabled, hover ? 1.0F : 0.0F), false);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private String[] buttons()
    {
        return new String[] {this.preview ? "Back" : "View", "Open", "Copy Path", "Delete", "Folder", "Done"};
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        int top = this.py + 26;
        int bottom = this.py + this.ph - 26;

        if (!this.preview && mouseY >= top && mouseY < bottom)
        {
            for (int i = 0; i < this.files.size(); ++i)
            {
                int x = this.px + 6 + (i % this.cols()) * (this.cellW() + 4);
                int y = top + (i / this.cols()) * (this.cellH() + 4) - Math.round(this.scroll);

                if (mouseX >= x && mouseX < x + this.cellW() && mouseY >= y && mouseY < y + this.cellH())
                {
                    if (this.files.get(i).equals(this.selected))
                    {
                        this.preview = true;
                    }

                    this.selected = this.files.get(i);
                    return;
                }
            }

            return;
        }

        String[] buttons = this.buttons();
        int bw = (this.pw - 8 - (buttons.length - 1) * 3) / buttons.length;
        int by = this.py + this.ph - 22;

        if (mouseY < by || mouseY >= by + 16)
        {
            return;
        }

        int index = (mouseX - this.px - 4) / (bw + 3);

        if (index < 4 && this.selected == null)
        {
            return;
        }

        switch (index)
        {
            case 0:
                this.preview = !this.preview;
                break;
            case 1:
                this.open(this.selected);
                break;
            case 2:
                GuiScreen.setClipboardString(this.selected.getAbsolutePath());
                Notifications.post("Path copied", this.selected.getName(), Notifications.Kind.SUCCESS);
                break;
            case 3:
                this.mc.displayGuiScreen(new GuiYesNo(this, "Delete " + this.selected.getName() + "?", "This cannot be undone.", 0));
                break;
            case 4:
                this.open(this.dir());
                break;
            default:
                this.mc.displayGuiScreen(this.parent);
        }
    }

    private void open(File file)
    {
        try
        {
            java.awt.Desktop.getDesktop().open(file);
        }
        catch (Throwable t)
        {
            Notifications.post("Location", file.getAbsolutePath(), Notifications.Kind.INFO);
        }
    }

    public void confirmClicked(boolean result, int id)
    {
        if (result && this.selected != null && this.selected.delete())
        {
            this.selected = null;
            this.preview = false;
        }

        this.mc.displayGuiScreen(this);
    }

    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();

        if (wheel != 0)
        {
            this.scroll -= (float)wheel * 0.3F;
        }
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (keyCode == 1)
        {
            if (this.preview)
            {
                this.preview = false;
            }
            else
            {
                this.mc.displayGuiScreen(this.parent);
            }
        }
    }

    public boolean doesGuiPauseGame()
    {
        return false;
    }
}
