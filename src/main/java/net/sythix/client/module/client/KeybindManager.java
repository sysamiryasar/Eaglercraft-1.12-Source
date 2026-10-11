package net.sythix.client.module.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.sythix.client.SythixClient;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.KeybindSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.Setting;

/**
 * #51 One place for every key: Minecraft's own keybinds and every Sythix module key, with
 * conflict warnings and hold-or-toggle behaviour for module toggle keys.
 */
public class KeybindManager extends Module
{
    /** One key: either a vanilla KeyBinding or a Sythix KeybindSetting. */
    public static final class Entry
    {
        public final String name;
        public final String group;
        public final KeyBinding vanilla;
        public final KeybindSetting setting;

        Entry(String name, String group, KeyBinding vanilla, KeybindSetting setting)
        {
            this.name = name;
            this.group = group;
            this.vanilla = vanilla;
            this.setting = setting;
        }

        public int key()
        {
            return this.vanilla != null ? this.vanilla.getKeyCode() : this.setting.getKey();
        }

        public int defaultKey()
        {
            return this.vanilla != null ? this.vanilla.getKeyCodeDefault() : this.setting.getDefault().intValue();
        }
    }

    private final ModeSetting toggleMode;
    private final BooleanSetting warnConflicts;
    private boolean warned;

    public KeybindManager()
    {
        super("Keybind Manager", "Every keybind in one list, with conflict warnings and hold/toggle keys.", Category.CLIENT, true);
        this.setCore();
        this.add(new ButtonSetting("Keybinds", "View, change and reset every key.", "Open", () -> this.mc.displayGuiScreen(new GuiKeybinds(this.mc.currentScreen, this))));
        this.toggleMode = this.add(new ModeSetting("Module Key Mode", "Toggle: press to switch a module on/off. Hold: the module is on only while the key is held.", "Toggle", "Toggle", "Hold"));
        this.warnConflicts = this.add(new BooleanSetting("Warn About Conflicts", "Show a notification when you join a world and two keys clash.", true));
    }

    public GuiScreen createScreen(GuiScreen parent)
    {
        return new GuiKeybinds(parent, this);
    }

    public boolean holdMode()
    {
        return "Hold".equals(this.toggleMode.get());
    }

    public void onWorldJoin()
    {
        if (this.warnConflicts.isOn() && !this.warned)
        {
            this.warned = true;
            int conflicts = this.countConflicts(true);

            if (conflicts > 0)
            {
                Notifications.post("Key conflicts", conflicts + " Sythix keys share a key with something else. Open Keybind Manager to fix.", Notifications.Kind.WARNING);
            }
        }
    }

    /** Every key in the game and the client. */
    public List<Entry> entries()
    {
        List<Entry> list = new ArrayList<Entry>();

        for (KeyBinding kb : this.mc.gameSettings.keyBindings)
        {
            list.add(new Entry(net.minecraft.client.resources.I18n.format(kb.getKeyDescription()), net.minecraft.client.resources.I18n.format(kb.getKeyCategory()), kb, null));
        }

        for (Module module : SythixClient.get().getModuleManager().getModules())
        {
            for (Setting<?> s : module.getSettings())
            {
                if (s instanceof KeybindSetting)
                {
                    list.add(new Entry(module.getName() + ": " + s.getName(), "Sythix", null, (KeybindSetting)s));
                }
            }
        }

        return list;
    }

    /** How many other entries use the same (non-empty) key. */
    public Map<Integer, Integer> keyUsage(List<Entry> entries)
    {
        Map<Integer, Integer> usage = new HashMap<Integer, Integer>();

        for (Entry e : entries)
        {
            if (e.key() != 0)
            {
                usage.merge(Integer.valueOf(e.key()), Integer.valueOf(1), Integer::sum);
            }
        }

        return usage;
    }

    public int countConflicts()
    {
        return this.countConflicts(false);
    }

    /** Conflicting entries; sythixOnly counts only clashes that involve a Sythix key. */
    public int countConflicts(boolean sythixOnly)
    {
        List<Entry> entries = this.entries();
        Map<Integer, Integer> usage = this.keyUsage(entries);
        int count = 0;

        Map<Integer, Integer> sythixUsage = new HashMap<Integer, Integer>();

        for (Entry e : entries)
        {
            if (e.setting != null && e.key() != 0)
            {
                sythixUsage.merge(Integer.valueOf(e.key()), Integer.valueOf(1), Integer::sum);
            }
        }

        for (Entry e : entries)
        {
            if (e.key() != 0 && usage.get(Integer.valueOf(e.key())).intValue() > 1 && (!sythixOnly || sythixUsage.containsKey(Integer.valueOf(e.key()))))
            {
                ++count;
            }
        }

        return count;
    }

    public void setKey(Entry e, int key)
    {
        if (e.vanilla != null)
        {
            this.mc.gameSettings.setOptionKeyBinding(e.vanilla, key);
            KeyBinding.resetKeyBindingArrayAndHash();
        }
        else
        {
            e.setting.set(Integer.valueOf(key));
            SythixClient.get().save();
        }
    }
}
