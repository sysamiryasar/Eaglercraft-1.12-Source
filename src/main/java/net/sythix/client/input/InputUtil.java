package net.sythix.client.input;

import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Reads whether keys are physically held. Kept in one place so a browser build only needs to
 * change this file.
 */
public final class InputUtil
{
    private InputUtil()
    {
    }

    /** True while the key or mouse button bound to this binding is held, even when a menu is open. */
    public static boolean isDown(KeyBinding binding)
    {
        return isKeyDown(binding.getKeyCode());
    }

    /** Key codes below 0 are mouse buttons (-100 = left, -99 = right), as in vanilla key bindings. */
    public static boolean isKeyDown(int keyCode)
    {
        if (keyCode < 0)
        {
            return Mouse.isCreated() && Mouse.isButtonDown(keyCode + 100);
        }

        return keyCode > 0 && keyCode < 256 && Keyboard.isCreated() && Keyboard.isKeyDown(keyCode);
    }

    public static boolean isShiftDown()
    {
        return Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54);
    }
}
