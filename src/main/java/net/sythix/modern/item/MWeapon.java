package net.sythix.modern.item;

import com.google.common.collect.Multimap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A melee weapon with its own damage and speed (mace, spear, trident).
 */
public class MWeapon extends Item
{
    private final double damage;
    private final double speed;

    /** damage = extra attack damage (a fist does 1), speed = attack speed modifier. */
    public MWeapon(int durability, double attackDamage, double attackSpeed)
    {
        this.damage = attackDamage;
        this.speed = attackSpeed;
        this.setMaxStackSize(1);
        this.setMaxDamage(durability);
    }

    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker)
    {
        stack.damageItem(1, attacker);
        return true;
    }

    public boolean onBlockDestroyed(ItemStack stack, World worldIn, IBlockState state, BlockPos pos, EntityLivingBase entityLiving)
    {
        if ((double)state.getBlockHardness(worldIn, pos) != 0.0D)
        {
            stack.damageItem(2, entityLiving);
        }

        return true;
    }

    public boolean isFull3D()
    {
        return true;
    }

    public int getItemEnchantability()
    {
        return 15;
    }

    public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot equipmentSlot)
    {
        Multimap<String, AttributeModifier> multimap = super.getItemAttributeModifiers(equipmentSlot);

        if (equipmentSlot == EntityEquipmentSlot.MAINHAND)
        {
            multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", this.damage, 0));
            multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", this.speed, 0));
        }

        return multimap;
    }
}
