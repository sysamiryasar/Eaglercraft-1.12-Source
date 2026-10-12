package net.sythix.modern.block;

import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityFurnace;

/**
 * Blast furnace and smoker: a furnace that cooks twice as fast, but only its own kind of items
 * (smoker: food; blast furnace: everything that is not food, like ores and tools).
 */
public class TileModernFurnace extends TileEntityFurnace
{
    private boolean smoker;

    public TileModernFurnace()
    {
    }

    public TileModernFurnace(boolean isSmoker)
    {
        this.smoker = isSmoker;
    }

    private boolean accepts(ItemStack stack)
    {
        boolean food = FurnaceRecipes.instance().getSmeltingResult(stack).getItem() instanceof ItemFood;
        return this.smoker == food;
    }

    public int getCookTime(ItemStack stack)
    {
        // Items of the wrong kind never finish
        return this.accepts(stack) ? 100 : 32767;
    }

    public String getName()
    {
        return this.hasCustomName() ? super.getName() : (this.smoker ? "container.m.smoker" : "container.m.blast_furnace");
    }

    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        this.smoker = compound.getBoolean("SythixSmoker");
    }

    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setBoolean("SythixSmoker", this.smoker);
        return compound;
    }
}
