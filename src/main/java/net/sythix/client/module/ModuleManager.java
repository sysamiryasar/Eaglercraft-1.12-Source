package net.sythix.client.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.sythix.client.module.hud.CpsModule;
import net.sythix.client.module.hud.FpsModule;
import net.sythix.client.module.hud.HudModule;
import net.sythix.client.module.hud.KeystrokesModule;
import net.sythix.client.module.hud.PingModule;

public class ModuleManager
{
    private final List<Module> modules = new ArrayList<Module>();

    public ModuleManager()
    {
        this.register(new FpsModule());
        this.register(new CpsModule());
        this.register(new PingModule());
        this.register(new KeystrokesModule());
    }

    private void register(Module module)
    {
        this.modules.add(module);
    }

    public List<Module> getModules()
    {
        return Collections.unmodifiableList(this.modules);
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

    /** Toggles every module whose toggle key matches. */
    public void onKeyPressed(int keyCode)
    {
        for (Module module : this.modules)
        {
            if (module.getToggleKey().getKey() == keyCode)
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
