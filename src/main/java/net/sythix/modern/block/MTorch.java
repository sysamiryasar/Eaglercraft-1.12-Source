package net.sythix.modern.block;

import net.minecraft.block.BlockTorch;
import net.minecraft.block.SoundType;

/**
 * Soul torch: a blue torch that gives a little less light.
 */
public class MTorch extends BlockTorch
{
    public MTorch(int lightLevel)
    {
        super();
        this.setHardness(0.0F);
        net.minecraft.block.SythixBlockAccess.light(this, lightLevel);
        net.minecraft.block.SythixBlockAccess.sound(this, SoundType.WOOD);
    }
}
