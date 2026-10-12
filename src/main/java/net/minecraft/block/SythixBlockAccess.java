package net.minecraft.block;

import net.minecraft.block.SoundType;

/**
 * Lets Sythix code outside this package use Block's protected builder methods (sound, light,
 * resistance...) when registering the extra 1.21 blocks.
 */
public final class SythixBlockAccess
{
    private SythixBlockAccess()
    {
    }

    public static Block hardness(Block block, float hardness)
    {
        return block.setHardness(hardness);
    }

    public static Block sound(Block block, SoundType sound)
    {
        return block.setSoundType(sound);
    }

    /** Light level 0-15. */
    public static Block light(Block block, int level)
    {
        return block.setLightLevel((float)level / 15.0F);
    }

    public static Block resistance(Block block, float resistance)
    {
        return block.setResistance(resistance);
    }

    public static Block opacity(Block block, int opacity)
    {
        return block.setLightOpacity(opacity);
    }

    public static Block unbreakable(Block block)
    {
        return block.setBlockUnbreakable();
    }

    public static Block tickRandomly(Block block)
    {
        return block.setTickRandomly(true);
    }
}
