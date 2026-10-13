package net.sythix.client.eagler;

import java.io.IOException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

/**
 * Item stacks between 1.8 and 1.12. The wire format is the same (id, count, damage, NBT); what
 * changed is how potions and spawn eggs are described: 1.8 keeps the potion or mob in the damage
 * value, 1.12 in NBT. The 1.8 damage is kept in a hidden tag so the stack goes back unchanged.
 */
final class LegacyItems
{
    /** Hidden tag with the 1.8 damage value of a converted stack. */
    private static final String KEPT = "SythixLegacy";

    /** 1.8 potion number (damage & 15) -> 1.12 potion name, and whether long/strong versions exist. */
    private static final String[] POTIONS = {null, "regeneration", "swiftness", "fire_resistance", "poison", "healing", "night_vision", null, "weakness", "strength", "slowness", "leaping", "harming", "water_breathing", "invisibility", null};
    private static final boolean[] HAS_LONG = {false, true, true, true, true, false, true, false, true, true, true, true, false, true, true, false};
    private static final boolean[] HAS_STRONG = {false, true, true, false, true, true, false, false, false, true, false, true, true, false, false, false};

    private LegacyItems()
    {
    }

    /** Copies one 1.8 stack from in to out as a 1.12 stack. */
    static void toModern(PacketBuffer in, PacketBuffer out) throws IOException
    {
        short id = in.readShort();

        if (id < 0)
        {
            out.writeShort(-1);
            return;
        }

        int count = in.readByte();
        int damage = in.readShort();
        NBTTagCompound tag = in.readCompoundTag();
        int potion = Item.getIdFromItem(Items.POTIONITEM);

        if (id == potion)
        {
            boolean splash = (damage & 16384) != 0;
            String name = potionName(damage);
            tag = tag == null ? new NBTTagCompound() : tag;
            tag.setString("Potion", "minecraft:" + name);
            tag.setShort(KEPT, (short)damage);
            id = (short)(splash ? Item.getIdFromItem(Items.SPLASH_POTION) : potion);
            damage = 0;
        }
        else if (id == Item.getIdFromItem(Items.SPAWN_EGG) && damage != 0)
        {
            Class <? extends Entity > type = EntityList.getClassFromID(damage);
            ResourceLocation key = type == null ? null : EntityList.getKey(type);

            if (key != null)
            {
                tag = tag == null ? new NBTTagCompound() : tag;
                NBTTagCompound entity = new NBTTagCompound();
                entity.setString("id", key.toString());
                tag.setTag("EntityTag", entity);
                tag.setShort(KEPT, (short)damage);
                damage = 0;
            }
        }

        out.writeShort(id);
        out.writeByte(count);
        out.writeShort(damage);
        out.writeCompoundTag(tag);
    }

    /** Copies one 1.12 stack from in to out as a 1.8 stack. */
    static void toLegacy(PacketBuffer in, PacketBuffer out) throws IOException
    {
        short id = in.readShort();

        if (id < 0)
        {
            out.writeShort(-1);
            return;
        }

        int count = in.readByte();
        int damage = in.readShort();
        NBTTagCompound tag = in.readCompoundTag();
        int potion = Item.getIdFromItem(Items.POTIONITEM);
        int splash = Item.getIdFromItem(Items.SPLASH_POTION);

        if (id == potion || id == splash || id == Item.getIdFromItem(Items.LINGERING_POTION))
        {
            if (tag != null && tag.hasKey(KEPT))
            {
                damage = tag.getShort(KEPT);
            }
            else
            {
                damage = potionDamage(tag == null ? "" : tag.getString("Potion"), id != potion);
            }

            if (tag != null)
            {
                tag.removeTag("Potion");
            }

            id = (short)potion;
        }
        else if (id == Item.getIdFromItem(Items.SPAWN_EGG) && tag != null)
        {
            if (tag.hasKey(KEPT))
            {
                damage = tag.getShort(KEPT);
            }
            else if (tag.hasKey("EntityTag"))
            {
                Class <? extends Entity > type = EntityList.getClassFromName(tag.getCompoundTag("EntityTag").getString("id"));
                int entityId = type == null ? -1 : EntityList.REGISTRY.getIDForObject(type);
                damage = entityId < 0 ? 0 : entityId;
            }

            tag.removeTag("EntityTag");
        }

        if (tag != null)
        {
            tag.removeTag(KEPT);

            if (tag.hasNoTags())
            {
                tag = null;
            }
        }

        out.writeShort(id);
        out.writeByte(count);
        out.writeShort(damage);
        out.writeCompoundTag(tag);
    }

    /** The 1.12 potion type name of a 1.8 potion damage value. */
    static String potionName(int damage)
    {
        int kind = damage & 15;

        if (POTIONS[kind] == null)
        {
            if (damage == 0)
            {
                return "water";
            }

            return (damage & 63) == 16 ? "awkward" : ((damage & 63) == 32 ? "thick" : "mundane");
        }

        if ((damage & 32) != 0 && HAS_STRONG[kind])
        {
            return "strong_" + POTIONS[kind];
        }

        if ((damage & 64) != 0 && HAS_LONG[kind])
        {
            return "long_" + POTIONS[kind];
        }

        return POTIONS[kind];
    }

    /** A 1.8 damage value for a 1.12 potion name (for potions made on this side, like in creative). */
    static int potionDamage(String name, boolean splash)
    {
        String n = name.startsWith("minecraft:") ? name.substring(10) : name;
        int flags = splash ? 16384 : 8192;

        if (n.equals("water") || n.isEmpty())
        {
            return 0;
        }

        if (n.equals("awkward"))
        {
            return 16;
        }

        if (n.equals("thick"))
        {
            return 32;
        }

        if (n.startsWith("strong_"))
        {
            flags |= 32;
            n = n.substring(7);
        }
        else if (n.startsWith("long_"))
        {
            flags |= 64;
            n = n.substring(5);
        }

        for (int i = 0; i < POTIONS.length; ++i)
        {
            if (n.equals(POTIONS[i]))
            {
                return flags | i;
            }
        }

        return 8192 | 64;
    }
}
