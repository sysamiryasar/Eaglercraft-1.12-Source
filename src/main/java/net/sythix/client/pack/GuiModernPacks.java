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
        return this.working ? "Converting..." : "1.13-1.21 packs, or your own Minecraft 1.21 install. Convert one, then enable \"(1.12)\" in Pack Manager.";
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
                if ((f.isDirectory() || f.getName().endsWith(".zip") || f.getName().endsWith(".jar")) && ModernPackConverter.isModern(f))
                {
                    rows.add(this.row(f, "pack format " + ModernPackConverter.packFormat(f)));
                }
            }
        }

        // Minecraft versions installed with the official launcher (your own copy)
        for (File jar : installedVersions())
        {
            rows.add(this.row(jar, "Minecraft installation: " + jar.getParent()));
        }

        return rows;
    }

    private Row row(File f, String subtitle)
    {
        String out = f.getName().endsWith(".jar") ? "Minecraft " + f.getName().replaceAll("\\.jar$", "") + " textures (1.12).zip" : f.getName().replaceAll("\\.zip$", "") + " (1.12).zip";
        boolean done = new File(this.dir(), out).isFile();
        return new Row(f, f.getName(), subtitle, done ? "converted" : "", done ? 0xFF40D060 : SythixTheme.RED);
    }

    /** Version jars 1.13 and newer in the usual Minecraft folders. */
    static List<File> installedVersions()
    {
        List<File> jars = new ArrayList<File>();
        String home = System.getProperty("user.home", "");
        String appData = System.getenv("APPDATA");
        File[] roots = new File[] {appData == null ? null : new File(appData, ".minecraft"), new File(home, "Library/Application Support/minecraft"), new File(home, ".minecraft")};

        for (File root : roots)
        {
            File[] versions = root == null ? null : new File(root, "versions").listFiles();

            if (versions == null)
            {
                continue;
            }

            for (File v : versions)
            {
                File jar = new File(v, v.getName() + ".jar");
                String[] parts = v.getName().split("[.\\-]");

                try
                {
                    if (jar.isFile() && parts.length >= 2 && Integer.parseInt(parts[0]) == 1 && Integer.parseInt(parts[1]) >= 13)
                    {
                        jars.add(jar);
                    }
                }
                catch (NumberFormatException ignored)
                {
                    // Snapshots and modded versions are skipped
                }
            }
        }

        return jars;
    }

    protected String emptyText()
    {
        return "Nothing found. Put a 1.13+ pack (.zip) or a Minecraft 1.21 .jar in the resourcepacks folder.";
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
            File pack = (File)this.selected;
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
