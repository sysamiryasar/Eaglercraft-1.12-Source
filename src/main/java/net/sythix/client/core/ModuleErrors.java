package net.sythix.client.core;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import net.sythix.client.module.Module;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Records module errors. A module that throws is switched off so the game keeps running.
 */
public final class ModuleErrors
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final List<String> REPORTS = new ArrayList<String>();

    /** When false, failing modules stay on (only for debugging). */
    public static boolean autoDisable = true;

    private ModuleErrors()
    {
    }

    public static synchronized void report(Module module, String where, Throwable t)
    {
        String name = module == null ? "Sythix" : module.getName();
        String line = new SimpleDateFormat("HH:mm:ss").format(new Date()) + " " + name + " (" + where + "): " + t;
        LOGGER.error("[Sythix] Module error in {} during {}", name, where, t);

        if (REPORTS.size() >= 200)
        {
            REPORTS.remove(0);
        }

        REPORTS.add(line);

        if (module != null && autoDisable && !module.isCore() && module.isEnabled())
        {
            module.setEnabled(false);
            Notifications.post("Module disabled", name + " hit an error and was switched off.", Notifications.Kind.ERROR);
        }
    }

    public static synchronized List<String> getReports()
    {
        return Collections.unmodifiableList(new ArrayList<String>(REPORTS));
    }

    public static synchronized void clear()
    {
        REPORTS.clear();
    }
}
