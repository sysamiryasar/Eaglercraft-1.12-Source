package net.sythix.modern.mob;

import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

/** Camel: a tall desert mount. It needs no taming: put a saddle on it and ride. Breed with cactus. */
public class Camel extends EntityHorse
{
    public Camel(World worldIn)
    {
        super(worldIn);
        this.setSize(1.7F, 2.375F);
        this.stepHeight = 1.5F;
    }

    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(32.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.2D);
        this.getEntityAttribute(JUMP_STRENGTH).setBaseValue(0.42D);
    }

    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, IEntityLivingData livingdata)
    {
        livingdata = super.onInitialSpawn(difficulty, livingdata);
        this.setHorseTamed(true);
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(32.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.2D);
        this.getEntityAttribute(JUMP_STRENGTH).setBaseValue(0.42D);
        this.setHealth(32.0F);
        return livingdata;
    }

    protected boolean handleEating(EntityPlayer player, ItemStack stack)
    {
        if (stack.getItem() == Item.getItemFromBlock(Blocks.CACTUS))
        {
            if (this.getGrowingAge() == 0 && !this.isInLove() && !this.isChild())
            {
                this.setInLove(player);
            }
            else
            {
                this.heal(2.0F);
            }

            return true;
        }

        return super.handleEating(player, stack);
    }

    public boolean isBreedingItem(ItemStack stack)
    {
        return stack.getItem() == Item.getItemFromBlock(Blocks.CACTUS);
    }

    public boolean canMateWith(EntityAnimal otherAnimal)
    {
        return otherAnimal != this && otherAnimal instanceof Camel && this.canMate() && ((Camel)otherAnimal).canMate();
    }

    public EntityAgeable createChild(EntityAgeable ageable)
    {
        Camel baby = new Camel(this.world);
        baby.setHorseTamed(true);
        return baby;
    }

    public boolean getCanSpawnHere()
    {
        BlockPos below = new BlockPos(MathHelper.floor(this.posX), MathHelper.floor(this.getEntityBoundingBox().minY) - 1, MathHelper.floor(this.posZ));
        return MobUtil.naturalSpawns() && this.world.getBlockState(below).getBlock() == Blocks.SAND && this.world.getLight(below.up()) > 8 && this.world.getCollisionBoxes(this, this.getEntityBoundingBox()).isEmpty();
    }
}
