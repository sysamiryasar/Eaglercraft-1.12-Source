package net.sythix.modern.item;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

/**
 * New foods: berries, dried kelp (quick to eat), suspicious stew (random effect), honey bottle
 * (cures poison) and the ominous bottle.
 */
public class MFood extends ItemFood
{
    public static final int NORMAL = 0;
    public static final int FAST = 1;
    public static final int STEW = 2;
    public static final int HONEY = 3;
    public static final int OMINOUS = 4;
    private final int kind;

    public MFood(int amount, float saturation, int foodKind)
    {
        super(amount, saturation, false);
        this.kind = foodKind;

        if (foodKind == STEW)
        {
            this.setMaxStackSize(1);
        }
        else if (foodKind == HONEY || foodKind == OMINOUS)
        {
            this.setMaxStackSize(16);
            this.setAlwaysEdible();
        }
    }

    public int getMaxItemUseDuration(ItemStack stack)
    {
        return this.kind == FAST ? 16 : (this.kind == HONEY || this.kind == OMINOUS ? 40 : 32);
    }

    public EnumAction getItemUseAction(ItemStack stack)
    {
        return this.kind == HONEY || this.kind == OMINOUS ? EnumAction.DRINK : EnumAction.EAT;
    }

    protected void onFoodEaten(ItemStack stack, World worldIn, EntityPlayer player)
    {
        if (worldIn.isRemote)
        {
            return;
        }

        if (this.kind == STEW)
        {
            Potion[] effects = new Potion[] {MobEffects.NIGHT_VISION, MobEffects.JUMP_BOOST, MobEffects.WEAKNESS, MobEffects.BLINDNESS, MobEffects.POISON, MobEffects.SATURATION, MobEffects.FIRE_RESISTANCE, MobEffects.REGENERATION};
            player.addPotionEffect(new PotionEffect(effects[worldIn.rand.nextInt(effects.length)], 140));
        }
        else if (this.kind == HONEY)
        {
            player.removePotionEffect(MobEffects.POISON);
        }
        else if (this.kind == OMINOUS)
        {
            Potion badOmen = Potion.REGISTRY.getObject(new net.minecraft.util.ResourceLocation("bad_omen"));

            if (badOmen != null)
            {
                player.addPotionEffect(new PotionEffect(badOmen, 120000));
            }
        }
    }

    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, EntityLivingBase entityLiving)
    {
        ItemStack result = super.onItemUseFinish(stack, worldIn, entityLiving);

        if (entityLiving instanceof EntityPlayer && !((EntityPlayer)entityLiving).capabilities.isCreativeMode)
        {
            ItemStack container = this.kind == STEW ? new ItemStack(Items.BOWL) : (this.kind == HONEY ? new ItemStack(Items.GLASS_BOTTLE) : ItemStack.EMPTY);

            if (!container.isEmpty())
            {
                if (result.isEmpty())
                {
                    return container;
                }

                ((EntityPlayer)entityLiving).inventory.addItemStackToInventory(container);
            }
        }

        return result;
    }
}
