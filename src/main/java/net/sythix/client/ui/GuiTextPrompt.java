package net.sythix.client.ui;

import java.io.IOException;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

/**
 * Asks for one line of text, then calls back and returns to the parent screen.
 */
public class GuiTextPrompt extends GuiScreen
{
    private final GuiScreen parent;
    private final String title;
    private final String hint;
    private final String initial;
    private final int maxLength;
    private final Consumer<String> onDone;
    private GuiTextField field;

    public GuiTextPrompt(GuiScreen parent, String title, String hint, String initial, int maxLength, Consumer<String> onDone)
    {
        this.parent = parent;
        this.title = title;
        this.hint = hint;
        this.initial = initial;
        this.maxLength = maxLength;
        this.onDone = onDone;
    }

    public void initGui()
    {
        this.field = new GuiTextField(0, this.fontRenderer, this.width / 2 - 120, this.height / 2 - 6, 240, 14);
        this.field.setMaxStringLength(this.maxLength);
        this.field.setText(this.initial);
        this.field.setFocused(true);
        this.field.setEnableBackgroundDrawing(false);
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        SythixTheme.drawRect(0.0F, 0.0F, (float)this.width, (float)this.height, 0xB00B0B0E);
        int x = this.width / 2 - 130;
        int y = this.height / 2 - 34;
        SythixTheme.drawRect((float)x, (float)y, (float)(x + 260), (float)(y + 76), SythixTheme.withAlpha(SythixTheme.CHARCOAL, 0xF4));
        SythixTheme.drawOutline((float)x, (float)y, (float)(x + 260), (float)(y + 76), SythixTheme.BORDER);
        this.fontRenderer.drawString(this.title, (float)(x + 8), (float)(y + 7), 0xFFFFFFFF, true);
        this.fontRenderer.drawString(this.fontRenderer.trimStringToWidth(this.hint, 244), (float)(x + 8), (float)(y + 18), SythixTheme.TEXT_MUTED, false);
        SythixTheme.drawRect((float)(this.width / 2 - 123), (float)(this.height / 2 - 9), (float)(this.width / 2 + 123), (float)(this.height / 2 + 9), 0xFF0F0F13);
        SythixTheme.drawOutline((float)(this.width / 2 - 123), (float)(this.height / 2 - 9), (float)(this.width / 2 + 123), (float)(this.height / 2 + 9), SythixTheme.RED);
        this.field.drawTextBox();
        this.fontRenderer.drawString("Enter = OK    Esc = Cancel", (float)(x + 8), (float)(y + 62), SythixTheme.TEXT_DISABLED, false);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public void updateScreen()
    {
        this.field.updateCursorCounter();
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (keyCode == 1)
        {
            this.mc.displayGuiScreen(this.parent);
        }
        else if (keyCode == 28 || keyCode == 156)
        {
            this.mc.displayGuiScreen(this.parent);
            this.onDone.accept(this.field.getText());
        }
        else
        {
            this.field.textboxKeyTyped(typedChar, keyCode);
        }
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.field.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public boolean doesGuiPauseGame()
    {
        return false;
    }
}
