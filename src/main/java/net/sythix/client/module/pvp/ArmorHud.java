package net.sythix.client.module.pvp;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.hud.StyledHudModule;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ModeSetting;
import net.sythix.client.setting.NumberSetting;

/**
 * #13 Armor icons, durability, low-durability warnings and layout.
 */
public class ArmorHud extends StyledHudModule
{
    private final ModeSetting layout;
    private final ModeSetting durability;
    private final BooleanSetting showMainHand;
    private final BooleanSetting showOffHand;
    private final BooleanSetting colorByDurability;
    private final BooleanSetting helmetFirst;
    private final BooleanSetting warnings;
    private final NumberSetting warnAt;
    private final List<String> warned = new ArrayList<String>();

    public ArmorHud()
    {
        super("Armor HUD", "Armor and held items with durability and low-durability warnings.", Category.PVP, false, 0.995D, 0.75D);
        this.layout = this.add(new ModeSetting("Layout", "Stack items vertically or in a row.", "Vertical", "Vertical", "Horizontal"));
        this.durability = this.add(new ModeSetting("Durability", "How durability is written.", "Percent", "Percent", "Remaining", "Remaining/Max", "None"));
        this.showMainHand = this.add(new BooleanSetting("Main Hand", "Include the item in your main hand.", true));
        this.showOffHand = this.add(new BooleanSetting("Off Hand", "Include the item in your off hand.", false));
        this.colorByDurability = this.add(new BooleanSetting("Durability Colors", "Colour the text from green to red.", true));
        this.helmetFirst = this.add(new BooleanSetting("Helmet First", "Order items from helmet down to boots.", true));
        this.warnings = this.add(new BooleanSetting("Low Durability Warning", "Blink and notify when a piece is almost broken.", true));
        this.warnAt = this.add(new NumberSetting("Warn At", "Warn when durability falls below this.", 10.0D, 1.0D, 50.0D, 1.0D, "%")).visibleWhen(this.warnings::isOn);
    }

    private List<ItemStack> items(boolean preview)
    {
        List<ItemStack> list = new ArrayList<ItemStack>();

        if (this.mc.player != null)
        {
            EntityEquipmentSlot[] order = this.helmetFirst.isOn() ? new EntityEquipmentSlot[] {EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST, EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET} : new EntityEquipmentSlot[] {EntityEquipmentSlot.FEET, EntityEquipmentSlot.LEGS, EntityEquipmentSlot.CHEST, EntityEquipmentSlot.HEAD};

            for (EntityEquipmentSlot slot : order)
            {
                ItemStack stack = this.mc.player.getItemStackFromSlot(slot);

                if (!stack.isEmpty())
                {
                    list.add(stack);
                }
            }

            if (this.showMainHand.isOn() && !this.mc.player.getHeldItemMainhand().isEmpty())
            {
                list.add(this.mc.player.getHeldItemMainhand());
            }

            if (this.showOffHand.isOn() && !this.mc.player.getHeldItemOffhand().isEmpty())
            {
                list.add(this.mc.player.getHeldItemOffhand());
            }
        }

        if (list.isEmpty() && preview)
        {
            list.add(new ItemStack(Items.DIAMOND_HELMET));
            list.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
            list.add(new ItemStack(Items.DIAMOND_LEGGINGS));
            list.add(new ItemStack(Items.DIAMOND_BOOTS));
            list.add(new ItemStack(Items.DIAMOND_SWORD));
        }

        return list;
    }

    private float fraction(ItemStack stack)
    {
        return 1.0F - (float)stack.getItemDamage() / (float)Math.max(1, stack.getMaxDamage());
    }

    private boolean isLow(ItemStack stack)
    {
        return this.warnings.isOn() && stack.isItemStackDamageable() && this.fraction(stack) * 100.0F < this.warnAt.getFloat();
    }

    public void onTick()
    {
        for (ItemStack stack : this.items(false))
        {
            String key = stack.getDisplayName();

            if (this.isLow(stack))
            {
                if (!this.warned.contains(key))
                {
                    this.warned.add(key);
                    Notifications.post("Low durability", key + " is almost broken!", Notifications.Kind.WARNING);
                }
            }
            else
            {
                this.warned.remove(key);
            }
        }
    }

    private String text(ItemStack stack)
    {
        if (!stack.isItemStackDamageable() || this.durability.is("None"))
        {
            return stack.getCount() > 1 ? String.valueOf(stack.getCount()) : "";
        }

        int max = stack.getMaxDamage();
        int left = max - stack.getItemDamage();

        if (this.durability.is("Remaining"))
        {
            return String.valueOf(left);
        }

        return this.durability.is("Remaining/Max") ? left + "/" + max : (left * 100 / Math.max(1, max)) + "%";
    }

    private int entryWidth(ItemStack stack)
    {
        String t = this.text(stack);
        return 16 + (t.isEmpty() ? 0 : 2 + this.mc.fontRenderer.getStringWidth(t));
    }

    public boolean shouldRender()
    {
        return !this.items(false).isEmpty();
    }

    public int getWidth()
    {
        int w = 0;

        for (ItemStack stack : this.items(true))
        {
            w = this.layout.is("Horizontal") ? w + this.entryWidth(stack) + 4 : Math.max(w, this.entryWidth(stack));
        }

        return Math.max(16, w) + this.getPad() * 2;
    }

    public int getHeight()
    {
        int n = Math.max(1, this.items(true).size());
        return (this.layout.is("Horizontal") ? 16 : n * 17 - 1) + this.getPad() * 2;
    }

    protected void renderContent(boolean preview)
    {
        this.drawBox(this.getWidth(), this.getHeight());
        int x = this.getPad();
        int y = this.getPad();
        boolean blinkOff = (Minecraft.getSystemTime() / 300L) % 2L == 0L;

        for (ItemStack stack : this.items(preview))
        {
            if (!(this.isLow(stack) && blinkOff))
            {
                this.drawItem(stack, x, y, false);
            }

            String t = this.text(stack);

            if (!t.isEmpty())
            {
                int color = this.colorByDurability.isOn() && stack.isItemStackDamageable() ? durabilityColor(this.fraction(stack)) : this.valueColor.getColor();
                this.drawText(t, (float)(x + 18), (float)(y + 4), color);
            }

            if (this.layout.is("Horizontal"))
            {
                x += this.entryWidth(stack) + 4;
            }
            else
            {
                y += 17;
            }
        }
    }
}
