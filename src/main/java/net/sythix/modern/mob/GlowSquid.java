package net.sythix.modern.mob;

import net.minecraft.block.material.Material;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Glow squid: a glowing squid of dark, deep water. Drops glow ink sacs. */
public class GlowSquid extends EntitySquid
{
    public GlowSquid(World worldIn)
    {
        super(worldIn);
    }

    public void onLivingUpdate()
    {
        super.onLivingUpdate();

        if (this.world.isRemote && this.rand.nextInt(6) == 0)
        {
            this.world.spawnParticle(EnumParticleTypes.WATER_WAKE, this.posX + (this.rand.nextDouble() - 0.5D) * this.width, this.posY + this.rand.nextDouble() * this.height, this.posZ + (this.rand.nextDouble() - 0.5D) * this.width, 0.0D, 0.0D, 0.0D);
        }
    }

    /** Always drawn at full brightness. */
    public int getBrightnessForRender()
    {
        return 15728880;
    }

    public boolean getCanSpawnHere()
    {
        return MobUtil.naturalSpawns() && this.posY < 40.0D && this.world.getBlockState(new BlockPos(this)).getMaterial() == Material.WATER && this.world.getLight(new BlockPos(this)) < 7 && this.world.checkNoEntityCollision(this.getEntityBoundingBox());
    }

    protected ResourceLocation getLootTable()
    {
        return null;
    }

    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
        MobUtil.drop(this, "glow_ink_sac", 1, 3, lootingModifier);
    }
}
