package net.sythix.client.ui.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ColorSetting;
import net.sythix.client.setting.KeybindSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;
import net.sythix.client.setting.Setting;
import net.sythix.client.setting.TextSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.InfoSetting;
import net.sythix.client.setting.GameOptionSetting;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.client.gui.GuiScreen;
import net.sythix.client.ui.ColorUtil;
import net.sythix.client.ui.SythixTheme;

/**
 * A scrollable list of setting controls: toggle switches, sliders, mode selectors, colour
 * pickers and keybind buttons. Each changed setting shows a reset button.
 */
public class SettingsPanel
{
    private static final int ROW = 16;
    private static final int SLIDER_ROW = 24;
    private static final int PICKER_HEIGHT = 76;
    private static final int RESET_WIDTH = 10;

    private final Minecraft mc = Minecraft.getMinecraft();
    private int x;
    private int y;
    private int width;
    private int height;
    private List<Setting<?>> settings = Collections.emptyList();

    private float scroll;
    private float targetScroll;
    private long lastFrame;

    private NumberSetting draggingSlider;
    private GameOptionSetting draggingOption;
    private ColorSetting expandedColor;
    /** Which part of the colour picker is being dragged: 0 none, 1 saturation/brightness, 2 hue, 3 alpha. */
    private int colorDragPart;
    private float pickerHue;
    private KeybindSetting listeningKeybind;
    private TextSetting editingText;
    private Setting<?> hoveredSetting;

    /** Switch animations, keyed by position in the list. */
    private final List<Float> switchAnimations = new ArrayList<Float>();

    /** Called whenever a value changes (used to save). */
    private Runnable onChange = () -> {};

    public void setBounds(int x, int y, int width, int height)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.clampScroll();
    }

    public void setSettings(List<Setting<?>> settings)
    {
        if (this.settings != settings)
        {
            this.settings = settings;
            this.scroll = 0.0F;
            this.targetScroll = 0.0F;
            this.expandedColor = null;
            this.listeningKeybind = null;
            this.editingText = null;
            this.draggingSlider = null;
            this.draggingOption = null;
            this.colorDragPart = 0;
            this.switchAnimations.clear();
        }
    }

    public void setOnChange(Runnable onChange)
    {
        this.onChange = onChange;
    }

    public Setting<?> getHoveredSetting()
    {
        return this.hoveredSetting;
    }

    public boolean isListeningForKey()
    {
        return this.listeningKeybind != null || this.editingText != null;
    }

    private List<Setting<?>> visibleSettings()
    {
        List<Setting<?>> list = new ArrayList<Setting<?>>();

        for (Setting<?> setting : this.settings)
        {
            if (setting.isVisible())
            {
                list.add(setting);
            }
        }

        return list;
    }

    private int rowHeight(Setting<?> setting)
    {
        if (setting instanceof NumberSetting || (setting instanceof GameOptionSetting && ((GameOptionSetting)setting).isSlider()))
        {
            return SLIDER_ROW;
        }

        if (setting instanceof TextSetting)
        {
            return 30;
        }

        if (setting == this.expandedColor)
        {
            return ROW + PICKER_HEIGHT + (this.expandedColor.allowsAlpha() ? 10 : 0);
        }

        return ROW;
    }

    private int contentHeight()
    {
        int total = 0;

        for (Setting<?> setting : this.visibleSettings())
        {
            total += this.rowHeight(setting);
        }

        return total;
    }

    private void clampScroll()
    {
        float max = Math.max(0, this.contentHeight() - this.height);
        this.targetScroll = Math.max(0.0F, Math.min(max, this.targetScroll));
    }

    public void scrollBy(int wheel)
    {
        this.targetScroll -= (float)wheel * 0.25F;
        this.clampScroll();
    }

    public boolean isInside(int mouseX, int mouseY)
    {
        return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
    }

    public void render(int mouseX, int mouseY)
    {
        long now = Minecraft.getSystemTime();
        float seconds = this.lastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastFrame) / 1000.0F);
        this.lastFrame = now;
        this.clampScroll();
        this.scroll += (this.targetScroll - this.scroll) * Math.min(1.0F, seconds * 14.0F);
        this.hoveredSetting = null;

        if (this.draggingSlider != null)
        {
            this.updateSlider(this.draggingSlider, mouseX);
        }

        if (this.draggingOption != null)
        {
            this.draggingOption.setPercent((double)(mouseX - (this.x + 4)) / (double)Math.max(1, this.controlRight() - (this.x + 4)));
        }

        if (this.colorDragPart != 0 && this.expandedColor != null)
        {
            this.updateColorDrag(mouseX, mouseY);
        }

        List<Setting<?>> visible = this.visibleSettings();

        while (this.switchAnimations.size() < this.settings.size())
        {
            this.switchAnimations.add(Float.valueOf(-1.0F));
        }

        FontRenderer font = this.mc.fontRenderer;
        int rowY = this.y - Math.round(this.scroll);
        SythixTheme.beginClip(this.x, this.y, this.x + this.width, this.y + this.height);

        for (Setting<?> setting : visible)
        {
            int h = this.rowHeight(setting);

            if (rowY + h > this.y && rowY < this.y + this.height)
            {
                boolean hovered = this.isInside(mouseX, mouseY) && mouseY >= rowY && mouseY < rowY + h;

                if (hovered)
                {
                    this.hoveredSetting = setting;
                    SythixTheme.drawRect((float)this.x, (float)rowY, (float)(this.x + this.width), (float)(rowY + h), 0x18FFFFFF);
                }

                this.drawRow(setting, rowY, mouseX, mouseY, seconds, font);
            }

            rowY += h;
        }

        SythixTheme.endClip();

        // Scroll bar
        int content = this.contentHeight();

        if (content > this.height)
        {
            float barHeight = Math.max(12.0F, (float)this.height * (float)this.height / (float)content);
            float barY = (float)this.y + (this.scroll / (float)(content - this.height)) * ((float)this.height - barHeight);
            SythixTheme.drawRect((float)(this.x + this.width - 2), barY, (float)(this.x + this.width), barY + barHeight, 0x80FFFFFF);
        }
    }

    private int controlRight()
    {
        return this.x + this.width - 4 - RESET_WIDTH;
    }

    private void drawRow(Setting<?> setting, int rowY, int mouseX, int mouseY, float seconds, FontRenderer font)
    {
        int left = this.x + 4;
        int right = this.controlRight();
        font.drawString(setting.getName(), (float)left, (float)(rowY + 4), SythixTheme.TEXT, false);

        if (setting instanceof BooleanSetting)
        {
            this.drawSwitch((BooleanSetting)setting, right - 18, rowY + 4, seconds);
        }
        else if (setting instanceof ModeSetting)
        {
            String text = "< " + ((ModeSetting)setting).get() + " >";
            font.drawString(text, (float)(right - font.getStringWidth(text)), (float)(rowY + 4), SythixTheme.LIGHT_BLUE, false);
        }
        else if (setting instanceof NumberSetting)
        {
            NumberSetting number = (NumberSetting)setting;
            String value = number.getDisplayValue();
            font.drawString(value, (float)(right - font.getStringWidth(value)), (float)(rowY + 4), SythixTheme.TEXT_MUTED, false);
            float trackLeft = (float)left;
            float trackRight = (float)right;
            float trackY = (float)(rowY + 16);
            float filled = trackLeft + (trackRight - trackLeft) * (float)number.getPercent();
            SythixTheme.drawRect(trackLeft, trackY, trackRight, trackY + 3.0F, 0xFF2A2A33);
            SythixTheme.drawGradientH(trackLeft, trackY, filled, trackY + 3.0F, SythixTheme.RED, SythixTheme.BLUE);
            SythixTheme.drawRect(filled - 2.0F, trackY - 2.0F, filled + 2.0F, trackY + 5.0F, 0xFFFFFFFF);
        }
        else if (setting instanceof GameOptionSetting)
        {
            GameOptionSetting option = (GameOptionSetting)setting;
            String value = option.getDisplay();

            if (option.isSlider())
            {
                font.drawString(value, (float)(right - font.getStringWidth(value)), (float)(rowY + 4), SythixTheme.TEXT_MUTED, false);
                float trackY = (float)(rowY + 16);
                float filled = (float)left + (float)(right - left) * (float)option.getPercent();
                SythixTheme.drawRect((float)left, trackY, (float)right, trackY + 3.0F, 0xFF2A2A33);
                SythixTheme.drawGradientH((float)left, trackY, filled, trackY + 3.0F, SythixTheme.RED, SythixTheme.BLUE);
                SythixTheme.drawRect(filled - 2.0F, trackY - 2.0F, filled + 2.0F, trackY + 5.0F, 0xFFFFFFFF);
            }
            else
            {
                String text = "< " + value + " >";
                font.drawString(text, (float)(right - font.getStringWidth(text)), (float)(rowY + 4), SythixTheme.LIGHT_BLUE, false);
            }
        }
        else if (setting instanceof ButtonSetting)
        {
            String text = ((ButtonSetting)setting).getButtonText();
            int bw = font.getStringWidth(text) + 10;
            boolean hover = mouseX >= right - bw && mouseX < right && mouseY >= rowY + 2 && mouseY < rowY + 14;
            SythixTheme.drawRect((float)(right - bw), (float)(rowY + 2), (float)right, (float)(rowY + 14), hover ? 0xFF30303A : 0xFF24242C);
            SythixTheme.drawOutline((float)(right - bw), (float)(rowY + 2), (float)right, (float)(rowY + 14), hover ? SythixTheme.RED : SythixTheme.BORDER);
            font.drawString(text, (float)(right - bw + 5), (float)(rowY + 4), SythixTheme.TEXT, false);
        }
        else if (setting instanceof InfoSetting)
        {
            int labelWidth = font.getStringWidth(setting.getName()) + 8;
            String text = font.trimStringToWidth(((InfoSetting)setting).getText(), Math.max(10, right - left - labelWidth));
            font.drawString(text, (float)(right - font.getStringWidth(text)), (float)(rowY + 4), SythixTheme.TEXT_MUTED, false);
        }
        else if (setting instanceof ColorSetting)
        {
            ColorSetting color = (ColorSetting)setting;
            float sx = (float)(right - 18);
            float sy = (float)(rowY + 4);
            SythixTheme.drawRect(sx, sy, sx + 18.0F, sy + 9.0F, 0xFF3A3A44);
            SythixTheme.drawRect(sx, sy, sx + 9.0F, sy + 4.5F, 0xFF62626E);
            SythixTheme.drawRect(sx + 9.0F, sy + 4.5F, sx + 18.0F, sy + 9.0F, 0xFF62626E);
            SythixTheme.drawRect(sx, sy, sx + 18.0F, sy + 9.0F, color.getColor());
            SythixTheme.drawOutline(sx, sy, sx + 18.0F, sy + 9.0F, color == this.expandedColor ? SythixTheme.LIGHT_BLUE : 0xFF000000);

            if (color == this.expandedColor)
            {
                this.drawColorPicker(color, rowY + ROW, font);
            }
        }
        else if (setting instanceof TextSetting)
        {
            TextSetting text = (TextSetting)setting;
            boolean editing = text == this.editingText;
            float bx = (float)left;
            float by = (float)(rowY + 14);
            SythixTheme.drawRect(bx, by, (float)right, by + 12.0F, 0xFF0F0F13);
            SythixTheme.drawOutline(bx, by, (float)right, by + 12.0F, editing ? SythixTheme.RED : SythixTheme.BORDER);
            String value = text.get();
            String shown = font.trimStringToWidth(value, right - left - 8, true);
            String caret = editing && (Minecraft.getSystemTime() / 500L) % 2L == 0L ? "_" : "";
            font.drawString(shown + caret, bx + 3.0F, by + 2.0F, value.isEmpty() && !editing ? SythixTheme.TEXT_DISABLED : SythixTheme.TEXT, false);
        }
        else if (setting instanceof KeybindSetting)
        {
            KeybindSetting keybind = (KeybindSetting)setting;
            boolean listening = keybind == this.listeningKeybind;
            String text = listening ? ((Minecraft.getSystemTime() / 400L) % 2L == 0L ? "> Press a key <" : ">             <") : "[" + keybind.getKeyName() + "]";
            font.drawString(text, (float)(right - font.getStringWidth(text)), (float)(rowY + 4), listening ? SythixTheme.RED : SythixTheme.LIGHT_BLUE, false);
        }

        if (!setting.isDefault())
        {
            boolean hoverReset = mouseX >= right + 2 && mouseX < right + 2 + RESET_WIDTH && mouseY >= rowY + 2 && mouseY < rowY + 14;
            font.drawString("\u21ba", (float)(right + 3), (float)(rowY + 4), hoverReset ? SythixTheme.RED : SythixTheme.TEXT_MUTED, false);
        }
    }

    private void drawSwitch(BooleanSetting setting, int sx, int sy, float seconds)
    {
        int index = this.settings.indexOf(setting);
        float target = setting.isOn() ? 1.0F : 0.0F;
        float anim = this.switchAnimations.get(index).floatValue();
        anim = anim < 0.0F ? target : SythixTheme.approach(anim, target, 8.0F, seconds);
        this.switchAnimations.set(index, Float.valueOf(anim));
        SythixTheme.drawRect((float)sx, (float)sy, (float)(sx + 18), (float)(sy + 9), SythixTheme.lerpColor(0xFF3A3A44, SythixTheme.RED, anim));
        float knobX = (float)sx + 1.0F + anim * 9.0F;
        SythixTheme.drawRect(knobX, (float)(sy + 1), knobX + 7.0F, (float)(sy + 8), 0xFFF0F0F4);
    }

    // Colour picker layout, relative to the top of the picker area
    private int pickerLeft()
    {
        return this.x + 4;
    }

    private int pickerRight()
    {
        return this.controlRight();
    }

    private void drawColorPicker(ColorSetting color, int top, FontRenderer font)
    {
        int left = this.pickerLeft();
        int right = this.pickerRight();
        float[] hsb = ColorUtil.rgbToHsb(color.getArgb());

        if (hsb[1] > 0.01F && hsb[2] > 0.01F)
        {
            this.pickerHue = hsb[0];
        }

        // Saturation (left to right) and brightness (top to bottom) for the current hue
        int pure = ColorUtil.hsbToRgb(this.pickerHue, 1.0F, 1.0F) | 0xFF000000;
        SythixTheme.drawGradientH((float)left, (float)top, (float)right, (float)(top + 44), 0xFFFFFFFF, pure);
        SythixTheme.drawGradientV((float)left, (float)top, (float)right, (float)(top + 44), 0x00000000, 0xFF000000);
        float cx = (float)left + hsb[1] * (float)(right - left);
        float cy = (float)top + (1.0F - hsb[2]) * 44.0F;
        SythixTheme.drawOutline(cx - 2.0F, cy - 2.0F, cx + 2.0F, cy + 2.0F, 0xFFFFFFFF);

        // Hue bar
        int hueTop = top + 48;
        float segment = (float)(right - left) / 6.0F;

        for (int i = 0; i < 6; ++i)
        {
            int from = ColorUtil.hsbToRgb((float)i / 6.0F, 1.0F, 1.0F) | 0xFF000000;
            int to = ColorUtil.hsbToRgb((float)(i + 1) / 6.0F, 1.0F, 1.0F) | 0xFF000000;
            SythixTheme.drawGradientH((float)left + segment * (float)i, (float)hueTop, (float)left + segment * (float)(i + 1), (float)(hueTop + 6), from, to);
        }

        float hx = (float)left + this.pickerHue * (float)(right - left);
        SythixTheme.drawRect(hx - 1.0F, (float)(hueTop - 1), hx + 1.0F, (float)(hueTop + 7), 0xFFFFFFFF);
        int nextTop = hueTop + 10;

        // Alpha bar
        if (color.allowsAlpha())
        {
            SythixTheme.drawRect((float)left, (float)nextTop, (float)right, (float)(nextTop + 6), 0xFF62626E);
            SythixTheme.drawGradientH((float)left, (float)nextTop, (float)right, (float)(nextTop + 6), color.getArgb() & 0xFFFFFF, color.getArgb() | 0xFF000000);
            float ax = (float)left + (float)((color.getArgb() >>> 24) & 255) / 255.0F * (float)(right - left);
            SythixTheme.drawRect(ax - 1.0F, (float)(nextTop - 1), ax + 1.0F, (float)(nextTop + 7), 0xFFFFFFFF);
            nextTop += 10;
        }

        // Chroma toggle and hex value
        String chroma = "Chroma: " + (color.isChroma() ? "ON" : "OFF");
        font.drawString(chroma, (float)left, (float)(nextTop + 2), color.isChroma() ? SythixTheme.LIGHT_BLUE : SythixTheme.TEXT_MUTED, false);
        String hex = String.format(color.allowsAlpha() ? "#%08X" : "#%06X", color.allowsAlpha() ? color.getArgb() : color.getArgb() & 0xFFFFFF);
        font.drawString(hex, (float)(right - font.getStringWidth(hex)), (float)(nextTop + 2), SythixTheme.TEXT_MUTED, false);
    }

    private void updateSlider(NumberSetting setting, int mouseX)
    {
        int left = this.x + 4;
        int right = this.controlRight();
        setting.setPercent((double)(mouseX - left) / (double)Math.max(1, right - left));
        this.onChange.run();
    }

    private void updateColorDrag(int mouseX, int mouseY)
    {
        ColorSetting color = this.expandedColor;
        int top = this.rowTop(color) + ROW;
        int left = this.pickerLeft();
        int right = this.pickerRight();
        float fx = Math.max(0.0F, Math.min(1.0F, (float)(mouseX - left) / (float)Math.max(1, right - left)));
        int alpha = (color.getArgb() >>> 24) & 255;
        float[] hsb = ColorUtil.rgbToHsb(color.getArgb());

        if (this.colorDragPart == 1)
        {
            float fy = Math.max(0.0F, Math.min(1.0F, (float)(mouseY - top) / 44.0F));
            color.set(Integer.valueOf(alpha << 24 | ColorUtil.hsbToRgb(this.pickerHue, fx, 1.0F - fy)));
        }
        else if (this.colorDragPart == 2)
        {
            this.pickerHue = Math.min(0.999F, fx);
            color.set(Integer.valueOf(alpha << 24 | ColorUtil.hsbToRgb(this.pickerHue, Math.max(hsb[1], 0.05F), Math.max(hsb[2], 0.05F))));
        }
        else if (this.colorDragPart == 3)
        {
            color.set(Integer.valueOf(Math.round(fx * 255.0F) << 24 | (color.getArgb() & 0xFFFFFF)));
        }

        this.onChange.run();
    }

    /** Screen y of a setting's row, taking scrolling into account. */
    private int rowTop(Setting<?> target)
    {
        int rowY = this.y - Math.round(this.scroll);

        for (Setting<?> setting : this.visibleSettings())
        {
            if (setting == target)
            {
                return rowY;
            }

            rowY += this.rowHeight(setting);
        }

        return rowY;
    }

    /** Returns true if the click was handled by the panel. */
    public boolean mouseClicked(int mouseX, int mouseY, int button)
    {
        if (!this.isInside(mouseX, mouseY))
        {
            this.listeningKeybind = null;
            this.editingText = null;
            return false;
        }

        int rowY = this.y - Math.round(this.scroll);
        int right = this.controlRight();

        for (Setting<?> setting : this.visibleSettings())
        {
            int h = this.rowHeight(setting);

            if (mouseY >= rowY && mouseY < rowY + h)
            {
                this.listeningKeybind = null;
                this.editingText = null;

                // Reset button
                if (!setting.isDefault() && mouseX >= right + 2 && mouseY < rowY + 14)
                {
                    setting.reset();
                    this.onChange.run();
                    return true;
                }

                if (setting instanceof BooleanSetting && mouseY < rowY + ROW)
                {
                    ((BooleanSetting)setting).toggle();
                }
                else if (setting instanceof ModeSetting)
                {
                    ((ModeSetting)setting).cycle(button == 1 ? -1 : 1);
                }
                else if (setting instanceof GameOptionSetting)
                {
                    GameOptionSetting option = (GameOptionSetting)setting;

                    if (option.isSlider())
                    {
                        this.draggingOption = option;
                    }
                    else
                    {
                        option.cycle();
                    }
                }
                else if (setting instanceof ButtonSetting)
                {
                    ((ButtonSetting)setting).press();
                    return true;
                }
                else if (setting instanceof NumberSetting)
                {
                    this.draggingSlider = (NumberSetting)setting;
                    this.updateSlider(this.draggingSlider, mouseX);
                }
                else if (setting instanceof KeybindSetting)
                {
                    this.listeningKeybind = (KeybindSetting)setting;
                    return true;
                }
                else if (setting instanceof TextSetting)
                {
                    this.editingText = (TextSetting)setting;
                    return true;
                }
                else if (setting instanceof ColorSetting)
                {
                    this.clickColor((ColorSetting)setting, rowY, mouseX, mouseY);
                }

                this.onChange.run();
                return true;
            }

            rowY += h;
        }

        return true;
    }

    private void clickColor(ColorSetting color, int rowY, int mouseX, int mouseY)
    {
        if (mouseY < rowY + ROW || color != this.expandedColor)
        {
            this.expandedColor = color == this.expandedColor ? null : color;

            if (this.expandedColor != null)
            {
                this.pickerHue = ColorUtil.rgbToHsb(color.getArgb())[0];
                // Scroll so the opened picker is in view
                int bottom = rowY + this.rowHeight(color);

                if (bottom > this.y + this.height)
                {
                    this.targetScroll += (float)(Math.min(bottom - (this.y + this.height), rowY - this.y));
                }
            }

            this.clampScroll();
            return;
        }

        int top = rowY + ROW;
        int hueTop = top + 48;
        int alphaTop = hueTop + 10;
        int chromaTop = color.allowsAlpha() ? alphaTop + 10 : alphaTop;

        if (mouseY < top + 44)
        {
            this.colorDragPart = 1;
        }
        else if (mouseY >= hueTop - 2 && mouseY < hueTop + 8)
        {
            this.colorDragPart = 2;
        }
        else if (color.allowsAlpha() && mouseY >= alphaTop - 2 && mouseY < alphaTop + 8)
        {
            this.colorDragPart = 3;
        }
        else if (mouseY >= chromaTop)
        {
            color.setChroma(!color.isChroma());
            return;
        }

        if (this.colorDragPart != 0)
        {
            this.updateColorDrag(mouseX, mouseY);
        }
    }

    public void mouseReleased()
    {
        if (this.draggingSlider != null || this.colorDragPart != 0)
        {
            this.onChange.run();
        }

        this.draggingSlider = null;
        this.draggingOption = null;
        this.colorDragPart = 0;
    }

    /** Returns true if the key was used (keybind capture or text editing). */
    public boolean keyTyped(char typedChar, int keyCode)
    {
        if (this.editingText != null)
        {
            TextSetting text = this.editingText;

            if (keyCode == 1 || keyCode == 28 || keyCode == 156)
            {
                // Escape or Enter finishes editing
                this.editingText = null;
            }
            else if (keyCode == 14)
            {
                String v = text.get();
                text.set(v.isEmpty() ? v : v.substring(0, v.length() - 1));
            }
            else if (GuiScreen.isKeyComboCtrlV(keyCode))
            {
                text.set(text.get() + ChatAllowedCharacters.filterAllowedCharacters(GuiScreen.getClipboardString()));
            }
            else if (ChatAllowedCharacters.isAllowedCharacter(typedChar))
            {
                text.set(text.get() + typedChar);
            }

            this.onChange.run();
            return true;
        }

        if (this.listeningKeybind == null)
        {
            return false;
        }

        if (keyCode == 14 || keyCode == 211)
        {
            // Backspace or Delete clears the binding
            this.listeningKeybind.set(Integer.valueOf(KeybindSetting.NONE));
        }
        else if (keyCode != 1)
        {
            this.listeningKeybind.set(Integer.valueOf(keyCode));
        }

        this.listeningKeybind = null;
        this.onChange.run();
        return true;
    }
}
