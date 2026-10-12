package net.sythix.modern.block;

import net.minecraft.block.BlockFalling;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;

/**
 * Suspicious sand and gravel: fall like sand; brush them to find old items (see the Brush).
 */
public class MSuspicious extends BlockFalling
{
    private final String plain;

    public MSuspicious(Material material, String plainBlock)
    {
        super(material);
        this.plain = plainBlock;
        net.minecraft.block.SythixBlockAccess.sound(this, material == Material.SAND ? SoundType.SAND : SoundType.GROUND);
    }

    /** The block it turns into after brushing: "sand" or "gravel". */
    public String getPlainBlock()
    {
        return this.plain;
    }
}
