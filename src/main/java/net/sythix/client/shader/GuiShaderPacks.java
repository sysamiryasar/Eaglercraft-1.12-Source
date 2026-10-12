package net.sythix.client.shader;

import java.io.File;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.sythix.client.core.Notifications;
import net.sythix.client.ui.GuiTextPrompt;
import net.sythix.client.ui.SythixTheme;
import net.sythix.client.ui.font.SmoothFont;
import org.lwjgl.input.Mouse;

/**
 * Sythix's shader screen: pick a shader pack (Sythix's own packs, OptiFine's internal one or any
 * pack you add), add new packs, open the folder and change the pack's options.
 */
public class GuiShaderPacks extends GuiScreen
{
    private final GuiScreen parent;
    private List<String> packs;
    private float scroll;
    private int listTop;
    private int listBottom;
    private int panelLeft;
    private int panelRight;
    private String status = "";
    private long statusTime;

    public GuiShaderPacks(GuiScreen parent)
    {
        this.parent = parent;
    }

    public void initGui()
    {
        ShaderPackManager.installBundled();
        this.packs = ShaderPackManager.packs();
        int pw = Math.min(320, this.width - 40);
        this.panelLeft = (this.width - pw) / 2;
        this.panelRight = this.panelLeft + pw;
        this.listTop = 50;
        this.listBottom = this.height - 54;
        int bw = (pw - 12) / 4;
        int y = this.height - 44;
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(1, this.panelLeft, y, bw, 20, "Add Pack"));
        this.buttonList.add(new GuiButton(2, this.panelLeft + (bw + 4), y, bw, 20, "Open Folder"));
        this.buttonList.add(new GuiButton(3, this.panelLeft + (bw + 4) * 2, y, bw, 20, "Options"));
        this.buttonList.add(new GuiButton(0, this.panelLeft + (bw + 4) * 3, y, bw, 20, "Done"));
        this.buttonList.get(2).enabled = ShaderPackManager.enabled();
    }

    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button.id == 0)
        {
            this.mc.displayGuiScreen(this.parent);
        }
        else if (button.id == 1)
        {
            this.mc.displayGuiScreen(new GuiTextPrompt(this, "Add a shader pack", "Path to a shader pack .zip or folder (or copy it into the shaderpacks folder)", "", 512, path -> {
                if (path == null || path.trim().isEmpty())
                {
                    return;
                }

                try
                {
                    String name = ShaderPackManager.importPack(new File(path.trim().replace("\"", "")));
                    Notifications.post("Shader pack added", ShaderPackManager.displayName(name), Notifications.Kind.SUCCESS);
                }
                catch (Exception e)
                {
                    Notifications.post("Could not add the pack", String.valueOf(e.getMessage()), Notifications.Kind.ERROR);
                }
            }));
        }
        else if (button.id == 2)
        {
            OpenGlHelper.openFile(ShaderPackManager.folder());
        }
        else if (button.id == 3)
        {
            this.mc.displayGuiScreen(new shadersmod.client.GuiShaderOptions(this, this.mc.gameSettings));
        }
    }

    /** Shaders do not work together with these options: turn them off and say so. */
    private void makeCompatible()
    {
        StringBuilder changed = new StringBuilder();

        if (this.mc.gameSettings.ofAaLevel != 0)
        {
            this.mc.gameSettings.ofAaLevel = 0;
            changed.append("anti-aliasing, ");
        }

        if (this.mc.gameSettings.ofAfLevel != 1)
        {
            this.mc.gameSettings.ofAfLevel = 1;
            changed.append("anisotropic filtering, ");
        }

        if (this.mc.gameSettings.ofFastRender)
        {
            this.mc.gameSettings.ofFastRender = false;
            changed.append("Fast Render, ");
        }

        if (this.mc.gameSettings.anaglyph)
        {
            this.mc.gameSettings.anaglyph = false;
            changed.append("3D anaglyph, ");
        }

        if (changed.length() > 0)
        {
            this.mc.gameSettings.saveOptions();
            Notifications.post("Changed for shaders", "Turned off " + changed.substring(0, changed.length() - 2), Notifications.Kind.INFO);
        }
    }

    private void select(String pack)
    {
        if (pack.equals(ShaderPackManager.current()))
        {
            return;
        }

        if (!ShaderPackManager.OFF.equals(pack))
        {
            this.makeCompatible();
        }

        ShaderPackManager.select(pack);
        this.status = ShaderPackManager.OFF.equals(pack) ? "Shaders turned off" : "Loaded " + ShaderPackManager.displayName(pack);
        this.statusTime = System.currentTimeMillis();
        this.initGui();
    }

    private static final int ROW = 30;

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0 && mouseX >= this.panelLeft && mouseX <= this.panelRight && mouseY >= this.listTop && mouseY <= this.listBottom)
        {
            int index = (int)((mouseY - this.listTop + this.scroll) / ROW);

            if (index >= 0 && index < this.packs.size())
            {
                this.select(this.packs.get(index));
            }
        }
    }

    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();

        if (wheel != 0)
        {
            this.scroll -= Math.signum(wheel) * ROW;
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        float max = Math.max(0, this.packs.size() * ROW - (this.listBottom - this.listTop));
        this.scroll = Math.max(0.0F, Math.min(max, this.scroll));
        SmoothFont bold = SmoothFont.BOLD;
        SmoothFont font = SmoothFont.REGULAR;
        bold.draw("Shaders", this.panelLeft, 14.0F, 1.6F, 0xFFFFFFFF);
        String current = ShaderPackManager.enabled() ? "Using " + ShaderPackManager.displayName(ShaderPackManager.current()) : "Shaders are off";
        font.draw(current, this.panelLeft, 32.0F, 0.85F, SythixTheme.TEXT_MUTED);
        SythixTheme.drawAccentGradient(this.panelLeft, 42.0F, this.panelLeft + 40, 43.0F, 1.0F);

        SythixTheme.beginClip(this.panelLeft - 2, this.listTop, this.panelRight + 2, this.listBottom);

        for (int i = 0; i < this.packs.size(); ++i)
        {
            String pack = this.packs.get(i);
            float top = this.listTop + i * ROW - this.scroll;

            if (top + ROW < this.listTop || top > this.listBottom)
            {
                continue;
            }

            boolean selected = pack.equals(ShaderPackManager.current());
            boolean hover = mouseX >= this.panelLeft && mouseX <= this.panelRight && mouseY >= top && mouseY < top + ROW - 4 && mouseY >= this.listTop && mouseY <= this.listBottom;
            SythixTheme.drawGlass(this.panelLeft, top, this.panelRight, top + ROW - 4, 5.0F, hover ? 1.35F : 1.0F);

            if (selected)
            {
                SythixTheme.drawRoundedOutline(this.panelLeft, top, this.panelRight, top + ROW - 4, 5.0F, 1.0F, SythixTheme.withAlpha(SythixTheme.lerpColor(SythixTheme.RED, SythixTheme.BLUE, 0.3F), 0xE0));
                SythixTheme.drawRounded(this.panelRight - 18, top + 7, this.panelRight - 6, top + 19, 6.0F, SythixTheme.lerpColor(SythixTheme.RED, SythixTheme.BLUE, 0.3F));
                bold.drawCentered("✓", this.panelRight - 12.0F, top + 9.5F, 0.75F, 0xFFFFFFFF);
            }

            bold.draw(ShaderPackManager.displayName(pack), this.panelLeft + 9.0F, top + 4.0F, 0.95F, 0xFFFFFFFF);
            font.draw(ShaderPackManager.description(pack), this.panelLeft + 9.0F, top + 14.5F, 0.8F, SythixTheme.TEXT_MUTED);
        }

        SythixTheme.endClip();

        if (!this.status.isEmpty() && System.currentTimeMillis() - this.statusTime < 4000L)
        {
            font.draw(this.status, this.panelLeft, this.height - 15.0F, 0.8F, SythixTheme.LIGHT_BLUE);
        }
        else
        {
            font.draw("Tip: copy shader packs (.zip) into the shaderpacks folder, then pick them here.", this.panelLeft, this.height - 15.0F, 0.75F, SythixTheme.withAlpha(SythixTheme.TEXT_MUTED, 0xA0));
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
