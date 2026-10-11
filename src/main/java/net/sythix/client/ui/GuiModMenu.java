package net.sythix.client.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.renderer.GlStateManager;
import net.sythix.client.SythixClient;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.module.client.SearchDiscovery;
import net.sythix.client.setting.Setting;
import net.sythix.client.ui.component.SettingsPanel;
import org.lwjgl.input.Mouse;

/**
 * The Sythix mod menu: browse modules by category, search, toggle, favourite and configure them.
 */
public class GuiModMenu extends GuiScreen
{
    private static final int SIDEBAR = 96;
    private static final int TOP = 26;
    private static final int CARD_HEIGHT = 42;
    private static final int GAP = 4;

    /** Remembered between openings. */
    private static String selectedTab = "All";

    private final GuiScreen parent;
    private GuiTextField search;
    private Module open;
    private final SettingsPanel panel = new SettingsPanel();
    private float scroll;
    private float targetScroll;
    private long openedTime;
    private int px;
    private int py;
    private int pw;
    private int ph;

    public GuiModMenu(GuiScreen parent)
    {
        this.parent = parent;
    }

    private List<String> tabs()
    {
        List<String> tabs = new ArrayList<String>();
        tabs.add("All");
        tabs.add("Favorites");

        for (Category category : Category.values())
        {
            tabs.add(category.getDisplayName());
        }

        return tabs;
    }

    public void initGui()
    {
        if (this.openedTime == 0L)
        {
            this.openedTime = Minecraft.getSystemTime();
        }

        this.pw = Math.min(this.width - 12, 470);
        this.ph = Math.min(this.height - 12, 300);
        this.px = (this.width - this.pw) / 2;
        this.py = (this.height - this.ph) / 2;
        String previous = this.search == null ? "" : this.search.getText();
        this.search = new GuiTextField(0, this.fontRenderer, this.px + SIDEBAR + 20, this.py + 9, this.pw - SIDEBAR - 110, 10);
        this.search.setEnableBackgroundDrawing(false);
        this.search.setMaxStringLength(40);
        this.search.setText(previous);
        this.search.setFocused(this.open == null);
        this.search.setTextColor(0xFFFFFF);
        this.layoutPanel();
    }

    private void layoutPanel()
    {
        if (this.open == null)
        {
            return;
        }

        int contentX = this.px + SIDEBAR + 6;
        int contentW = this.pw - SIDEBAR - 12;
        int top = this.py + TOP + 22;
        int previewW = this.open.hasPreview() ? Math.min(140, contentW / 3) : 0;
        this.panel.setBounds(contentX + (previewW > 0 ? previewW + 6 : 0), top, contentW - (previewW > 0 ? previewW + 6 : 0), this.py + this.ph - 30 - top);
        this.panel.setSettings(new ArrayList<Setting<?>>(this.open.getSettings()));
        this.panel.setOnChange(() -> {});
    }

    private List<Module> filtered()
    {
        List<Module> list = new ArrayList<Module>();
        String query = this.search == null ? "" : this.search.getText().trim().toLowerCase(Locale.ROOT);
        SearchDiscovery discovery = SythixClient.get().getModuleManager().get(SearchDiscovery.class);
        boolean searchDescriptions = discovery == null || discovery.searchDescriptions();

        for (Module module : SythixClient.get().getModuleManager().getModules())
        {
            boolean tabMatch = selectedTab.equals("All") || (selectedTab.equals("Favorites") && module.isFavorite()) || module.getCategory().getDisplayName().equals(selectedTab);

            if (!query.isEmpty())
            {
                // Searching always looks through every module
                tabMatch = true;
            }

            boolean queryMatch = query.isEmpty() || module.getName().toLowerCase(Locale.ROOT).contains(query) || module.getCategory().getDisplayName().toLowerCase(Locale.ROOT).contains(query) || (searchDescriptions && module.getDescription().toLowerCase(Locale.ROOT).contains(query));

            if (tabMatch && queryMatch)
            {
                list.add(module);
            }
        }

        if (discovery != null && discovery.favoritesFirst())
        {
            List<Module> sorted = new ArrayList<Module>();

            for (Module module : list)
            {
                if (module.isFavorite())
                {
                    sorted.add(module);
                }
            }

            for (Module module : list)
            {
                if (!module.isFavorite())
                {
                    sorted.add(module);
                }
            }

            return sorted;
        }

        return list;
    }

    private int columns()
    {
        int contentW = this.pw - SIDEBAR - 12;
        return Math.max(1, Math.min(3, contentW / 120));
    }

    private int cardWidth()
    {
        int contentW = this.pw - SIDEBAR - 12;
        return (contentW - GAP * (this.columns() - 1)) / this.columns();
    }

    private int gridTop()
    {
        return this.py + TOP + 4;
    }

    private int gridBottom()
    {
        return this.py + this.ph - 6;
    }

    private int contentHeight()
    {
        int rows = (this.filtered().size() + this.columns() - 1) / this.columns();
        return rows * (CARD_HEIGHT + GAP);
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        if (this.mc.world == null)
        {
            SythixTheme.drawBackground(this.width, this.height, mouseX, mouseY, 0.45F);
        }
        else
        {
            SythixTheme.drawRect(0.0F, 0.0F, (float)this.width, (float)this.height, 0x900B0B0E);
        }

        float appear = SythixTheme.reducedMotion ? 1.0F : Math.min(1.0F, (float)(Minecraft.getSystemTime() - this.openedTime) / 180.0F);
        GlStateManager.pushMatrix();
        float s = 0.96F + 0.04F * appear;
        GlStateManager.translate((float)this.width / 2.0F, (float)this.height / 2.0F, 0.0F);
        GlStateManager.scale(s, s, 1.0F);
        GlStateManager.translate((float)(-this.width) / 2.0F, (float)(-this.height) / 2.0F, 0.0F);

        // Window
        SythixTheme.drawRect((float)this.px, (float)this.py, (float)(this.px + this.pw), (float)(this.py + this.ph), SythixTheme.withAlpha(SythixTheme.CHARCOAL, (int)(0xF0 * SythixTheme.panelOpacity)));
        SythixTheme.drawRect((float)this.px, (float)this.py, (float)(this.px + SIDEBAR), (float)(this.py + this.ph), SythixTheme.withAlpha(SythixTheme.BLACK, (int)(0xC0 * SythixTheme.panelOpacity)));

        if (SythixTheme.borders)
        {
            SythixTheme.drawOutline((float)this.px, (float)this.py, (float)(this.px + this.pw), (float)(this.py + this.ph), SythixTheme.BORDER);
        }

        this.drawSidebar(mouseX, mouseY);

        if (this.open == null)
        {
            this.drawGrid(mouseX, mouseY);
        }
        else
        {
            this.drawModule(mouseX, mouseY);
        }

        GlStateManager.popMatrix();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawSidebar(int mouseX, int mouseY)
    {
        SythixTheme.drawTexture(SythixTheme.LOGO, (float)(this.px + 4), (float)(this.py + 2), 22.0F, 22.0F, 1.0F);
        this.fontRenderer.drawString(SythixClient.NAME, (float)(this.px + 26), (float)(this.py + 6), SythixTheme.RED, true);
        this.fontRenderer.drawString("Mods", (float)(this.px + 28 + this.fontRenderer.getStringWidth(SythixClient.NAME)), (float)(this.py + 6), SythixTheme.TEXT, true);
        this.fontRenderer.drawString(SythixClient.get().getModuleManager().getModules().size() + " modules", (float)(this.px + 26), (float)(this.py + 15), SythixTheme.TEXT_MUTED, false);
        SythixTheme.drawAccentLine((float)this.px, (float)(this.py + TOP - 1), (float)(this.px + SIDEBAR), 1.0F);
        int y = this.py + TOP + 3;
        int rowH = Math.max(11, Math.min(15, (this.ph - TOP - 46) / this.tabs().size()));

        for (String tab : this.tabs())
        {
            boolean selected = tab.equals(selectedTab) && this.open == null;
            boolean hovered = mouseX >= this.px && mouseX < this.px + SIDEBAR && mouseY >= y && mouseY < y + rowH;

            if (selected)
            {
                SythixTheme.drawRect((float)this.px, (float)y, (float)(this.px + SIDEBAR), (float)(y + rowH), 0x30D81820);
                SythixTheme.drawRect((float)this.px, (float)y, (float)(this.px + 2), (float)(y + rowH), SythixTheme.RED);
            }
            else if (hovered)
            {
                SythixTheme.drawRect((float)this.px, (float)y, (float)(this.px + SIDEBAR), (float)(y + rowH), 0x18FFFFFF);
            }

            String label = this.fontRenderer.trimStringToWidth(tab, SIDEBAR - 12);
            this.fontRenderer.drawString(label, (float)(this.px + 7), (float)(y + (rowH - 8) / 2), selected ? SythixTheme.TEXT : SythixTheme.TEXT_MUTED, false);
            y += rowH;
        }

        // Bottom shortcuts
        this.drawSideButton("HUD Editor", this.py + this.ph - 38, mouseX, mouseY);
        this.drawSideButton(SythixClient.NAME + " Settings", this.py + this.ph - 20, mouseX, mouseY);
    }

    private void drawSideButton(String text, int y, int mouseX, int mouseY)
    {
        boolean hovered = mouseX >= this.px + 4 && mouseX < this.px + SIDEBAR - 4 && mouseY >= y && mouseY < y + 15;
        SythixTheme.drawButton((float)(this.px + 4), (float)y, (float)(SIDEBAR - 8), 15.0F, true, hovered ? 1.0F : 0.0F);
        String label = this.fontRenderer.trimStringToWidth(text, SIDEBAR - 12);
        this.fontRenderer.drawString(label, (float)(this.px + SIDEBAR / 2 - this.fontRenderer.getStringWidth(label) / 2), (float)(y + 4), SythixTheme.TEXT, false);
    }

    private void drawGrid(int mouseX, int mouseY)
    {
        // Search bar
        int sx = this.px + SIDEBAR + 6;
        int sw = this.pw - SIDEBAR - 12;
        SythixTheme.drawRect((float)sx, (float)(this.py + 5), (float)(sx + sw), (float)(this.py + 21), 0xFF0F0F13);
        SythixTheme.drawOutline((float)sx, (float)(this.py + 5), (float)(sx + sw), (float)(this.py + 21), this.search.isFocused() ? SythixTheme.RED : SythixTheme.BORDER);
        this.fontRenderer.drawString("?", (float)(sx + 5), (float)(this.py + 9), SythixTheme.TEXT_MUTED, false);

        if (this.search.getText().isEmpty())
        {
            this.fontRenderer.drawString("Search modules...", (float)(sx + 14), (float)(this.py + 9), SythixTheme.TEXT_DISABLED, false);
        }

        this.search.drawTextBox();
        List<Module> list = this.filtered();
        String count = list.size() + " shown";
        this.fontRenderer.drawString(count, (float)(sx + sw - 5 - this.fontRenderer.getStringWidth(count)), (float)(this.py + 9), SythixTheme.TEXT_MUTED, false);

        // Cards
        this.targetScroll = Math.max(0.0F, Math.min((float)Math.max(0, this.contentHeight() - (this.gridBottom() - this.gridTop())), this.targetScroll));
        this.scroll += (this.targetScroll - this.scroll) * (SythixTheme.reducedMotion ? 1.0F : 0.35F);
        SythixTheme.beginClip(sx, this.gridTop(), sx + sw, this.gridBottom());
        int cw = this.cardWidth();

        for (int i = 0; i < list.size(); ++i)
        {
            Module module = list.get(i);
            int col = i % this.columns();
            int row = i / this.columns();
            int x = sx + col * (cw + GAP);
            int y = this.gridTop() + row * (CARD_HEIGHT + GAP) - Math.round(this.scroll);

            if (y + CARD_HEIGHT < this.gridTop() || y > this.gridBottom())
            {
                continue;
            }

            this.drawCard(module, x, y, cw, mouseX, mouseY);
        }

        SythixTheme.endClip();

        if (list.isEmpty())
        {
            String none = selectedTab.equals("Favorites") && this.search.getText().isEmpty() ? "No favourites yet - click the star on a module." : "No modules match your search.";
            this.fontRenderer.drawString(none, (float)(sx + sw / 2 - this.fontRenderer.getStringWidth(none) / 2), (float)(this.gridTop() + 30), SythixTheme.TEXT_MUTED, false);
        }
    }

    private void drawCard(Module module, int x, int y, int w, int mouseX, int mouseY)
    {
        boolean inGrid = mouseY >= this.gridTop() && mouseY < this.gridBottom();
        boolean hovered = inGrid && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + CARD_HEIGHT;
        SythixTheme.drawRect((float)x, (float)y, (float)(x + w), (float)(y + CARD_HEIGHT), hovered ? 0xFF24242C : 0xFF1C1C22);

        if (SythixTheme.borders)
        {
            SythixTheme.drawOutline((float)x, (float)y, (float)(x + w), (float)(y + CARD_HEIGHT), module.isEnabled() ? SythixTheme.withAlpha(SythixTheme.RED, 0xB0) : SythixTheme.BORDER);
        }

        if (module.isEnabled())
        {
            SythixTheme.drawGradientH((float)x, (float)(y + CARD_HEIGHT - 2), (float)(x + w), (float)(y + CARD_HEIGHT - 1), SythixTheme.RED, SythixTheme.BLUE);
        }

        String name = this.fontRenderer.trimStringToWidth(module.getName(), w - 34);
        this.fontRenderer.drawString(name, (float)(x + 5), (float)(y + 5), module.isEnabled() ? 0xFFFFFFFF : SythixTheme.TEXT, false);
        this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(module.getDescription(), w - 10), (float)(x + 5), (float)(y + 16), SythixTheme.TEXT_MUTED, false);
        this.fontRenderer.drawString(module.getCategory().getDisplayName(), (float)(x + 5), (float)(y + 28), SythixTheme.withAlpha(SythixTheme.LIGHT_BLUE, 0xC0), false);

        // Toggle switch (top right), or "Core" label
        int tx = x + w - 23;

        if (module.isCore())
        {
            this.fontRenderer.drawString("Core", (float)(x + w - 5 - this.fontRenderer.getStringWidth("Core")), (float)(y + 5), SythixTheme.TEXT_MUTED, false);
        }
        else
        {
            SythixTheme.drawRect((float)tx, (float)(y + 4), (float)(tx + 18), (float)(y + 13), module.isEnabled() ? SythixTheme.RED : 0xFF3A3A44);
            float knob = (float)tx + (module.isEnabled() ? 10.0F : 1.0F);
            SythixTheme.drawRect(knob, (float)(y + 5), knob + 7.0F, (float)(y + 12), 0xFFF0F0F4);
        }

        // Favourite star and settings gear (bottom right)
        boolean starHover = hovered && mouseX >= x + w - 30 && mouseX < x + w - 18 && mouseY >= y + 26;
        boolean gearHover = hovered && mouseX >= x + w - 16 && mouseY >= y + 26;
        this.fontRenderer.drawString("★", (float)(x + w - 28), (float)(y + 28), module.isFavorite() ? 0xFFFFD24A : (starHover ? SythixTheme.TEXT : SythixTheme.TEXT_DISABLED), false);
        this.fontRenderer.drawString("⚙", (float)(x + w - 14), (float)(y + 28), gearHover ? SythixTheme.RED : SythixTheme.TEXT_MUTED, false);
    }

    private void drawModule(int mouseX, int mouseY)
    {
        int sx = this.px + SIDEBAR + 6;
        int sw = this.pw - SIDEBAR - 12;
        Module module = this.open;
        boolean backHover = mouseX >= sx && mouseX < sx + 40 && mouseY >= this.py + 5 && mouseY < this.py + 21;
        this.fontRenderer.drawString("< Back", (float)sx, (float)(this.py + 9), backHover ? SythixTheme.RED : SythixTheme.TEXT_MUTED, false);
        this.fontRenderer.drawString(module.getName(), (float)(sx + 46), (float)(this.py + 9), 0xFFFFFFFF, true);
        int rx = sx + sw;

        // Enable switch
        if (!module.isCore())
        {
            SythixTheme.drawRect((float)(rx - 18), (float)(this.py + 9), (float)rx, (float)(this.py + 18), module.isEnabled() ? SythixTheme.RED : 0xFF3A3A44);
            float knob = (float)(rx - 18) + (module.isEnabled() ? 10.0F : 1.0F);
            SythixTheme.drawRect(knob, (float)(this.py + 10), knob + 7.0F, (float)(this.py + 17), 0xFFF0F0F4);
        }

        // Header buttons: Reset, Open (when the module has its own screen), Favourite
        int bx = rx - 24;
        bx = this.headerButton("Reset", bx, mouseX, mouseY);

        if (module.createScreen(this) != null)
        {
            bx = this.headerButton("Open", bx, mouseX, mouseY);
        }

        this.headerButton(module.isFavorite() ? "★ Fav" : "☆ Fav", bx, mouseX, mouseY);
        this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(module.getDescription(), sw), (float)sx, (float)(this.py + TOP + 6), SythixTheme.TEXT_MUTED, false);
        SythixTheme.drawAccentLine((float)sx, (float)(this.py + TOP + 17), (float)(sx + sw), 0.8F);

        if (module.hasPreview())
        {
            int previewW = Math.min(140, sw / 3);
            int top = this.py + TOP + 22;
            int h = Math.min(100, this.py + this.ph - 30 - top);

            try
            {
                module.renderPreview(sx, top, previewW, h);
            }
            catch (Throwable t)
            {
                net.sythix.client.core.ModuleErrors.report(module, "preview", t);
            }

            SythixTheme.drawOutline((float)sx, (float)top, (float)(sx + previewW), (float)(top + h), SythixTheme.BORDER);
        }

        this.panel.render(mouseX, mouseY);
        Setting<?> hovered = this.panel.getHoveredSetting();
        String hint = hovered != null ? hovered.getDescription() : "Click a value to change it. ↺ resets one setting.";
        this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(hint, sw), (float)sx, (float)(this.py + this.ph - 22), SythixTheme.TEXT_MUTED, false);
    }

    /** Draws a small header button ending at rightX; returns the x where the next button should end. */
    private int headerButton(String text, int rightX, int mouseX, int mouseY)
    {
        int w = this.fontRenderer.getStringWidth(text) + 10;
        int x = rightX - w;
        boolean hover = mouseX >= x && mouseX < rightX && mouseY >= this.py + 6 && mouseY < this.py + 20;
        SythixTheme.drawButton((float)x, (float)(this.py + 6), (float)w, 14.0F, true, hover ? 1.0F : 0.0F);
        this.fontRenderer.drawString(text, (float)(x + 5), (float)(this.py + 9), SythixTheme.TEXT, false);
        return x - 4;
    }

    private int headerButtonAt(int mouseX, int mouseY)
    {
        int sx = this.px + SIDEBAR + 6;
        int rx = sx + this.pw - SIDEBAR - 12;

        if (mouseY < this.py + 6 || mouseY >= this.py + 20)
        {
            return -1;
        }

        if (!this.open.isCore() && mouseX >= rx - 18 && mouseX < rx)
        {
            return 0;
        }

        int bx = rx - 24;
        String[] labels = this.open.createScreen(this) != null ? new String[] {"Reset", "Open", "Fav"} : new String[] {"Reset", "Fav"};

        for (int i = 0; i < labels.length; ++i)
        {
            String text = labels[i].equals("Fav") ? "★ Fav" : labels[i];
            int w = this.fontRenderer.getStringWidth(text) + 10;

            if (mouseX >= bx - w && mouseX < bx)
            {
                return labels[i].equals("Reset") ? 1 : (labels[i].equals("Open") ? 2 : 3);
            }

            bx = bx - w - 4;
        }

        return -1;
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        // Sidebar tabs
        if (mouseX >= this.px && mouseX < this.px + SIDEBAR)
        {
            int y = this.py + TOP + 3;
            int rowH = Math.max(11, Math.min(15, (this.ph - TOP - 46) / this.tabs().size()));

            for (String tab : this.tabs())
            {
                if (mouseY >= y && mouseY < y + rowH)
                {
                    selectedTab = tab;
                    this.open = null;
                    this.search.setText("");
                    this.targetScroll = 0.0F;
                    this.initGui();
                    return;
                }

                y += rowH;
            }

            if (mouseY >= this.py + this.ph - 38 && mouseY < this.py + this.ph - 23)
            {
                this.mc.displayGuiScreen(new GuiHudEditor(this));
            }
            else if (mouseY >= this.py + this.ph - 20 && mouseY < this.py + this.ph - 5)
            {
                this.mc.displayGuiScreen(new GuiSythixSettings(this));
            }

            return;
        }

        if (this.open != null)
        {
            int sx = this.px + SIDEBAR + 6;

            if (mouseX >= sx && mouseX < sx + 40 && mouseY >= this.py + 5 && mouseY < this.py + 21)
            {
                this.closeModule();
                return;
            }

            int button = this.headerButtonAt(mouseX, mouseY);

            if (button == 0)
            {
                this.open.toggle();
            }
            else if (button == 1)
            {
                this.mc.displayGuiScreen(new GuiYesNo(this, "Reset " + this.open.getName() + "?", "All of its settings go back to their defaults.", 1));
            }
            else if (button == 2)
            {
                this.mc.displayGuiScreen(this.open.createScreen(this));
            }
            else if (button == 3)
            {
                this.open.setFavorite(!this.open.isFavorite());
            }
            else
            {
                this.panel.mouseClicked(mouseX, mouseY, mouseButton);
            }

            return;
        }

        this.search.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseY < this.gridTop() || mouseY >= this.gridBottom())
        {
            return;
        }

        List<Module> list = this.filtered();
        int sx = this.px + SIDEBAR + 6;
        int cw = this.cardWidth();

        for (int i = 0; i < list.size(); ++i)
        {
            int x = sx + (i % this.columns()) * (cw + GAP);
            int y = this.gridTop() + (i / this.columns()) * (CARD_HEIGHT + GAP) - Math.round(this.scroll);

            if (mouseX >= x && mouseX < x + cw && mouseY >= y && mouseY < y + CARD_HEIGHT)
            {
                Module module = list.get(i);

                if (mouseY >= y + 26 && mouseX >= x + cw - 30 && mouseX < x + cw - 18)
                {
                    module.setFavorite(!module.isFavorite());
                }
                else if ((mouseY >= y + 26 && mouseX >= x + cw - 16) || mouseButton == 1 || module.isCore())
                {
                    this.openModule(module);
                }
                else
                {
                    module.toggle();
                }

                return;
            }
        }
    }

    private void openModule(Module module)
    {
        this.open = module;
        this.search.setFocused(false);
        this.layoutPanel();
    }

    private void closeModule()
    {
        this.open = null;
        this.search.setFocused(true);
        SythixClient.get().save();
    }

    public void confirmClicked(boolean result, int id)
    {
        if (result && id == 1 && this.open != null)
        {
            this.open.resetToDefaults();
        }

        this.mc.displayGuiScreen(this);
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
            if (this.open != null)
            {
                this.panel.scrollBy(wheel);
            }
            else
            {
                this.targetScroll -= (float)wheel * 0.3F;
            }
        }
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (this.open != null)
        {
            if (this.panel.keyTyped(typedChar, keyCode))
            {
                return;
            }

            if (keyCode == 1)
            {
                this.closeModule();
                return;
            }

            return;
        }

        if (keyCode == 1 || (keyCode == this.mc.gameSettings.keyBindSythixMenu.getKeyCode() && !this.search.isFocused()))
        {
            this.mc.displayGuiScreen(this.parent);
            return;
        }

        if (this.search.textboxKeyTyped(typedChar, keyCode))
        {
            this.targetScroll = 0.0F;
        }
    }

    public void updateScreen()
    {
        this.search.updateCursorCounter();
    }

    public void onGuiClosed()
    {
        SythixClient.get().save();
        this.mc.gameSettings.saveOptions();
    }

    public boolean doesGuiPauseGame()
    {
        return false;
    }
}
