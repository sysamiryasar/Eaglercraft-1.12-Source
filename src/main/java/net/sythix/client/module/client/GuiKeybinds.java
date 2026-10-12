package net.sythix.client.module.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.gui.GuiScreen;
import net.sythix.client.ui.GuiListScreen;
import net.sythix.client.ui.SythixTheme;

/**
 * Lists every keybind; red rows share a key with something else.
 */
public class GuiKeybinds extends GuiListScreen
{
    private static final String[] FILTERS = new String[] {"All", "Conflicts", "Sythix", "Minecraft"};
    private final KeybindManager manager;
    private int filter;
    private KeybindManager.Entry capturing;
    private List<KeybindManager.Entry> entries;

    public GuiKeybinds(GuiScreen parent, KeybindManager manager)
    {
        super(parent, "Keybind Manager");
        this.manager = manager;
    }

    protected String header()
    {
        return this.capturing != null ? "Press a key for \"" + this.capturing.name + "\" (Esc = none)" : this.manager.countConflicts() + " conflicting keys. Double-click a row to rebind it.";
    }

    protected List<Row> rows()
    {
        this.entries = this.manager.entries();
        Map<Integer, Integer> usage = this.manager.keyUsage(this.entries);
        List<Row> rows = new ArrayList<Row>();

        for (KeybindManager.Entry e : this.entries)
        {
            boolean conflict = e.key() != 0 && usage.get(Integer.valueOf(e.key())).intValue() > 1;
            boolean sythix = e.setting != null;

            if (this.filter == 1 && !conflict || this.filter == 2 && !sythix || this.filter == 3 && sythix)
            {
                continue;
            }

            String key = e == this.capturing ? "> ? <" : (e.key() == 0 ? "None" : GameSettings.getKeyDisplayString(e.key()));
            rows.add(new Row(e.name, e.name, e.group + (conflict ? " - shares its key" : ""), key, conflict ? 0xFFFF4040 : (sythix ? SythixTheme.RED : SythixTheme.BLUE)));
        }

        return rows;
    }

    protected String[] buttons()
    {
        return new String[] {"Rebind", "Clear", "Reset", "Reset All", "Show: " + FILTERS[this.filter], "Done"};
    }

    protected boolean enabled(int index)
    {
        return index >= 3 || this.find() != null;
    }

    private KeybindManager.Entry find()
    {
        if (this.selected == null || this.entries == null)
        {
            return null;
        }

        for (KeybindManager.Entry e : this.entries)
        {
            if (e.name.equals(this.selected))
            {
                return e;
            }
        }

        return null;
    }

    protected void press(int index)
    {
        KeybindManager.Entry e = this.find();

        switch (index)
        {
            case 0:
                this.capturing = e;
                break;
            case 1:
                this.manager.setKey(e, 0);
                this.status = "Cleared " + e.name;
                break;
            case 2:
                this.manager.setKey(e, e.defaultKey());
                this.status = "Reset " + e.name;
                break;
            case 3:
                for (KeybindManager.Entry each : this.manager.entries())
                {
                    if (each.key() != each.defaultKey())
                    {
                        this.manager.setKey(each, each.defaultKey());
                    }
                }

                this.status = "All keys reset to their defaults.";
                break;
            case 4:
                this.filter = (this.filter + 1) % FILTERS.length;
                break;
            default:
                this.mc.displayGuiScreen(this.parent);
        }
    }

    protected void open(Object key)
    {
        this.capturing = this.find();
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (this.capturing != null)
        {
            this.manager.setKey(this.capturing, keyCode == 1 ? 0 : keyCode);
            this.status = this.capturing.name + " = " + (keyCode == 1 ? "None" : GameSettings.getKeyDisplayString(keyCode));
            this.capturing = null;
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        if (this.capturing != null && mouseButton > 1)
        {
            // Middle and side mouse buttons can be bound like in vanilla (-100 + button)
            this.manager.setKey(this.capturing, mouseButton - 100);
            this.capturing = null;
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }
}
