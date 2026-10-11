package net.sythix.client.module.advanced;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.sythix.client.SythixClient;
import net.sythix.client.core.ModuleErrors;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.InfoSetting;
import net.sythix.client.ui.GuiTextPage;
import org.apache.commons.io.FileUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/**
 * #57 System information and compatibility checks, exportable as a text report for support.
 * The report never contains your account, session or server passwords.
 */
public class Diagnostics extends Module
{
    public Diagnostics()
    {
        super("Diagnostics", "System info, compatibility checks and a shareable report.", Category.ADVANCED, true);
        this.setCore();
        this.add(new ButtonSetting("Report", "Show the full diagnostics report.", "Open", () -> this.mc.displayGuiScreen(this.createScreen(this.mc.currentScreen))));
        this.add(new InfoSetting("Checks", "Summary of the compatibility checks.", () -> {
            int[] r = this.checkCounts();
            return r[0] + " OK, " + r[1] + " warnings";
        }));
        this.add(new ButtonSetting("Export", "Save the report to sythix/diagnostics and copy it to the clipboard.", "Export", this::export));
    }

    public GuiScreen createScreen(GuiScreen parent)
    {
        return new GuiTextPage(parent, "Diagnostics", this::report);
    }

    private static long mb(long bytes)
    {
        return bytes / 1024L / 1024L;
    }

    private static String gl(int name)
    {
        try
        {
            String s = GlStateManager.glGetString(name);
            return s == null ? "unknown" : s;
        }
        catch (Throwable t)
        {
            return "unknown";
        }
    }

    private static int glMajor()
    {
        String v = gl(GL11.GL_VERSION);

        try
        {
            return Integer.parseInt(v.substring(0, v.indexOf('.')));
        }
        catch (RuntimeException e)
        {
            return 0;
        }
    }

    /** Each check: {name, ok?, advice}. */
    private Object[][] checks()
    {
        Runtime rt = Runtime.getRuntime();
        long free = new File(this.mc.mcDataDir.getAbsolutePath()).getUsableSpace();
        int errors = ModuleErrors.getReports().size();
        return new Object[][] {
            {"Memory (max " + mb(rt.maxMemory()) + " MB)", Boolean.valueOf(mb(rt.maxMemory()) >= 1000L), "Give the game at least 1 GB (-Xmx1G) for texture packs."},
            {"OpenGL " + glMajor() + ".x", Boolean.valueOf(glMajor() >= 2), "Update your graphics driver; OpenGL 2.0+ is recommended."},
            {"Framebuffers / shaders", Boolean.valueOf(OpenGlHelper.shadersSupported), "Your graphics card does not support shader effects."},
            {"VBOs", Boolean.valueOf(OpenGlHelper.vboSupported), "Vertex buffers are not available; performance may be lower."},
            {"Free disk space (" + mb(free) + " MB)", Boolean.valueOf(mb(free) > 200L), "Free up disk space; settings and screenshots may fail to save."},
            {"Module errors (" + errors + ")", Boolean.valueOf(errors == 0), "See Error Handler for details."},
            {"Render distance (" + this.mc.gameSettings.renderDistanceChunks + ")", Boolean.valueOf(this.mc.gameSettings.renderDistanceChunks <= 16 || mb(rt.maxMemory()) >= 2000L), "High render distance with little memory can stutter."}
        };
    }

    private int[] checkCounts()
    {
        int ok = 0;
        int warn = 0;

        for (Object[] c : this.checks())
        {
            if (((Boolean)c[1]).booleanValue())
            {
                ++ok;
            }
            else
            {
                ++warn;
            }
        }

        return new int[] {ok, warn};
    }

    public String report()
    {
        Runtime rt = Runtime.getRuntime();
        StringBuilder b = new StringBuilder();
        b.append("# Client\n");
        b.append(SythixClient.getBrandingLine()).append('\n');
        b.append("OptiFine: ").append(net.minecraft.src.Config.getVersion()).append('\n');
        b.append("Modules on: ").append(this.countEnabled()).append(" of ").append(SythixClient.get().getModuleManager().getModules().size()).append('\n');
        b.append("Resource packs active: ").append(this.mc.getResourcePackRepository().getRepositoryEntries().size()).append('\n');
        b.append('\n').append("# System\n");
        b.append("Java: ").append(System.getProperty("java.version")).append(" (").append(System.getProperty("java.vendor")).append(")\n");
        b.append("OS: ").append(System.getProperty("os.name")).append(' ').append(System.getProperty("os.version")).append(" (").append(System.getProperty("os.arch")).append(")\n");
        b.append("CPU cores: ").append(rt.availableProcessors()).append('\n');
        b.append("Memory: ").append(mb(rt.totalMemory() - rt.freeMemory())).append(" / ").append(mb(rt.maxMemory())).append(" MB\n");
        b.append('\n').append("# Graphics\n");
        b.append("GPU: ").append(gl(GL11.GL_RENDERER)).append('\n');
        b.append("Vendor: ").append(gl(GL11.GL_VENDOR)).append('\n');
        b.append("OpenGL: ").append(gl(GL11.GL_VERSION)).append('\n');
        b.append("Window: ").append(Display.getWidth()).append('x').append(Display.getHeight()).append(this.mc.gameSettings.fullScreen ? " fullscreen" : "").append('\n');
        b.append("FPS: ").append(Minecraft.getDebugFPS()).append(" (limit ").append(this.mc.gameSettings.limitFramerate).append(", vsync ").append(this.mc.gameSettings.enableVsync ? "on" : "off").append(")\n");
        b.append("Graphics: ").append(this.mc.gameSettings.fancyGraphics ? "Fancy" : "Fast").append(", render distance ").append(this.mc.gameSettings.renderDistanceChunks).append(", shaders ").append(net.minecraft.src.Config.isShaders() ? "on" : "off").append('\n');
        b.append('\n').append("# Checks\n");

        for (Object[] c : this.checks())
        {
            boolean ok = ((Boolean)c[1]).booleanValue();
            b.append(ok ? "§aOK§r  " : "§6WARN§r  ").append(c[0]);

            if (!ok)
            {
                b.append(" - ").append(c[2]);
            }

            b.append('\n');
        }

        b.append('\n').append("# Recent module errors\n");
        java.util.List<String> errors = ModuleErrors.getReports();

        if (errors.isEmpty())
        {
            b.append("None\n");
        }

        for (int i = Math.max(0, errors.size() - 10); i < errors.size(); ++i)
        {
            b.append(errors.get(i)).append('\n');
        }

        return b.toString();
    }

    private int countEnabled()
    {
        int n = 0;

        for (Module m : SythixClient.get().getModuleManager().getModules())
        {
            if (m.isEnabled())
            {
                ++n;
            }
        }

        return n;
    }

    private void export()
    {
        try
        {
            String text = this.report().replaceAll("§.", "");
            File file = new File(new File(new File(this.mc.mcDataDir, "sythix"), "diagnostics"), "diagnostics-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".txt");
            FileUtils.writeStringToFile(file, text, StandardCharsets.UTF_8);
            GuiScreen.setClipboardString(text);
            Notifications.post("Diagnostics exported", "Copied to clipboard and saved as " + file.getName(), Notifications.Kind.SUCCESS);
        }
        catch (Exception e)
        {
            Notifications.post("Export failed", String.valueOf(e.getMessage()), Notifications.Kind.ERROR);
        }
    }
}
