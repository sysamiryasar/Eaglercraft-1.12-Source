package net.sythix.modern.item;

import javax.annotation.Nullable;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;

/**
 * Crossbow: hold right-click to load an arrow, then right-click again to fire it fast and far.
 */
public class MCrossbow extends Item
{
    public MCrossbow()
    {
        this.setMaxStackSize(1);
        this.setMaxDamage(465);
        this.addPropertyOverride(new ResourceLocation("pull"), new IItemPropertyGetter()
        {
            public float apply(ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn)
            {
                return entityIn == null || entityIn.getActiveItemStack().getItem() != MCrossbow.this ? 0.0F : (float)(stack.getMaxItemUseDuration() - entityIn.getItemInUseCount()) / (float)chargeTime(stack);
            }
        });
        this.addPropertyOverride(new ResourceLocation("pulling"), new IItemPropertyGetter()
        {
            public float apply(ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn)
            {
                return entityIn != null && entityIn.isHandActive() && entityIn.getActiveItemStack() == stack ? 1.0F : 0.0F;
            }
        });
        this.addPropertyOverride(new ResourceLocation("charged"), new IItemPropertyGetter()
        {
            public float apply(ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn)
            {
                return isCharged(stack) ? 1.0F : 0.0F;
            }
        });
    }

    public static boolean isCharged(ItemStack stack)
    {
        return stack.hasTagCompound() && stack.getTagCompound().getBoolean("Charged");
    }

    private static void setCharged(ItemStack stack, boolean charged)
    {
        if (!stack.hasTagCompound())
        {
            stack.setTagCompound(new NBTTagCompound());
        }

        stack.getTagCompound().setBoolean("Charged", charged);
    }

    private static ItemStack findArrow(EntityPlayer player)
    {
        for (int i = 0; i < player.inventory.getSizeInventory(); ++i)
        {
            ItemStack s = player.inventory.getStackInSlot(i);

            if (s.getItem() instanceof ItemArrow)
            {
                return s;
            }
        }

        return ItemStack.EMPTY;
    }

    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn)
    {
        ItemStack stack = playerIn.getHeldItem(handIn);

        if (isCharged(stack))
        {
            if (!worldIn.isRemote)
            {
                // Multishot fires three arrows in a spread
                boolean multi = net.sythix.modern.ModernEffects.level("multishot", stack) > 0;
                int pierce = net.sythix.modern.ModernEffects.level("piercing", stack);

                for (int i = multi ? -1 : 0; i <= (multi ? 1 : 0); ++i)
                {
                    ItemArrow arrowItem = (ItemArrow)Items.ARROW;
                    EntityArrow arrow = arrowItem.createArrow(worldIn, new ItemStack(Items.ARROW), playerIn);
                    arrow.setAim(playerIn, playerIn.rotationPitch, playerIn.rotationYaw + (float)i * 10.0F, 0.0F, 3.15F, 1.0F);
                    // Piercing: harder hits (1.12 arrows cannot pass through entities)
                    arrow.setDamage(arrow.getDamage() + 1.5D + pierce * 0.5D);
                    arrow.setIsCritical(true);

                    if (playerIn.capabilities.isCreativeMode || i != 0)
                    {
                        arrow.pickupStatus = EntityArrow.PickupStatus.CREATIVE_ONLY;
                    }

                    worldIn.spawnEntity(arrow);
                }

                stack.damageItem(multi ? 3 : 1, playerIn);
            }

            setCharged(stack, false);
            worldIn.playSound((EntityPlayer)null, playerIn.posX, playerIn.posY, playerIn.posZ, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.0F, 1.4F);
            return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
        }

        if (playerIn.capabilities.isCreativeMode || !findArrow(playerIn).isEmpty())
        {
            playerIn.setActiveHand(handIn);
            return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
        }

        return new ActionResult<ItemStack>(EnumActionResult.FAIL, stack);
    }

    public void onPlayerStoppedUsing(ItemStack stack, World worldIn, EntityLivingBase entityLiving, int timeLeft)
    {
        if (!(entityLiving instanceof EntityPlayer) || this.getMaxItemUseDuration(stack) - timeLeft < chargeTime(stack))
        {
            return;
        }

        EntityPlayer player = (EntityPlayer)entityLiving;

        if (!player.capabilities.isCreativeMode)
        {
            ItemStack arrow = findArrow(player);

            if (arrow.isEmpty())
            {
                return;
            }

            arrow.shrink(1);
        }

        setCharged(stack, true);
        worldIn.playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.PLAYERS, 0.6F, 1.6F);
    }

    /** Ticks to load (Quick Charge makes it faster). */
    static int chargeTime(ItemStack stack)
    {
        return Math.max(5, 25 - 5 * net.sythix.modern.ModernEffects.level("quick_charge", stack));
    }

    public int getMaxItemUseDuration(ItemStack stack)
    {
        return 72000;
    }

    public EnumAction getItemUseAction(ItemStack stack)
    {
        return EnumAction.BOW;
    }

    public int getItemEnchantability()
    {
        return 1;
    }
}
