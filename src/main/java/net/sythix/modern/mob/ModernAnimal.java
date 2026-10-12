package net.sythix.modern.mob;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/** Base of the new farm-style animals: wander, panic when hurt, follow food and breed with it. */
public abstract class ModernAnimal extends EntityAnimal
{
    public ModernAnimal(World worldIn)
    {
        super(worldIn);
    }

    protected void initEntityAI()
    {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(1, new EntityAIPanic(this, 1.4D));
        this.tasks.addTask(2, new EntityAIMate(this, 1.0D));
        Item food = MobUtil.item(this.food());

        if (food != null)
        {
            this.tasks.addTask(3, new EntityAITempt(this, 1.1D, food, false));
        }

        this.tasks.addTask(4, new EntityAIFollowParent(this, 1.1D));
        this.tasks.addTask(5, new EntityAIWanderAvoidWater(this, 1.0D));
        this.tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
        this.tasks.addTask(7, new EntityAILookIdle(this));
    }

    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(this.maxHealth());
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(this.speed());
    }

    protected abstract double maxHealth();

    protected abstract double speed();

    /** Registry name of the item used to breed and lure this animal. */
    protected abstract String food();

    public boolean isBreedingItem(ItemStack stack)
    {
        Item food = MobUtil.item(this.food());
        return food != null && stack.getItem() == food;
    }

    public EntityAgeable createChild(EntityAgeable ageable)
    {
        try
        {
            return this.getClass().getConstructor(World.class).newInstance(this.world);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /** Natural spawning: the "New Mobs" setting, enough light and one of the given ground blocks. */
    protected boolean canSpawnOn(Block... ground)
    {
        if (!MobUtil.naturalSpawns())
        {
            return false;
        }

        BlockPos below = new BlockPos(MathHelper.floor(this.posX), MathHelper.floor(this.getEntityBoundingBox().minY) - 1, MathHelper.floor(this.posZ));
        Block b = this.world.getBlockState(below).getBlock();
        boolean ok = ground.length == 0 ? b == Blocks.GRASS : false;

        for (Block g : ground)
        {
            ok |= b == g;
        }

        return ok && this.world.getLight(below.up()) > 8 && this.world.checkNoEntityCollision(this.getEntityBoundingBox()) && this.world.getCollisionBoxes(this, this.getEntityBoundingBox()).isEmpty();
    }

    public boolean getCanSpawnHere()
    {
        return this.canSpawnOn();
    }

    protected ResourceLocation getLootTable()
    {
        return null;
    }
}
