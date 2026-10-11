package net.sythix.client.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.sythix.client.SythixClient;
import net.sythix.client.input.InputUtil;
import net.sythix.client.module.hud.HudModule;
import net.sythix.client.setting.Setting;
import net.sythix.client.ui.component.SettingsPanel;
import org.lwjgl.input.Mouse;

/**
 * Lets the player move, resize, toggle and configure HUD elements.
 *
 * Drag an element to move it (it snaps to screen edges, the centre and other elements; hold
 * Shift to place freely). Scroll over it to resize. Click it to edit its settings in the panel.
 */
public class GuiHudEditor extends GuiScreen
{
    private static final int PANEL_WIDTH = 176;
    private static final int SNAP_DISTANCE = 4;
    private static final int LIST_ROW = 16;
    private static final int HEADER = 22;
    private static boolean panelOnLeft;

    private final GuiScreen parent;
    private final SettingsPanel settingsPanel = new SettingsPanel();
    private HudModule selected;
    private HudModule dragging;
    private float dragOffsetX;
    private float dragOffsetY;
    private Float guideX;
    private Float guideY;
    private long openedTime;

    public GuiHudEditor(GuiScreen parent)
    {
        this.parent = parent;
        this.settingsPanel.setOnChange(() -> {});
    }

    private List<HudModule> modules()
    {
        return SythixClient.get().getModuleManager().getHudModules();
    }

    private int panelX()
    {
        return panelOnLeft ? 0 : this.width - PANEL_WIDTH;
    }

    private int listTop()
    {
        return HEADER + 2;
    }

    private int settingsTop()
    {
        return this.listTop() + this.modules().size() * LIST_ROW + 18;
    }

    private int footerTop()
    {
        return this.height - 58;
    }

    public void initGui()
    {
        if (this.openedTime == 0L)
        {
            this.openedTime = Minecraft.getSystemTime();
        }

        if (this.selected == null && !this.modules().isEmpty())
        {
            this.selected = this.modules().get(0);
        }

        int px = this.panelX();
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(0, px + 6, this.height - 24, 80, 18, "Reset All"));
        this.buttonList.add(new GuiButton(1, px + PANEL_WIDTH - 86, this.height - 24, 80, 18, "Done"));
        this.buttonList.add(new GuiButton(2, px + PANEL_WIDTH - 22, 3, 16, 16, panelOnLeft ? ">" : "<"));
        this.buttonList.add(new GuiButton(3, px + PANEL_WIDTH - 70, this.settingsTop() - 16, 64, 14, "Reset"));
        this.updateSettingsPanel();
    }

    private void updateSettingsPanel()
    {
        this.settingsPanel.setBounds(this.panelX(), this.settingsTop(), PANEL_WIDTH, this.footerTop() - this.settingsTop() - 2);
        this.settingsPanel.setSettings(this.selected == null ? new ArrayList<Setting<?>>() : this.selected.getSettings());
    }

    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button.id == 0)
        {
            this.mc.displayGuiScreen(new GuiYesNo(this, "Reset every HUD element?", "Positions, sizes, colours and toggles go back to their defaults.", 0));
        }
        else if (button.id == 1)
        {
            this.mc.displayGuiScreen(this.parent);
        }
        else if (button.id == 2)
        {
            panelOnLeft = !panelOnLeft;
            this.initGui();
        }
        else if (button.id == 3 && this.selected != null)
        {
            boolean enabled = this.selected.isEnabled();
            this.selected.resetToDefaults();
            this.selected.setEnabled(enabled);
        }
    }

    public void confirmClicked(boolean result, int id)
    {
        if (result && id == 0)
        {
            SythixClient.get().getModuleManager().resetAll();
            SythixClient.get().save();
        }

        this.mc.displayGuiScreen(this);
    }

    public void onGuiClosed()
    {
        SythixClient.get().save();
    }

    public boolean doesGuiPauseGame()
    {
        return false;
    }

    private boolean isOverPanel(int mouseX, int mouseY)
    {
        return mouseX >= this.panelX() && mouseX < this.panelX() + PANEL_WIDTH;
    }

    private HudModule moduleAt(int mouseX, int mouseY, ScaledResolution res)
    {
        List<HudModule> list = this.modules();

        for (int i = list.size() - 1; i >= 0; --i)
        {
            HudModule module = list.get(i);

            if (module.isEnabled())
            {
                float x = module.getScreenX(res);
                float y = module.getScreenY(res);

                if ((float)mouseX >= x && (float)mouseX < x + module.getScaledWidth() && (float)mouseY >= y && (float)mouseY < y + module.getScaledHeight())
                {
                    return module;
                }
            }
        }

        return null;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        ScaledResolution res = new ScaledResolution(this.mc);

        if (this.mc.world == null)
        {
            SythixTheme.drawBackground(this.width, this.height, -1, -1, 0.35F);
        }
        else
        {
            SythixTheme.drawRect(0.0F, 0.0F, (float)this.width, (float)this.height, 0x400B0B0E);
        }

        // Centre guide lines (faint), brighter while snapped
        SythixTheme.drawRect((float)this.width / 2.0F - 0.5F, 0.0F, (float)this.width / 2.0F + 0.5F, (float)this.height, 0x14FFFFFF);
        SythixTheme.drawRect(0.0F, (float)this.height / 2.0F - 0.5F, (float)this.width, (float)this.height / 2.0F + 0.5F, 0x14FFFFFF);

        if (this.dragging != null)
        {
            this.updateDrag(mouseX, mouseY, res);
        }

        if (this.guideX != null)
        {
            SythixTheme.drawRect(this.guideX.floatValue() - 0.5F, 0.0F, this.guideX.floatValue() + 0.5F, (float)this.height, 0xC06CC6FF);
        }

        if (this.guideY != null)
        {
            SythixTheme.drawRect(0.0F, this.guideY.floatValue() - 0.5F, (float)this.width, this.guideY.floatValue() + 0.5F, 0xC06CC6FF);
        }

        HudModule hovered = this.isOverPanel(mouseX, mouseY) ? null : this.moduleAt(mouseX, mouseY, res);

        for (HudModule module : this.modules())
        {
            if (!module.isEnabled())
            {
                continue;
            }

            module.render(res, true);
            float x = module.getScreenX(res);
            float y = module.getScreenY(res);
            float w = module.getScaledWidth();
            float h = module.getScaledHeight();
            int outline = module == this.selected ? SythixTheme.RED : (module == hovered ? SythixTheme.LIGHT_BLUE : 0x50FFFFFF);
            SythixTheme.drawOutline(x - 1.0F, y - 1.0F, x + w + 1.0F, y + h + 1.0F, outline);

            if (module == hovered || module == this.dragging)
            {
                String info = module.getName() + "  " + Math.round(module.getScale() * 100.0F) + "%";
                float ty = y > 11.0F ? y - 10.0F : y + h + 2.0F;
                this.fontRenderer.drawString(info, x, ty, 0xFFFFFFFF, true);
            }
        }

        this.drawPanel(mouseX, mouseY);
        // Hints, centred in the free area beside the panel
        int areaLeft = panelOnLeft ? PANEL_WIDTH : 0;
        int areaWidth = this.width - PANEL_WIDTH;
        String[] hints = new String[] {"Drag: move  |  Scroll: resize", "Shift: no snapping  |  Arrows: nudge"};

        for (int i = 0; i < hints.length; ++i)
        {
            int hintWidth = this.fontRenderer.getStringWidth(hints[i]);

            if (hintWidth <= areaWidth - 4)
            {
                this.fontRenderer.drawString(hints[i], (float)(areaLeft + (areaWidth - hintWidth) / 2), (float)(this.height - 21 + i * 10), 0x90FFFFFF, true);
            }
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawPanel(int mouseX, int mouseY)
    {
        int px = this.panelX();
        float slide = Math.min(1.0F, (float)(Minecraft.getSystemTime() - this.openedTime) / 200.0F);
        SythixTheme.drawRect((float)px, 0.0F, (float)(px + PANEL_WIDTH), (float)this.height, SythixTheme.withAlpha(SythixTheme.CHARCOAL, (int)(0xEE * slide)));
        SythixTheme.drawRect(panelOnLeft ? (float)(px + PANEL_WIDTH - 1) : (float)px, 0.0F, panelOnLeft ? (float)(px + PANEL_WIDTH) : (float)(px + 1), (float)this.height, SythixTheme.BORDER);

        // Header
        this.fontRenderer.drawString(SythixClient.NAME, (float)(px + 6), 7.0F, SythixTheme.RED, true);
        this.fontRenderer.drawString("HUD Editor", (float)(px + 8 + this.fontRenderer.getStringWidth(SythixClient.NAME)), 7.0F, SythixTheme.TEXT, true);
        SythixTheme.drawAccentLine((float)px, (float)HEADER, (float)(px + PANEL_WIDTH), 1.0F);

        // Element list with toggle switches
        int rowY = this.listTop();

        for (HudModule module : this.modules())
        {
            boolean hovered = mouseX >= px && mouseX < px + PANEL_WIDTH && mouseY >= rowY && mouseY < rowY + LIST_ROW;

            if (module == this.selected)
            {
                SythixTheme.drawRect((float)px, (float)rowY, (float)(px + PANEL_WIDTH), (float)(rowY + LIST_ROW), 0x30D81820);
                SythixTheme.drawRect((float)px, (float)rowY, (float)(px + 2), (float)(rowY + LIST_ROW), SythixTheme.RED);
            }
            else if (hovered)
            {
                SythixTheme.drawRect((float)px, (float)rowY, (float)(px + PANEL_WIDTH), (float)(rowY + LIST_ROW), 0x18FFFFFF);
            }

            this.fontRenderer.drawString(module.getName(), (float)(px + 8), (float)(rowY + 4), module.isEnabled() ? SythixTheme.TEXT : SythixTheme.TEXT_MUTED, false);
            int sx = px + PANEL_WIDTH - 26;
            SythixTheme.drawRect((float)sx, (float)(rowY + 4), (float)(sx + 18), (float)(rowY + 13), module.isEnabled() ? SythixTheme.RED : 0xFF3A3A44);
            float knob = (float)sx + (module.isEnabled() ? 10.0F : 1.0F);
            SythixTheme.drawRect(knob, (float)(rowY + 5), knob + 7.0F, (float)(rowY + 12), 0xFFF0F0F4);
            rowY += LIST_ROW;
        }

        // Selected element title above its settings
        if (this.selected != null)
        {
            int titleY = this.settingsTop() - 14;
            this.fontRenderer.drawString(this.selected.getName() + " Settings", (float)(px + 6), (float)titleY, SythixTheme.LIGHT_BLUE, true);
            SythixTheme.drawRect((float)(px + 4), (float)(this.settingsTop() - 2), (float)(px + PANEL_WIDTH - 4), (float)(this.settingsTop() - 1), SythixTheme.BORDER);
        }

        this.settingsPanel.render(mouseX, mouseY);

        // Description of the hovered setting (or the selected element)
        int footer = this.footerTop();
        SythixTheme.drawRect((float)(px + 4), (float)footer, (float)(px + PANEL_WIDTH - 4), (float)(footer + 1), SythixTheme.BORDER);
        Setting<?> hoveredSetting = this.settingsPanel.getHoveredSetting();
        String description = hoveredSetting != null ? hoveredSetting.getDescription() : (this.selected != null ? this.selected.getDescription() : "");
        List<String> lines = this.fontRenderer.listFormattedStringToWidth(description, PANEL_WIDTH - 12);

        for (int i = 0; i < Math.min(3, lines.size()); ++i)
        {
            this.fontRenderer.drawString(lines.get(i), (float)(px + 6), (float)(footer + 4 + i * 9), SythixTheme.TEXT_MUTED, false);
        }
    }

    private void updateDrag(int mouseX, int mouseY, ScaledResolution res)
    {
        HudModule module = this.dragging;
        float w = module.getScaledWidth();
        float h = module.getScaledHeight();
        float x = (float)mouseX - this.dragOffsetX;
        float y = (float)mouseY - this.dragOffsetY;
        this.guideX = null;
        this.guideY = null;

        if (!InputUtil.isShiftDown())
        {
            float bestX = SNAP_DISTANCE + 1;
            float bestY = SNAP_DISTANCE + 1;
            float snappedX = x;
            float snappedY = y;
            List<float[]> xTargets = new ArrayList<float[]>();
            List<float[]> yTargets = new ArrayList<float[]>();
            // {target position for the element's left/top edge, guide line position}
            xTargets.add(new float[] {0.0F, 0.0F});
            xTargets.add(new float[] {(float)this.width - w, (float)this.width});
            xTargets.add(new float[] {((float)this.width - w) / 2.0F, (float)this.width / 2.0F});
            yTargets.add(new float[] {0.0F, 0.0F});
            yTargets.add(new float[] {(float)this.height - h, (float)this.height});
            yTargets.add(new float[] {((float)this.height - h) / 2.0F, (float)this.height / 2.0F});

            for (HudModule other : this.modules())
            {
                if (other == module || !other.isEnabled())
                {
                    continue;
                }

                float ox = other.getScreenX(res);
                float oy = other.getScreenY(res);
                float ow = other.getScaledWidth();
                float oh = other.getScaledHeight();
                xTargets.add(new float[] {ox, ox});
                xTargets.add(new float[] {ox + ow - w, ox + ow});
                xTargets.add(new float[] {ox + ow + 2.0F, ox + ow + 1.0F});
                xTargets.add(new float[] {ox - w - 2.0F, ox - 1.0F});
                yTargets.add(new float[] {oy, oy});
                yTargets.add(new float[] {oy + oh - h, oy + oh});
                yTargets.add(new float[] {oy + oh + 2.0F, oy + oh + 1.0F});
                yTargets.add(new float[] {oy - h - 2.0F, oy - 1.0F});
            }

            for (float[] target : xTargets)
            {
                float distance = Math.abs(x - target[0]);

                if (distance < bestX)
                {
                    bestX = distance;
                    snappedX = target[0];
                    this.guideX = Float.valueOf(target[1]);
                }
            }

            for (float[] target : yTargets)
            {
                float distance = Math.abs(y - target[0]);

                if (distance < bestY)
                {
                    bestY = distance;
                    snappedY = target[0];
                    this.guideY = Float.valueOf(target[1]);
                }
            }

            x = snappedX;
            y = snappedY;
        }

        module.setScreenPosition(x, y, res);
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (this.isOverPanel(mouseX, mouseY))
        {
            // Element list
            int index = (mouseY - this.listTop()) / LIST_ROW;

            if (mouseY >= this.listTop() && index >= 0 && index < this.modules().size())
            {
                HudModule module = this.modules().get(index);

                if (mouseX >= this.panelX() + PANEL_WIDTH - 30)
                {
                    module.toggle();
                }

                this.selected = module;
                this.initGui();
                return;
            }

            this.settingsPanel.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        ScaledResolution res = new ScaledResolution(this.mc);
        HudModule module = this.moduleAt(mouseX, mouseY, res);

        if (module != null)
        {
            if (module != this.selected)
            {
                this.selected = module;
                this.initGui();
            }

            if (mouseButton == 0)
            {
                this.dragging = module;
                this.dragOffsetX = (float)mouseX - module.getScreenX(res);
                this.dragOffsetY = (float)mouseY - module.getScreenY(res);
            }
        }
    }

    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        super.mouseReleased(mouseX, mouseY, state);
        this.dragging = null;
        this.guideX = null;
        this.guideY = null;
        this.settingsPanel.mouseReleased();
    }

    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();

        if (wheel != 0)
        {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

            if (this.isOverPanel(mouseX, mouseY))
            {
                this.settingsPanel.scrollBy(wheel);
                return;
            }

            ScaledResolution res = new ScaledResolution(this.mc);
            HudModule module = this.moduleAt(mouseX, mouseY, res);

            if (module != null)
            {
                // Keep the element's centre in place while resizing
                float cx = module.getScreenX(res) + module.getScaledWidth() / 2.0F;
                float cy = module.getScreenY(res) + module.getScaledHeight() / 2.0F;
                module.getScaleSetting().set(Double.valueOf(module.getScaleSetting().get().doubleValue() + (wheel > 0 ? 0.05D : -0.05D)));
                module.setScreenPosition(cx - module.getScaledWidth() / 2.0F, cy - module.getScaledHeight() / 2.0F, res);
            }
        }
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (this.settingsPanel.keyTyped(typedChar, keyCode))
        {
            return;
        }

        if (this.selected != null && this.selected.isEnabled() && keyCode >= 200 && keyCode <= 208)
        {
            ScaledResolution res = new ScaledResolution(this.mc);
            float x = this.selected.getScreenX(res);
            float y = this.selected.getScreenY(res);

            switch (keyCode)
            {
                case 200: --y; break;
                case 208: ++y; break;
                case 203: --x; break;
                case 205: ++x; break;
                default: break;
            }

            this.selected.setScreenPosition(x, y, res);
            return;
        }

        if (keyCode == 1 || keyCode == this.mc.gameSettings.keyBindSythixMenu.getKeyCode())
        {
            this.mc.displayGuiScreen(this.parent);
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }
}
