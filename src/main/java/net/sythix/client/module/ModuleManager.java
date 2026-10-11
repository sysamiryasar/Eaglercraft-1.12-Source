package net.sythix.client.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.sythix.client.core.ModuleErrors;
import net.sythix.client.module.hud.HudModule;

/**
 * Holds every module, in the order of the Sythix module list, and dispatches events to them
 * safely: a module that throws is reported and switched off instead of crashing the game.
 */
public class ModuleManager
{
    private final List<Module> modules = new ArrayList<Module>();
    private final Map<Class<?>, Module> byClass = new HashMap<Class<?>, Module>();

    public ModuleManager()
    {
        ModuleRegistry.registerAll(this);
    }

    public void register(Module module)
    {
        this.modules.add(module);
        this.byClass.put(module.getClass(), module);
    }

    public List<Module> getModules()
    {
        return Collections.unmodifiableList(this.modules);
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type)
    {
        return (T)this.byClass.get(type);
    }

    /** The module if it exists and is enabled, otherwise null. */
    public <T extends Module> T getEnabled(Class<T> type)
    {
        T module = this.get(type);
        return module != null && module.isEnabled() ? module : null;
    }

    public List<HudModule> getHudModules()
    {
        List<HudModule> list = new ArrayList<HudModule>();

        for (Module module : this.modules)
        {
            if (module instanceof HudModule)
            {
                list.add((HudModule)module);
            }
        }

        // Stable sort: lower layers first, so higher layers are drawn on top
        list.sort(java.util.Comparator.comparingInt(HudModule::getLayer));
        return list;
    }

    public Module getByName(String name)
    {
        for (Module module : this.modules)
        {
            if (module.getName().equalsIgnoreCase(name))
            {
                return module;
            }
        }

        return null;
    }

    /** Runs an event on every enabled module, catching and reporting errors per module. */
    public void forEachEnabled(String event, Consumer<Module> action)
    {
        for (Module module : this.modules)
        {
            if (module.isEnabled())
            {
                try
                {
                    action.accept(module);
                }
                catch (Throwable t)
                {
                    ModuleErrors.report(module, event, t);
                }
            }
        }
    }

    /** Toggles every module whose toggle key matches. */
    public void handleToggleKeys(int keyCode)
    {
        if (keyCode == 0)
        {
            return;
        }

        for (Module module : this.modules)
        {
            if (!module.isCore() && module.getToggleKey().getKey() == keyCode)
            {
                module.toggle();
            }
        }
    }

    public void resetAll()
    {
        for (Module module : this.modules)
        {
            module.resetToDefaults();
        }
    }
}
