package net.sythix.client.module.advanced;

import java.io.File;
import java.nio.charset.StandardCharsets;
import net.sythix.client.SythixClient;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.InfoSetting;
import org.apache.commons.io.FileUtils;

/**
 * #59 Turns every optional module off to find problems, and puts everything back afterwards.
 * Your settings are kept in a snapshot (sythix/safemode-snapshot.json) while safe mode is on.
 */
public class SafeMode extends Module
{
    private final BooleanSetting startInSafeMode;
    private boolean checkedStart;

    public SafeMode()
    {
        super("Safe Mode", "Switch off every optional module to troubleshoot, then restore with one click.", Category.ADVANCED, true);
        this.setCore();
        this.add(new InfoSetting("Status", "Whether safe mode is on.", () -> this.isActive() ? "ON - optional modules are off" : "Off"));
        this.add(new ButtonSetting("Enter Safe Mode", "Save a snapshot and switch every optional module off.", "Enter", this::enter));
        this.add(new ButtonSetting("Exit And Restore", "Restore everything from the snapshot.", "Restore", this::exit));
        this.startInSafeMode = this.add(new BooleanSetting("Start In Safe Mode", "Enter safe mode automatically the next time the game starts (then this switches itself off).", false));
    }

    private File snapshot()
    {
        return new File(new File(this.mc.mcDataDir, "sythix"), "safemode-snapshot.json");
    }

    public boolean isActive()
    {
        return this.snapshot().isFile();
    }

    public void onTick()
    {
        if (!this.checkedStart)
        {
            this.checkedStart = true;

            if (this.startInSafeMode.isOn())
            {
                this.startInSafeMode.set(Boolean.FALSE);
                this.enter();
            }
            else if (this.isActive())
            {
                Notifications.post("Safe mode is on", "Optional modules are off. Restore them in Safe Mode.", Notifications.Kind.WARNING);
            }
        }
    }

    private void enter()
    {
        if (this.isActive())
        {
            Notifications.post("Safe mode", "Already on.", Notifications.Kind.INFO);
            return;
        }

        try
        {
            FileUtils.writeStringToFile(this.snapshot(), SythixClient.get().getConfigManager().toJsonString(), StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            Notifications.post("Safe mode", "Could not save a snapshot, nothing changed: " + e.getMessage(), Notifications.Kind.ERROR);
            return;
        }

        int off = 0;

        for (Module module : SythixClient.get().getModuleManager().getModules())
        {
            if (!module.isCore() && module.isEnabled())
            {
                module.setEnabled(false);
                ++off;
            }
        }

        SythixClient.get().save();
        Notifications.post("Safe mode on", off + " modules switched off. Use Exit And Restore to undo.", Notifications.Kind.WARNING);
    }

    private void exit()
    {
        if (!this.isActive())
        {
            Notifications.post("Safe mode", "Safe mode is not on.", Notifications.Kind.INFO);
            return;
        }

        try
        {
            String json = FileUtils.readFileToString(this.snapshot(), StandardCharsets.UTF_8);
            SythixClient.get().getModuleManager().resetAll();
            SythixClient.get().getConfigManager().apply(json);
            this.startInSafeMode.set(Boolean.FALSE);
            SythixClient.get().save();
            this.snapshot().delete();
            Notifications.post("Safe mode off", "All your modules and settings are back.", Notifications.Kind.SUCCESS);
        }
        catch (Exception e)
        {
            Notifications.post("Restore failed", "The snapshot is kept in sythix/safemode-snapshot.json: " + e.getMessage(), Notifications.Kind.ERROR);
        }
    }
}
