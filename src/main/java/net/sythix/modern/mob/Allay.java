package net.sythix.modern.mob;

import java.util.List;
import java.util.UUID;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

/**
 * Allay: give it an item and it collects matching dropped items within 16 blocks and brings them to
 * you. Right-click it with an empty hand to take the item back.
 */
public class Allay extends EntityCreature implements IAnimals
{
    private UUID owner;
    private ItemStack carried = ItemStack.EMPTY;
    private double wx;
    private double wy;
    private double wz;

    public Allay(World worldIn)
    {
        super(worldIn);
        this.setSize(0.35F, 0.6F);
        this.setNoGravity(true);
        this.setDropChance(EntityEquipmentSlot.MAINHAND, 2.0F);
    }

    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0D);
    }

    protected boolean processInteract(EntityPlayer player, EnumHand hand)
    {
        ItemStack held = player.getHeldItem(hand);
        ItemStack liked = this.getHeldItemMainhand();

        if (this.world.isRemote)
        {
            return true;
        }

        if (held.isEmpty() && !liked.isEmpty())
        {
            this.entityDropItem(liked, 0.5F);
            this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
            this.owner = null;
            this.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, 0.8F);
            return true;
        }

        if (!held.isEmpty() && liked.isEmpty())
        {
            ItemStack copy = held.copy();
            copy.setCount(1);
            this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, copy);

            if (!player.capabilities.isCreativeMode)
            {
                held.shrink(1);
            }

            this.owner = player.getUniqueID();
            this.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, 1.6F);
            return true;
        }

        return false;
    }

    protected void updateAITasks()
    {
        super.updateAITasks();
        EntityPlayer player = this.owner == null ? null : this.world.getPlayerEntityByUUID(this.owner);
        ItemStack liked = this.getHeldItemMainhand();

        if (!liked.isEmpty() && this.carried.getCount() < this.carried.getMaxStackSize())
        {
            List<EntityItem> items = this.world.<EntityItem>getEntitiesWithinAABB(EntityItem.class, this.getEntityBoundingBox().grow(16.0D, 8.0D, 16.0D),
                e -> e.isEntityAlive() && !e.cannotPickup() && ItemStack.areItemsEqual(e.getItem(), liked) && ItemStack.areItemStackTagsEqual(e.getItem(), liked));

            if (!items.isEmpty())
            {
                EntityItem target = items.get(0);
                MobUtil.flyTowards(this, target.posX, target.posY + 0.3D, target.posZ, 0.4D);

                if (this.getDistanceSqToEntity(target) < 2.0D)
                {
                    this.pickUp(target);
                }

                return;
            }
        }

        if (!this.carried.isEmpty() && player != null)
        {
            MobUtil.flyTowards(this, player.posX, player.posY + 1.2D, player.posZ, 0.4D);

            if (this.getDistanceSqToEntity(player) < 6.0D)
            {
                EntityItem drop = new EntityItem(this.world, this.posX, this.posY, this.posZ, this.carried);
                drop.setDefaultPickupDelay();
                this.world.spawnEntity(drop);
                this.carried = ItemStack.EMPTY;
                this.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, 1.2F);
            }

            return;
        }

        if (player != null && this.getDistanceSqToEntity(player) > 36.0D)
        {
            MobUtil.flyTowards(this, player.posX, player.posY + 1.5D, player.posZ, 0.35D);
            return;
        }

        if (this.ticksExisted % 60 == 0 || this.getDistanceSq(this.wx, this.wy, this.wz) < 1.0D)
        {
            double cx = player != null ? player.posX : this.posX;
            double cy = player != null ? player.posY + 1.5D : this.posY;
            double cz = player != null ? player.posZ : this.posZ;
            this.wx = cx + this.rand.nextInt(7) - 3;
            this.wy = cy + this.rand.nextInt(3) - 1;
            this.wz = cz + this.rand.nextInt(7) - 3;
        }

        MobUtil.flyTowards(this, this.wx, this.wy, this.wz, 0.15D);
    }

    private void pickUp(EntityItem e)
    {
        ItemStack stack = e.getItem();

        if (this.carried.isEmpty())
        {
            this.carried = stack.copy();
            this.carried.setCount(0);
        }

        int take = Math.min(stack.getCount(), this.carried.getMaxStackSize() - this.carried.getCount());
        this.carried.grow(take);
        stack.shrink(take);

        if (stack.isEmpty())
        {
            e.setDead();
        }

        this.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.6F, 1.8F);
    }

    public void travel(float strafe, float vertical, float forward)
    {
        MobUtil.flyTravel(this);
    }

    public void fall(float distance, float damageMultiplier)
    {
    }

    protected boolean canDespawn()
    {
        return false;
    }

    public void writeEntityToNBT(NBTTagCompound compound)
    {
        super.writeEntityToNBT(compound);

        if (this.owner != null)
        {
            compound.setUniqueId("Liked", this.owner);
        }

        if (!this.carried.isEmpty())
        {
            compound.setTag("Carried", this.carried.writeToNBT(new NBTTagCompound()));
        }
    }

    public void readEntityFromNBT(NBTTagCompound compound)
    {
        super.readEntityFromNBT(compound);
        this.owner = compound.hasUniqueId("Liked") ? compound.getUniqueId("Liked") : null;
        this.carried = compound.hasKey("Carried") ? new ItemStack(compound.getCompoundTag("Carried")) : ItemStack.EMPTY;
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
        if (!this.carried.isEmpty())
        {
            this.entityDropItem(this.carried, 0.3F);
            this.carried = ItemStack.EMPTY;
        }
    }

    protected ResourceLocation getLootTable()
    {
        return null;
    }

    /** Allays do not spawn naturally (in 1.21 they are found in outposts and mansions). */
    public boolean getCanSpawnHere()
    {
        return false;
    }

    protected SoundEvent getAmbientSound()
    {
        return SoundEvents.ENTITY_PARROT_AMBIENT;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return SoundEvents.ENTITY_PARROT_HURT;
    }

    protected SoundEvent getDeathSound()
    {
        return SoundEvents.ENTITY_PARROT_DEATH;
    }

    protected float getSoundPitch()
    {
        return 1.6F;
    }
}
