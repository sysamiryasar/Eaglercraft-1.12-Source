package net.sythix.client.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.util.Util;
import net.sythix.client.SythixClient;
import net.sythix.client.module.visuals.TexturePackManager;
import org.lwjgl.input.Mouse;

/**
 * Texture Pack Manager screen: available and active packs, favourites, preview and compatibility.
 */
public class GuiTexturePacks extends GuiScreen
{
    private static final int ROW = 24;
    private final GuiScreen parent;
    private final List<ResourcePackRepository.Entry> available = new ArrayList<ResourcePackRepository.Entry>();
    /** Active packs, top of the list = highest priority. */
    private final List<ResourcePackRepository.Entry> active = new ArrayList<ResourcePackRepository.Entry>();
    private ResourcePackRepository.Entry selected;
    private boolean changed;
    private float scrollLeft;
    private float scrollRight;
    private int px;
    private int py;
    private int pw;
    private int ph;

    public GuiTexturePacks(GuiScreen parent)
    {
        this.parent = parent;
    }

    private TexturePackManager manager()
    {
        return SythixClient.get().getModuleManager().get(TexturePackManager.class);
    }

    public void initGui()
    {
        this.pw = Math.min(this.width - 12, 460);
        this.ph = Math.min(this.height - 12, 290);
        this.px = (this.width - this.pw) / 2;
        this.py = (this.height - this.ph) / 2;

        if (this.available.isEmpty() && this.active.isEmpty())
        {
            ResourcePackRepository repo = this.mc.getResourcePackRepository();
            repo.updateRepositoryEntriesAll();
            this.active.addAll(repo.getRepositoryEntries());
            java.util.Collections.reverse(this.active);

            for (ResourcePackRepository.Entry entry : repo.getRepositoryEntriesAll())
            {
                if (!this.active.contains(entry))
                {
                    this.available.add(entry);
                }
            }

            this.available.sort((a, b) -> Boolean.compare(this.manager().isFavorite(b.getResourcePackName()), this.manager().isFavorite(a.getResourcePackName())));
        }
    }

    private int listTop()
    {
        return this.py + 36;
    }

    private int listBottom()
    {
        return this.py + this.ph - 74;
    }

    private int colWidth()
    {
        return (this.pw - 18) / 2;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        if (this.mc.world == null)
        {
            SythixTheme.drawBackground(this.width, this.height, -1, -1, 0.45F);
        }
        else
        {
            SythixTheme.drawRect(0.0F, 0.0F, (float)this.width, (float)this.height, 0x900B0B0E);
        }

        SythixTheme.drawRect((float)this.px, (float)this.py, (float)(this.px + this.pw), (float)(this.py + this.ph), SythixTheme.withAlpha(SythixTheme.CHARCOAL, 0xF0));
        SythixTheme.drawOutline((float)this.px, (float)this.py, (float)(this.px + this.pw), (float)(this.py + this.ph), SythixTheme.BORDER);
        this.fontRenderer.drawString("Texture Pack Manager", (float)(this.px + 8), (float)(this.py + 8), 0xFFFFFFFF, true);
        SythixTheme.drawAccentLine((float)this.px, (float)(this.py + 21), (float)(this.px + this.pw), 1.0F);
        int lx = this.px + 6;
        int rx = this.px + 12 + this.colWidth();
        this.fontRenderer.drawString("Available (click to use, ★ = favourite)", (float)lx, (float)(this.py + 25), SythixTheme.TEXT_MUTED, false);
        this.fontRenderer.drawString("Active (top = highest priority)", (float)rx, (float)(this.py + 25), SythixTheme.TEXT_MUTED, false);
        this.scrollLeft = this.drawList(this.available, lx, mouseX, mouseY, this.scrollLeft);
        this.scrollRight = this.drawList(this.active, rx, mouseX, mouseY, this.scrollRight);

        // Preview of the selected pack
        int pyTop = this.listBottom() + 6;
        SythixTheme.drawRect((float)(this.px + 6), (float)pyTop, (float)(this.px + this.pw - 6), (float)(pyTop + 42), 0x40000000);

        if (this.selected != null)
        {
            this.selected.bindTexturePackIcon(this.mc.getTextureManager());
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            drawModalRectWithCustomSizedTexture(this.px + 9, pyTop + 3, 0.0F, 0.0F, 36, 36, 36.0F, 36.0F);
            this.fontRenderer.drawString(this.selected.getResourcePackName(), (float)(this.px + 50), (float)(pyTop + 4), 0xFFFFFFFF, false);
            List<String> desc = this.fontRenderer.listFormattedStringToWidth(this.selected.getTexturePackDescription(), this.pw - 60);

            for (int i = 0; i < Math.min(2, desc.size()); ++i)
            {
                this.fontRenderer.drawString(desc.get(i), (float)(this.px + 50), (float)(pyTop + 14 + i * 9), SythixTheme.TEXT_MUTED, false);
            }

            String compat = TexturePackManager.compatibility(this.selected.getPackFormat());
            this.fontRenderer.drawString(compat == null ? "Compatible with 1.12 (pack format 3)" : compat + " (format " + this.selected.getPackFormat() + ")", (float)(this.px + 50), (float)(pyTop + 33), compat == null ? 0xFF55FF55 : 0xFFFFD24A, false);
        }
        else
        {
            this.fontRenderer.drawString("Select a pack to preview it.", (float)(this.px + 12), (float)(pyTop + 16), SythixTheme.TEXT_MUTED, false);
        }

        String[] buttons = new String[] {"★ Favourite", "Move Up", "Move Down", "Open Folder", "Done"};
        int bw = (this.pw - 12 - 4 * 4) / 5;

        for (int i = 0; i < buttons.length; ++i)
        {
            int bx = this.px + 6 + i * (bw + 4);
            int by = this.py + this.ph - 22;
            boolean hover = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + 16;
            SythixTheme.drawButton((float)bx, (float)by, (float)bw, 16.0F, true, hover ? 1.0F : 0.0F);
            this.fontRenderer.drawString(buttons[i], (float)(bx + bw / 2 - this.fontRenderer.getStringWidth(buttons[i]) / 2), (float)(by + 4), SythixTheme.TEXT, false);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private float drawList(List<ResourcePackRepository.Entry> list, int x, int mouseX, int mouseY, float scroll)
    {
        int w = this.colWidth();
        int max = Math.max(0, list.size() * ROW - (this.listBottom() - this.listTop()));
        scroll = Math.max(0.0F, Math.min((float)max, scroll));
        SythixTheme.drawRect((float)x, (float)this.listTop(), (float)(x + w), (float)this.listBottom(), 0x30000000);
        SythixTheme.beginClip(x, this.listTop(), x + w, this.listBottom());
        int y = this.listTop() - Math.round(scroll);
        TexturePackManager manager = this.manager();

        for (ResourcePackRepository.Entry entry : list)
        {
            if (y + ROW > this.listTop() && y < this.listBottom())
            {
                boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + ROW && mouseY >= this.listTop() && mouseY < this.listBottom();
                SythixTheme.drawRect((float)x, (float)y, (float)(x + w), (float)(y + ROW - 1), entry == this.selected ? 0x40D81820 : (hover ? 0x20FFFFFF : 0x00000000));
                entry.bindTexturePackIcon(this.mc.getTextureManager());
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                drawModalRectWithCustomSizedTexture(x + 2, y + 2, 0.0F, 0.0F, 20, 20, 20.0F, 20.0F);
                String name = this.fontRenderer.trimStringToWidth(entry.getResourcePackName().replace(".zip", ""), w - 40);
                this.fontRenderer.drawString(name, (float)(x + 26), (float)(y + 3), 0xFFFFFFFF, false);
                boolean warn = manager.warnIncompatible() && TexturePackManager.compatibility(entry.getPackFormat()) != null;
                this.fontRenderer.drawString(warn ? "⚠ format " + entry.getPackFormat() : "1.12 pack", (float)(x + 26), (float)(y + 13), warn ? 0xFFFFD24A : SythixTheme.TEXT_MUTED, false);

                if (manager.isFavorite(entry.getResourcePackName()))
                {
                    this.fontRenderer.drawString("★", (float)(x + w - 10), (float)(y + 3), 0xFFFFD24A, false);
                }
            }

            y += ROW;
        }

        SythixTheme.endClip();
        return scroll;
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        int lx = this.px + 6;
        int rx = this.px + 12 + this.colWidth();
        int w = this.colWidth();

        if (mouseY >= this.listTop() && mouseY < this.listBottom())
        {
            if (mouseX >= lx && mouseX < lx + w)
            {
                this.clickList(this.available, this.active, mouseY, this.scrollLeft, true);
            }
            else if (mouseX >= rx && mouseX < rx + w)
            {
                this.clickList(this.active, this.available, mouseY, this.scrollRight, false);
            }

            return;
        }

        int bw = (this.pw - 12 - 4 * 4) / 5;
        int by = this.py + this.ph - 22;

        if (mouseY < by || mouseY >= by + 16)
        {
            return;
        }

        int index = (mouseX - this.px - 6) / (bw + 4);

        if (index == 0 && this.selected != null)
        {
            this.manager().toggleFavorite(this.selected.getResourcePackName());
            SythixClient.get().save();
        }
        else if ((index == 1 || index == 2) && this.selected != null && this.active.contains(this.selected))
        {
            int i = this.active.indexOf(this.selected);
            int j = index == 1 ? i - 1 : i + 1;

            if (j >= 0 && j < this.active.size())
            {
                this.active.set(i, this.active.get(j));
                this.active.set(j, this.selected);
                this.changed = true;
            }
        }
        else if (index == 3)
        {
            java.io.File dir = this.mc.getResourcePackRepository().getDirResourcepacks();

            try
            {
                java.awt.Desktop.getDesktop().open(dir);
            }
            catch (Throwable t)
            {
                net.sythix.client.core.Notifications.post("Resource pack folder", dir.getAbsolutePath(), net.sythix.client.core.Notifications.Kind.INFO);
            }
        }
        else if (index == 4)
        {
            this.mc.displayGuiScreen(this.parent);
        }
    }

    /** Clicking a pack selects it; clicking a selected pack moves it to the other list. */
    private void clickList(List<ResourcePackRepository.Entry> from, List<ResourcePackRepository.Entry> to, int mouseY, float scroll, boolean toTop)
    {
        int index = (mouseY - this.listTop() + Math.round(scroll)) / ROW;

        if (index < 0 || index >= from.size())
        {
            return;
        }

        ResourcePackRepository.Entry entry = from.get(index);

        if (entry == this.selected)
        {
            from.remove(entry);

            if (toTop)
            {
                to.add(0, entry);
            }
            else
            {
                to.add(entry);
            }

            this.changed = true;
        }
        else
        {
            this.selected = entry;
        }
    }

    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();

        if (wheel != 0)
        {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;

            if (mouseX < this.px + this.pw / 2)
            {
                this.scrollLeft -= (float)wheel * 0.25F;
            }
            else
            {
                this.scrollRight -= (float)wheel * 0.25F;
            }
        }
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (keyCode == 1)
        {
            this.mc.displayGuiScreen(this.parent);
        }
    }

    public void onGuiClosed()
    {
        if (this.changed)
        {
            List<ResourcePackRepository.Entry> order = new ArrayList<ResourcePackRepository.Entry>(this.active);
            java.util.Collections.reverse(order);
            this.manager().apply(order);
        }
    }
}
