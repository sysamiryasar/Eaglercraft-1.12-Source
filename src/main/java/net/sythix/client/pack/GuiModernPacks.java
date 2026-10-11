package net.sythix.client.pack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.sythix.client.ui.GuiListScreen;
import net.sythix.client.ui.SythixTheme;

/**
 * Lists resource packs made for Minecraft 1.13-1.21 and converts them for 1.12.
 */
public class GuiModernPacks extends GuiListScreen
{
    private boolean working;
    private List<Row> cached;

    public GuiModernPacks(GuiScreen parent)
    {
        super(parent, "Modern Pack Converter");
    }

    private File dir()
    {
        return this.mc.getResourcePackRepository().getDirResourcepacks();
    }

    protected String header()
    {
        return this.working ? "Converting..." : "Packs for 1.13-1.21 in your resourcepacks folder. Convert one, then enable \"(1.12)\" in Pack Manager.";
    }

    protected List<Row> rows()
    {
        // Reading every zip is slow, so the list is only rebuilt after a conversion
        if (this.cached == null)
        {
            this.cached = this.scan();
        }

        return this.cached;
    }

    private List<Row> scan()
    {
        List<Row> rows = new ArrayList<Row>();
        File[] files = this.dir().listFiles();

        if (files != null)
        {
            java.util.Arrays.sort(files);

            for (File f : files)
            {
                if ((f.isDirectory() || f.getName().endsWith(".zip")) && ModernPackConverter.isModern(f))
                {
                    boolean done = new File(this.dir(), f.getName().replaceAll("\\.zip$", "") + " (1.12).zip").isFile();
                    rows.add(new Row(f.getName(), f.getName(), "pack format " + ModernPackConverter.packFormat(f), done ? "converted" : "", done ? 0xFF40D060 : SythixTheme.RED));
                }
            }
        }

        return rows;
    }

    protected String emptyText()
    {
        return "No modern packs found. Put a 1.13+ pack (.zip) in the resourcepacks folder.";
    }

    protected String[] buttons()
    {
        return new String[] {"Convert", "Open Folder", "Done"};
    }

    protected boolean enabled(int index)
    {
        return index != 0 || this.selected != null && !this.working;
    }

    protected void open(Object key)
    {
        this.press(0);
    }

    protected void press(int index)
    {
        if (index == 0)
        {
            File pack = new File(this.dir(), (String)this.selected);
            this.working = true;
            // Big packs take a few seconds; convert off the game thread
            Thread thread = new Thread(() -> {
                String message;

                try
                {
                    ModernPackConverter.Result r = ModernPackConverter.convert(pack, this.dir());
                    message = String.join(" ", r.notes);
                }
                catch (Exception e)
                {
                    message = "Conversion failed: " + e;
                }

                String text = message;
                this.mc.addScheduledTask(() -> {
                    this.working = false;
                    this.status = text;
                    this.cached = null;
                    this.mc.getResourcePackRepository().updateRepositoryEntriesAll();
                });
            }, "Sythix pack converter");
            thread.setDaemon(true);
            thread.start();
        }
        else if (index == 1)
        {
            net.minecraft.client.renderer.OpenGlHelper.openFile(this.dir());
        }
        else
        {
            this.mc.displayGuiScreen(this.parent);
        }
    }
}
