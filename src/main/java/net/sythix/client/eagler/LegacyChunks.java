package net.sythix.client.eagler;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.chunk.BlockStateContainer;

/**
 * Chunks between 1.8 and 1.12. A 1.8 section is 4096 block numbers (block id << 4 | data) with
 * all sections' light after all their blocks; 1.12 packs each section's blocks with a palette and
 * keeps its light next to it. The section is rebuilt with the game's own BlockStateContainer, so
 * the bytes are exactly what the game reads.
 */
final class LegacyChunks
{
    private static final IBlockState[] STATES = new IBlockState[65536];

    private LegacyChunks()
    {
    }

    /** The block state of a 1.8 block number (id << 4 | data). */
    static IBlockState state(int legacy)
    {
        legacy &= 65535;
        IBlockState state = STATES[legacy];

        if (state == null)
        {
            Block block = Block.getBlockById(legacy >> 4);

            try
            {
                state = block.getStateFromMeta(legacy & 15);
            }
            catch (RuntimeException e)
            {
                state = block.getDefaultState();
            }

            STATES[legacy] = state == null ? Blocks.AIR.getDefaultState() : state;
            state = STATES[legacy];
        }

        return state;
    }

    /** The 1.12 block state number of a 1.8 block number (for block change packets). */
    static int modernId(int legacy)
    {
        int id = Block.BLOCK_STATE_IDS.get(state(legacy));
        return id < 0 ? 0 : id;
    }

    /** How many sections a bitmask has. */
    static int sections(int mask)
    {
        return Integer.bitCount(mask & 65535);
    }

    /**
     * Converts the data of one 1.8 chunk (starting at in's reader index) to 1.12 section data, and
     * leaves in's reader index after it.
     */
    static byte[] convert(PacketBuffer in, int mask, boolean full, boolean skyLight)
    {
        int count = sections(mask);
        short[][] blocks = new short[count][4096];
        byte[][] blockLight = new byte[count][2048];
        byte[][] sky = new byte[count][];

        for (int s = 0; s < count; ++s)
        {
            short[] b = blocks[s];

            for (int i = 0; i < 4096; ++i)
            {
                // little endian
                int lo = in.readUnsignedByte();
                int hi = in.readUnsignedByte();
                b[i] = (short)(hi << 8 | lo);
            }
        }

        for (int s = 0; s < count; ++s)
        {
            in.readBytes(blockLight[s]);
        }

        if (skyLight)
        {
            for (int s = 0; s < count; ++s)
            {
                sky[s] = new byte[2048];
                in.readBytes(sky[s]);
            }
        }

        byte[] biomes = null;

        if (full)
        {
            biomes = new byte[256];
            in.readBytes(biomes);
        }

        PacketBuffer out = new PacketBuffer(io.netty.buffer.Unpooled.buffer(count * 10240 + 256));

        for (int s = 0; s < count; ++s)
        {
            BlockStateContainer container = new BlockStateContainer();
            short[] b = blocks[s];

            for (int i = 0; i < 4096; ++i)
            {
                if (b[i] != 0)
                {
                    // index = y << 8 | z << 4 | x in both versions
                    container.set(i & 15, i >> 8 & 15, i >> 4 & 15, state(b[i]));
                }
            }

            container.write(out);
            out.writeBytes(blockLight[s]);

            if (skyLight)
            {
                out.writeBytes(sky[s]);
            }
        }

        if (biomes != null)
        {
            out.writeBytes(biomes);
        }

        byte[] data = new byte[out.readableBytes()];
        out.readBytes(data);
        return data;
    }

    /** How many bytes one 1.8 chunk's data takes. */
    static int size(int mask, boolean full, boolean skyLight)
    {
        return sections(mask) * (8192 + 2048 + (skyLight ? 2048 : 0)) + (full ? 256 : 0);
    }
}
