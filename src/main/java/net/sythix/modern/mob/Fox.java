package net.sythix.modern.mob;

import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILeapAtTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISit;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

/** Fox: a shy taiga hunter of chickens and rabbits. It cannot be tamed; breed foxes with sweet berries. */
public class Fox extends EntityWolf
{
    public Fox(World worldIn)
    {
        super(worldIn);
        this.setSize(0.6F, 0.7F);
    }

    protected void initEntityAI()
    {
        this.aiSit = new EntityAISit(this);
        this.tasks.addTask(1, new EntityAISwimming(this));
        this.tasks.addTask(2, new EntityAIAvoidEntity<EntityPlayer>(this, EntityPlayer.class, p -> !p.isSneaking(), 8.0F, 1.2D, 1.5D));
        this.tasks.addTask(2, new EntityAIAvoidEntity<EntityWolf>(this, EntityWolf.class, w -> !(w instanceof Fox), 8.0F, 1.2D, 1.5D));
        this.tasks.addTask(4, new EntityAILeapAtTarget(this, 0.4F));
        this.tasks.addTask(5, new EntityAIAttackMelee(this, 1.2D, true));
        this.tasks.addTask(6, new EntityAIMate(this, 1.0D));
        Item berries = MobUtil.item("sweet_berries");

        if (berries != null)
        {
            this.tasks.addTask(7, new EntityAITempt(this, 1.0D, berries, true));
        }

        this.tasks.addTask(8, new EntityAIWanderAvoidWater(this, 1.0D));
        this.tasks.addTask(10, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(10, new EntityAILookIdle(this));
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityChicken>(this, EntityChicken.class, false));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityRabbit>(this, EntityRabbit.class, 10, false, false, r -> !(r instanceof Frog)));
    }

    /** Foxes ignore bones and meat: only sweet berries work on them. */
    public boolean processInteract(EntityPlayer player, EnumHand hand)
    {
        ItemStack held = player.getHeldItem(hand);

        if (held.getItem() == Items.BONE || (!this.isBreedingItem(held) && held.getItem() instanceof net.minecraft.item.ItemFood))
        {
            return false;
        }

        return super.processInteract(player, hand);
    }

    public boolean isBreedingItem(ItemStack stack)
    {
        Item berries = MobUtil.item("sweet_berries");
        return berries != null && stack.getItem() == berries;
    }

    public boolean canMateWith(EntityAnimal otherAnimal)
    {
        return otherAnimal != this && otherAnimal instanceof Fox && this.isInLove() && otherAnimal.isInLove();
    }

    public Fox createChild(EntityAgeable ageable)
    {
        return new Fox(this.world);
    }

    public boolean getCanSpawnHere()
    {
        return MobUtil.naturalSpawns() && super.getCanSpawnHere();
    }

    protected ResourceLocation getLootTable()
    {
        return null;
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
        if (this.rand.nextInt(5) == 0)
        {
            MobUtil.drop(this, "sweet_berries", 1, 1, 0);
        }
    }

    protected SoundEvent getAmbientSound()
    {
        return null;
    }

    protected float getSoundPitch()
    {
        return 1.6F;
    }
}
