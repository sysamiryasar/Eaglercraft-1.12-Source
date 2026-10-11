package net.sythix.modern.mob;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Strider: walks on lava in the Nether. Saddle it and steer it with a warped fungus on a stick.
 */
public class Strider extends EntityPig
{
    public Strider(World worldIn)
    {
        super(worldIn);
        this.setSize(0.9F, 1.7F);
        this.isImmuneToFire = true;
        this.setPathPriority(PathNodeType.LAVA, 0.0F);
        this.setPathPriority(PathNodeType.DANGER_FIRE, 0.0F);
        this.setPathPriority(PathNodeType.DAMAGE_FIRE, 0.0F);
    }

    protected void initEntityAI()
    {
        this.tasks.addTask(1, new EntityAIPanic(this, 1.65D));
        this.tasks.addTask(2, new EntityAIMate(this, 1.0D));
        Item stick = MobUtil.item("warped_fungus_on_a_stick");

        if (stick != null)
        {
            this.tasks.addTask(3, new EntityAITempt(this, 1.4D, stick, false));
        }

        this.tasks.addTask(3, new EntityAITempt(this, 1.1D, Items.NETHER_WART, false));
        this.tasks.addTask(4, new EntityAIFollowParent(this, 1.1D));
        this.tasks.addTask(5, new EntityAIWander(this, 1.0D, 60));
        this.tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(7, new EntityAILookIdle(this));
    }

    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.175D);
    }

    /** Steered with a warped fungus on a stick instead of a carrot on a stick. */
    public boolean canBeSteered()
    {
        Entity rider = this.getControllingPassenger();

        if (!(rider instanceof EntityPlayer))
        {
            return false;
        }

        Item stick = MobUtil.item("warped_fungus_on_a_stick");
        EntityPlayer p = (EntityPlayer)rider;
        return stick != null && (p.getHeldItemMainhand().getItem() == stick || p.getHeldItemOffhand().getItem() == stick);
    }

    public void travel(float strafe, float vertical, float forward)
    {
        super.travel(strafe, vertical, forward);
        BlockPos at = new BlockPos(this.posX, this.posY, this.posZ);

        if (this.world.getBlockState(at).getMaterial() == Material.LAVA && this.world.getBlockState(at.up()).getMaterial() != Material.LAVA)
        {
            // stand on top of the lava
            this.setPosition(this.posX, at.getY() + 1.0D, this.posZ);
            this.motionY = 0.0D;
            this.onGround = true;
            this.fallDistance = 0.0F;
        }
        else if (this.isInLava())
        {
            this.motionY = 0.15D;
        }
    }

    public boolean isBreedingItem(ItemStack stack)
    {
        return stack.getItem() == Items.NETHER_WART;
    }

    public Strider createChild(EntityAgeable ageable)
    {
        return new Strider(this.world);
    }

    public void onStruckByLightning(EntityLightningBolt lightningBolt)
    {
    }

    public boolean getCanSpawnHere()
    {
        return MobUtil.naturalSpawns();
    }

    protected ResourceLocation getLootTable()
    {
        return null;
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
        MobUtil.drop(this, "string", 2, 5, lootingModifier);
    }
}
