package net.sythix.modern.mob;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

/**
 * Piglin: a nether mob that leaves players wearing gold armour alone and trades (barters) for gold
 * ingots. Outside the Nether it turns into a zombie pigman after 15 seconds.
 */
public class Piglin extends EntityMob
{
    private static final Object[][] BARTER = new Object[][] {
        {"ender_pearl", 2, 4, 10}, {"string", 3, 9, 20}, {"quartz", 5, 12, 20}, {"iron_nugget", 9, 36, 10}, {"leather", 2, 4, 40},
        {"nether_brick", 2, 8, 40}, {"soul_sand", 2, 8, 40}, {"gravel", 8, 16, 40}, {"fire_charge", 1, 1, 40}, {"obsidian", 1, 1, 40},
        {"spectral_arrow", 6, 12, 40}, {"magma_cream", 2, 6, 10}};

    private int admiring;
    private int outsideNether;

    public Piglin(World worldIn)
    {
        super(worldIn);
        this.setSize(0.6F, 1.95F);
        this.experienceValue = 5;
    }

    protected void initEntityAI()
    {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(2, new EntityAIAttackMelee(this, 1.0D, false));
        this.tasks.addTask(7, new EntityAIWanderAvoidWater(this, 0.8D));
        this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(8, new EntityAILookIdle(this));
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, 10, true, false, p -> !this.ignores(p)));
    }

    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(16.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.35D);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(5.0D);
    }

    /** Players wearing a piece of gold armour are left alone (brutes ignore this). */
    protected boolean ignores(EntityPlayer player)
    {
        for (ItemStack armor : player.getArmorInventoryList())
        {
            if (armor.getItem() instanceof ItemArmor && ((ItemArmor)armor.getItem()).getArmorMaterial() == ItemArmor.ArmorMaterial.GOLD)
            {
                return true;
            }
        }

        return false;
    }

    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, IEntityLivingData livingdata)
    {
        livingdata = super.onInitialSpawn(difficulty, livingdata);
        this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));

        if (this.rand.nextFloat() < 0.1F)
        {
            this.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
        }

        return livingdata;
    }

    /** Giving a piglin a gold ingot: it admires it for a few seconds and then throws back an item. */
    protected boolean processInteract(EntityPlayer player, EnumHand hand)
    {
        ItemStack held = player.getHeldItem(hand);

        if (this.canBarter() && held.getItem() == Items.GOLD_INGOT && this.admiring <= 0 && this.getAttackTarget() == null)
        {
            if (!this.world.isRemote)
            {
                if (!player.capabilities.isCreativeMode)
                {
                    held.shrink(1);
                }

                this.admiring = 120;
                this.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, new ItemStack(Items.GOLD_INGOT));
                this.playSound(SoundEvents.ENTITY_ZOMBIE_PIG_AMBIENT, 1.0F, 1.4F);
            }

            return true;
        }

        return super.processInteract(player, hand);
    }

    protected boolean canBarter()
    {
        return true;
    }

    public void onLivingUpdate()
    {
        super.onLivingUpdate();

        if (this.world.isRemote)
        {
            return;
        }

        if (this.admiring > 0)
        {
            this.getNavigator().clearPathEntity();

            if (--this.admiring == 0)
            {
                this.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, ItemStack.EMPTY);
                this.barter();
            }
        }

        if (!this.world.provider.isNether() && ++this.outsideNether > 300)
        {
            EntityPigZombie zombie = new EntityPigZombie(this.world);
            zombie.copyLocationAndAnglesFrom(this);
            zombie.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, this.getHeldItemMainhand().copy());
            zombie.addPotionEffect(new net.minecraft.potion.PotionEffect(net.minecraft.init.MobEffects.NAUSEA, 200));
            this.world.removeEntity(this);
            this.world.spawnEntity(zombie);
            this.playSound(SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0F, 1.0F);
        }
    }

    private void barter()
    {
        int total = 0;

        for (Object[] o : BARTER)
        {
            total += (Integer)o[3];
        }

        int pick = this.rand.nextInt(total);

        for (Object[] o : BARTER)
        {
            pick -= (Integer)o[3];

            if (pick < 0)
            {
                MobUtil.drop(this, (String)o[0], (Integer)o[1], (Integer)o[2], 0);
                break;
            }
        }

        this.playSound(SoundEvents.ENTITY_ZOMBIE_PIG_AMBIENT, 1.0F, 1.6F);
    }

    public boolean getCanSpawnHere()
    {
        return MobUtil.naturalSpawns() && super.getCanSpawnHere();
    }

    protected boolean isValidLightLevel()
    {
        return this.world.provider.isNether() || super.isValidLightLevel();
    }

    public void writeEntityToNBT(NBTTagCompound compound)
    {
        super.writeEntityToNBT(compound);
        compound.setInteger("TimeOutsideNether", this.outsideNether);
    }

    public void readEntityFromNBT(NBTTagCompound compound)
    {
        super.readEntityFromNBT(compound);
        this.outsideNether = compound.getInteger("TimeOutsideNether");
    }

    protected ResourceLocation getLootTable()
    {
        return null;
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
        if (this.rand.nextInt(10) == 0)
        {
            MobUtil.drop(this, "gold_nugget", 1, 3, lootingModifier);
        }
    }

    protected SoundEvent getAmbientSound()
    {
        return SoundEvents.ENTITY_ZOMBIE_PIG_AMBIENT;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return SoundEvents.ENTITY_ZOMBIE_PIG_HURT;
    }

    protected SoundEvent getDeathSound()
    {
        return SoundEvents.ENTITY_ZOMBIE_PIG_DEATH;
    }

    protected float getSoundPitch()
    {
        return 1.3F + this.rand.nextFloat() * 0.2F;
    }

    public boolean isAdmiring()
    {
        return this.admiring > 0;
    }

    /** For the target AI. */
    public void setAttackTarget(EntityLivingBase target)
    {
        super.setAttackTarget(target);

        if (target != null)
        {
            this.admiring = 0;
        }
    }
}
