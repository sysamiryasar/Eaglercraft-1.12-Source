package net.sythix.client.plugin;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import net.sythix.client.module.ModuleManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Loads plugin jars from sythix/plugins with the Java ServiceLoader.
 */
public final class PluginLoader
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final List<String> LOADED = new ArrayList<String>();
    private static final List<String> FAILED = new ArrayList<String>();

    private PluginLoader()
    {
    }

    public static File folder(File gameDir)
    {
        return new File(new File(gameDir, "sythix"), "plugins");
    }

    /** Returns how many plugins were loaded. */
    public static int load(File gameDir, ModuleManager modules)
    {
        File dir = folder(gameDir);
        dir.mkdirs();
        File[] jars = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".jar"));

        if (jars == null || jars.length == 0)
        {
            return 0;
        }

        List<URL> urls = new ArrayList<URL>();

        for (File jar : jars)
        {
            try
            {
                urls.add(jar.toURI().toURL());
            }
            catch (Exception e)
            {
                FAILED.add(jar.getName() + ": " + e);
            }
        }

        URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[0]), PluginLoader.class.getClassLoader());
        Iterator<SythixPlugin> it = ServiceLoader.load(SythixPlugin.class, loader).iterator();

        while (true)
        {
            try
            {
                if (!it.hasNext())
                {
                    break;
                }

                SythixPlugin plugin = it.next();
                plugin.onLoad(modules);
                LOADED.add(plugin.getName() + " " + plugin.getVersion());
                LOGGER.info("[Sythix] Loaded plugin {} {}", plugin.getName(), plugin.getVersion());
            }
            catch (Throwable t)
            {
                // A broken plugin must never stop the game from starting
                FAILED.add(String.valueOf(t));
                LOGGER.error("[Sythix] Plugin failed to load", t);

                if (FAILED.size() > 50)
                {
                    break;
                }
            }
        }

        return LOADED.size();
    }

    public static List<String> loaded()
    {
        return Collections.unmodifiableList(LOADED);
    }

    public static List<String> failed()
    {
        return Collections.unmodifiableList(FAILED);
    }
}
