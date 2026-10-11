package net.sythix.client.module.inventory;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.Chunk;
import net.sythix.client.module.Category;
import net.sythix.client.module.hud.TextHudModule;
import net.sythix.client.setting.BooleanSetting;

/**
 * #26 Block, entity, biome, light, time and chunk information that the client already has.
 */
public class WorldInformation extends TextHudModule
{
    private final BooleanSetting block;
    private final BooleanSetting entity;
    private final BooleanSetting biome;
    private final BooleanSetting light;
    private final BooleanSetting time;
    private final BooleanSetting day;
    private final BooleanSetting chunk;

    public WorldInformation()
    {
        super("World Information", "Looked-at block and entity, biome, light, time of day and chunk.", Category.INVENTORY, false, 0.995D, 0.02D);
        this.block = this.add(new BooleanSetting("Looked-at Block", "Name and position of the block you look at.", true));
        this.entity = this.add(new BooleanSetting("Looked-at Entity", "Name (and health) of the entity you look at.", true));
        this.biome = this.add(new BooleanSetting("Biome", "Biome you are in.", true));
        this.light = this.add(new BooleanSetting("Light Level", "Block light where you stand (mobs spawn at 7 or less).", false));
        this.time = this.add(new BooleanSetting("Time of Day", "In-game clock.", false));
        this.day = this.add(new BooleanSetting("Day", "In-game day number.", false));
        this.chunk = this.add(new BooleanSetting("Chunk", "Chunk coordinates.", false));
    }

    protected List<String[]> getLines()
    {
        List<String[]> lines = new ArrayList<String[]>();
        Entity viewer = this.mc.getRenderViewEntity();

        if (viewer == null || this.mc.world == null)
        {
            lines.add(new String[] {"Block", "Grass Block"});
            lines.add(new String[] {"Biome", "Plains"});
            return lines;
        }

        BlockPos feet = new BlockPos(viewer.posX, viewer.getEntityBoundingBox().minY, viewer.posZ);
        RayTraceResult hit = this.mc.objectMouseOver;

        if (this.block.isOn())
        {
            if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK)
            {
                IBlockState state = this.mc.world.getBlockState(hit.getBlockPos());
                BlockPos p = hit.getBlockPos();
                lines.add(new String[] {"Block", state.getBlock().getLocalizedName() + " (" + p.getX() + ", " + p.getY() + ", " + p.getZ() + ")"});
            }
            else
            {
                lines.add(new String[] {"Block", "-"});
            }
        }

        if (this.entity.isOn() && this.mc.pointedEntity != null)
        {
            Entity e = this.mc.pointedEntity;
            String text = e.getName();

            if (e instanceof EntityLivingBase)
            {
                text = text + String.format(" (%.0f HP)", ((EntityLivingBase)e).getHealth());
            }

            lines.add(new String[] {"Entity", text});
        }

        if (this.biome.isOn())
        {
            lines.add(new String[] {"Biome", this.mc.world.getBiome(feet).getBiomeName()});
        }

        if (this.light.isOn())
        {
            Chunk c = this.mc.world.getChunkFromBlockCoords(feet);
            lines.add(new String[] {"Light", c.getLightFor(EnumSkyBlock.BLOCK, feet) + " (sky " + c.getLightFor(EnumSkyBlock.SKY, feet) + ")"});
        }

        if (this.time.isOn())
        {
            long t = (this.mc.world.getWorldTime() + 6000L) % 24000L;
            lines.add(new String[] {"Time", String.format("%02d:%02d", t / 1000L, t % 1000L * 60L / 1000L)});
        }

        if (this.day.isOn())
        {
            lines.add(new String[] {"Day", String.valueOf(this.mc.world.getWorldTime() / 24000L + 1L)});
        }

        if (this.chunk.isOn())
        {
            lines.add(new String[] {"Chunk", (feet.getX() >> 4) + ", " + (feet.getZ() >> 4) + " (in " + (feet.getX() & 15) + ", " + (feet.getZ() & 15) + ")"});
        }

        if (lines.isEmpty())
        {
            lines.add(new String[] {"World", "-"});
        }

        return lines;
    }
}
