package net.sythix.modern.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.sythix.modern.ModernBlocks;

/**
 * A configurable block for most of the extra 1.21 blocks: full cubes, decorative shapes, light
 * sources, ores (drops + experience), slippery and sticky blocks.
 */
public class MBlock extends Block
{
    private AxisAlignedBB box = FULL_BLOCK_AABB;
    private AxisAlignedBB collision = FULL_BLOCK_AABB;
    private BlockRenderLayer layer = BlockRenderLayer.SOLID;
    private boolean fullCube = true;
    private String dropName;
    private int dropMin = 1;
    private int dropMax = 1;
    private int xpMin;
    private int xpMax;
    private boolean fortune;
    private float fallFactor = 1.0F;
    private float slow = 1.0F;
    private float touchDamage;
    private boolean noDrop;

    public MBlock(Material material)
    {
        super(material);
    }

    public MBlock(Material material, MapColor color)
    {
        super(material, color);
    }

    // ---- Builder ------------------------------------------------------------------------------

    /** Shape in pixels (0-16), also used for collision. */
    public MBlock shape(double x1, double y1, double z1, double x2, double y2, double z2)
    {
        this.box = new AxisAlignedBB(x1 / 16.0D, y1 / 16.0D, z1 / 16.0D, x2 / 16.0D, y2 / 16.0D, z2 / 16.0D);
        this.collision = this.box;
        this.fullCube = false;
        return this;
    }

    /** Collision box in pixels (null = walk through). */
    public MBlock collision(@Nullable double[] pixels)
    {
        this.collision = pixels == null ? null : new AxisAlignedBB(pixels[0] / 16.0D, pixels[1] / 16.0D, pixels[2] / 16.0D, pixels[3] / 16.0D, pixels[4] / 16.0D, pixels[5] / 16.0D);
        return this;
    }

    public MBlock layer(BlockRenderLayer renderLayer)
    {
        this.layer = renderLayer;

        if (renderLayer != BlockRenderLayer.SOLID)
        {
            this.fullCube = false;
        }

        return this;
    }

    /** Drops another item (by registry name) instead of itself. */
    public MBlock drops(String itemName, int min, int max, boolean fortuneBonus)
    {
        this.dropName = itemName;
        this.dropMin = min;
        this.dropMax = max;
        this.fortune = fortuneBonus;
        return this;
    }

    public MBlock xp(int min, int max)
    {
        this.xpMin = min;
        this.xpMax = max;
        return this;
    }

    public MBlock slippery(float value)
    {
        this.slipperiness = value;
        return this;
    }

    /** Fall damage multiplier (honey: 0.2). */
    public MBlock softLanding(float factor)
    {
        this.fallFactor = factor;
        return this;
    }

    /** Movement multiplier for entities inside or on top (honey, mud). */
    public MBlock slows(float factor)
    {
        this.slow = factor;
        return this;
    }

    /** Damage per second to entities standing on it (campfires). */
    public MBlock hurts(float damage)
    {
        this.touchDamage = damage;
        return this;
    }

    public MBlock noDrops()
    {
        this.noDrop = true;
        return this;
    }

    // ---- Shape and rendering ------------------------------------------------------------------

    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
    {
        return this.box;
    }

    @Nullable
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos)
    {
        return this.collision;
    }

    public boolean isOpaqueCube(IBlockState state)
    {
        return this.fullCube;
    }

    public boolean isFullCube(IBlockState state)
    {
        return this.fullCube;
    }

    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face)
    {
        return this.box == FULL_BLOCK_AABB ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    public BlockRenderLayer getBlockLayer()
    {
        return this.layer;
    }

    public boolean isPassable(IBlockAccess worldIn, BlockPos pos)
    {
        return this.collision == null || super.isPassable(worldIn, pos);
    }

    public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
    {
        // See-through cubes of the same kind hide the faces between them (like glass)
        if (this.layer == BlockRenderLayer.TRANSLUCENT && this.box == FULL_BLOCK_AABB && blockAccess.getBlockState(pos.offset(side)).getBlock() == this)
        {
            return false;
        }

        return this.box != FULL_BLOCK_AABB || super.shouldSideBeRendered(blockState, blockAccess, pos, side);
    }

    // ---- Drops ----------------------------------------------------------------------------------

    public Item getItemDropped(IBlockState state, Random rand, int fortuneLevel)
    {
        if (this.noDrop)
        {
            return net.minecraft.init.Items.AIR;
        }

        if (this.dropName != null)
        {
            Item item = ModernBlocks.item(this.dropName);

            if (item != null)
            {
                return item;
            }
        }

        return super.getItemDropped(state, rand, fortuneLevel);
    }

    public int damageDropped(IBlockState state)
    {
        // Lapis lazuli is dye meta 4, bone meal 15
        if ("dye".equals(this.dropName))
        {
            return 4;
        }

        return 0;
    }

    public int quantityDropped(Random random)
    {
        return this.dropName == null ? 1 : this.dropMin + random.nextInt(this.dropMax - this.dropMin + 1);
    }

    public int quantityDroppedWithBonus(int fortuneLevel, Random random)
    {
        int base = this.quantityDropped(random);

        if (this.fortune && fortuneLevel > 0)
        {
            int bonus = random.nextInt(fortuneLevel + 2) - 1;
            return base * (Math.max(0, bonus) + 1);
        }

        return base;
    }

    public void dropBlockAsItemWithChance(World worldIn, BlockPos pos, IBlockState state, float chance, int fortuneLevel)
    {
        super.dropBlockAsItemWithChance(worldIn, pos, state, chance, fortuneLevel);

        if (this.xpMax > 0)
        {
            this.dropXpOnBlockBreak(worldIn, pos, MathHelper.getInt(worldIn.rand, this.xpMin, this.xpMax));
        }
    }

    // ---- Entity effects -------------------------------------------------------------------------

    public void onFallenUpon(World worldIn, BlockPos pos, Entity entityIn, float fallDistance)
    {
        entityIn.fall(fallDistance * this.fallFactor, 1.0F);
    }

    public void onEntityCollidedWithBlock(World worldIn, BlockPos pos, IBlockState state, Entity entityIn)
    {
        if (this.slow < 1.0F)
        {
            entityIn.motionX *= (double)this.slow;
            entityIn.motionZ *= (double)this.slow;
        }
    }

    public void onEntityWalk(World worldIn, BlockPos pos, Entity entityIn)
    {
        if (this.touchDamage > 0.0F && !worldIn.isRemote && entityIn instanceof EntityLivingBase && !entityIn.isSneaking() && !entityIn.isImmuneToFire() && entityIn.ticksExisted % 20 == 0)
        {
            entityIn.attackEntityFrom(DamageSource.IN_FIRE, this.touchDamage);
        }

        if (this.slow < 1.0F)
        {
            entityIn.motionX *= (double)this.slow;
            entityIn.motionZ *= (double)this.slow;
        }

        super.onEntityWalk(worldIn, pos, entityIn);
    }
}
