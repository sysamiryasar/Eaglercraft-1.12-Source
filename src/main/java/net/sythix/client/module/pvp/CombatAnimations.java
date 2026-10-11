package net.sythix.client.module.pvp;

import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.NumberSetting;

/**
 * #11 Sword, item-use and swing animations, including a classic (1.7 style) sword-block pose.
 * Purely visual on your own screen.
 */
public class CombatAnimations extends Module
{
    private final NumberSetting swingSpeed;
    private final BooleanSetting classicBlock;
    private final BooleanSetting swingWhileUsing;
    private final BooleanSetting noEquipBounce;

    public CombatAnimations()
    {
        super("Combat Animations", "Swing speed, classic sword-block pose and item-use animations (visual only).", Category.PVP, false);
        this.swingSpeed = this.add(new NumberSetting("Swing Speed", "Speed of your arm swing animation. 1.0 = vanilla.", 1.0D, 0.5D, 2.0D, 0.05D, "x"));
        this.classicBlock = this.add(new BooleanSetting("Classic Sword Block", "Hold right click with a sword to show the 1.7 blocking pose.", true));
        this.swingWhileUsing = this.add(new BooleanSetting("Swing While Using", "Show the swing animation while eating, drinking or blocking.", true));
        this.noEquipBounce = this.add(new BooleanSetting("No Re-equip Bounce", "Stop the hand dipping down when an item updates (for example durability).", true));
    }

    /** Multiplier applied to your own arm swing animation length. */
    public float getSwingSpeed()
    {
        return this.swingSpeed.getFloat();
    }

    /** True while the classic sword-block pose should be drawn for the main hand. */
    public boolean isClassicBlocking(ItemStack mainHand)
    {
        return this.classicBlock.isOn() && mainHand.getItem() instanceof ItemSword && this.mc.gameSettings.keyBindUseItem.isKeyDown() && this.mc.currentScreen == null;
    }

    public boolean swingWhileUsing()
    {
        return this.swingWhileUsing.isOn();
    }

    public boolean noEquipBounce()
    {
        return this.noEquipBounce.isOn();
    }
}
