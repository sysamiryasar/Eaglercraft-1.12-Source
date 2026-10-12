package net.sythix.client.module.pvp;

import java.util.Collections;
import java.util.List;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.sythix.client.module.Category;
import net.sythix.client.module.hud.TextHudModule;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.GameOptionSetting;
import net.sythix.client.setting.NumberSetting;

/**
 * #16 Toggle sprint, toggle sneak, FOV and view-bobbing options. Toggle sprint/sneak only hold
 * the key for you; they do the same as holding it down.
 */
public class MovementControls extends TextHudModule
{
    private final BooleanSetting toggleSprint;
    private final BooleanSetting toggleSneak;
    private final BooleanSetting showStatus;
    private final BooleanSetting staticFov;
    private final NumberSetting bobbing;
    private boolean sprintToggled;
    private boolean sneakToggled;

    public MovementControls()
    {
        super("Movement Controls", "Toggle sprint and sneak, FOV and view-bobbing options.", Category.PVP, false, 0.005D, 0.96D);
        this.toggleSprint = this.add(new BooleanSetting("Toggle Sprint", "Tap the sprint key to switch sprinting on/off.", true));
        this.toggleSneak = this.add(new BooleanSetting("Toggle Sneak", "Tap the sneak key to switch sneaking on/off.", false));
        this.showStatus = this.add(new BooleanSetting("Show Status", "Show the sprint/sneak state on screen.", true));
        this.add(new GameOptionSetting("Field of view.", GameSettings.Options.FOV));
        this.staticFov = this.add(new BooleanSetting("Static FOV", "Stop the view zooming when you sprint or get speed effects.", false));
        this.add(new GameOptionSetting("Camera bobbing while walking.", GameSettings.Options.VIEW_BOBBING));
        this.bobbing = this.add(new NumberSetting("Bobbing Amount", "How strong view bobbing is when it is on.", 100.0D, 0.0D, 100.0D, 5.0D, "%"));
    }

    public boolean staticFov()
    {
        return this.staticFov.isOn();
    }

    public float bobbingAmount()
    {
        return this.bobbing.getFloat() / 100.0F;
    }

    public void onKey(int keyCode)
    {
        if (this.toggleSprint.isOn() && keyCode == this.mc.gameSettings.keyBindSprint.getKeyCode())
        {
            this.sprintToggled = !this.sprintToggled;
        }

        if (this.toggleSneak.isOn() && keyCode == this.mc.gameSettings.keyBindSneak.getKeyCode())
        {
            this.sneakToggled = !this.sneakToggled;
        }
    }

    public void onMouseClick(int button)
    {
        this.onKey(button - 100);
    }

    public void onTick()
    {
        boolean playing = this.mc.player != null && this.mc.currentScreen == null;
        this.mc.gameSettings.keyBindSprint.sythixForced = playing && this.toggleSprint.isOn() && this.sprintToggled;
        this.mc.gameSettings.keyBindSneak.sythixForced = playing && this.toggleSneak.isOn() && this.sneakToggled && !this.mc.player.capabilities.isFlying;
    }

    protected void onDisable()
    {
        this.mc.gameSettings.keyBindSprint.sythixForced = false;
        this.mc.gameSettings.keyBindSneak.sythixForced = false;
    }

    private String status()
    {
        KeyBinding sneak = this.mc.gameSettings.keyBindSneak;
        KeyBinding sprint = this.mc.gameSettings.keyBindSprint;

        if (this.mc.player != null && this.mc.player.capabilities.isFlying)
        {
            return "Flying";
        }

        if (sneak.isKeyDown())
        {
            return this.sneakToggled && this.toggleSneak.isOn() ? "Sneaking (Toggled)" : "Sneaking (Held)";
        }

        if (this.sprintToggled && this.toggleSprint.isOn())
        {
            return "Sprinting (Toggled)";
        }

        return sprint.isKeyDown() ? "Sprinting (Held)" : "";
    }

    public boolean shouldRender()
    {
        return this.showStatus.isOn() && !this.status().isEmpty();
    }

    protected List<String[]> getLines()
    {
        String s = this.status();
        return Collections.singletonList(new String[] {"", s.isEmpty() ? "Sprinting (Toggled)" : s});
    }
}
