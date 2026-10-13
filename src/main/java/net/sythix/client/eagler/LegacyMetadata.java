package net.sythix.client.eagler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.PacketBuffer;

/**
 * Entity data (health, flags, held items, mob details) between 1.8 and 1.12. Both send a list of
 * numbered values, but 1.12 renumbered them and changed some types, so every value is mapped by
 * what kind of entity it belongs to. Values without a 1.12 counterpart are left out.
 */
final class LegacyMetadata
{
    // 1.12 DataSerializers ids
    private static final int BYTE = 0;
    private static final int VARINT = 1;
    private static final int FLOAT = 2;
    private static final int STRING = 3;
    private static final int ITEM = 5;
    private static final int BOOLEAN = 6;
    private static final int ROTATIONS = 7;
    private static final int BLOCK_STATE = 12;

    /** What an entity is, as far as its data goes. */
    enum Kind
    {
        OTHER, PLAYER, MOB, ANIMAL, PIG, SHEEP, VILLAGER, RABBIT, WOLF, OCELOT, HORSE, OTHER_HORSE, CREEPER, ZOMBIE, SKELETON, SPIDER, ENDERMAN, GHAST, SLIME, BLAZE, WITCH, IRON_GOLEM, BAT, GUARDIAN, WITHER, ARMOR_STAND, ITEM, ITEM_FRAME, FIREWORK, ARROW, MINECART, BOAT, POTION;

        boolean living()
        {
            return this.ordinal() >= PLAYER.ordinal() && this.ordinal() <= ARMOR_STAND.ordinal();
        }

        boolean ageable()
        {
            return this == ANIMAL || this == PIG || this == SHEEP || this == VILLAGER || this == RABBIT || this == WOLF || this == OCELOT || this == HORSE || this == OTHER_HORSE;
        }

        /** A mob (EntityLiving), which has AI flags; players and armor stands do not. */
        boolean mob()
        {
            return this.living() && this != PLAYER && this != ARMOR_STAND;
        }
    }

    /** One 1.8 value. */
    static final class Entry
    {
        final int index;
        final int type;
        final Object value;

        Entry(int index, int type, Object value)
        {
            this.index = index;
            this.type = type;
            this.value = value;
        }

        int asInt()
        {
            return this.value instanceof Number ? ((Number)this.value).intValue() : 0;
        }

        float asFloat()
        {
            return this.value instanceof Number ? ((Number)this.value).floatValue() : 0.0F;
        }
    }

    /** Per-entity values that 1.12 sends together (an enderman's carried block and its data). */
    static final class Extra
    {
        int carriedBlock;
        int carriedData;
    }

    private LegacyMetadata()
    {
    }

    /** Reads a 1.8 entity data list; item values are converted to 1.12 stacks right away. */
    static List<Entry> read(PacketBuffer in) throws IOException
    {
        List<Entry> list = new ArrayList<Entry>();

        while (in.isReadable())
        {
            int b = in.readUnsignedByte();

            if (b == 127)
            {
                break;
            }

            int type = (b & 224) >> 5;
            int index = b & 31;
            Object value;

            switch (type)
            {
                case 0:
                    value = Byte.valueOf(in.readByte());
                    break;

                case 1:
                    value = Short.valueOf(in.readShort());
                    break;

                case 2:
                    value = Integer.valueOf(in.readInt());
                    break;

                case 3:
                    value = Float.valueOf(in.readFloat());
                    break;

                case 4:
                    value = in.readString(32767);
                    break;

                case 5:
                    PacketBuffer item = new PacketBuffer(io.netty.buffer.Unpooled.buffer());
                    LegacyItems.toModern(in, item);
                    byte[] bytes = new byte[item.readableBytes()];
                    item.readBytes(bytes);
                    value = bytes;
                    break;

                case 6:
                    value = new int[] {in.readInt(), in.readInt(), in.readInt()};
                    break;

                case 7:
                    value = new float[] {in.readFloat(), in.readFloat(), in.readFloat()};
                    break;

                default:
                    throw new IOException("Unknown 1.8 entity data type " + type);
            }

            list.add(new Entry(index, type, value));
        }

        return list;
    }

    /** Writes the 1.12 version of a 1.8 list, ending with the 1.12 end marker. */
    static void write(List<Entry> list, Kind kind, Extra extra, PacketBuffer out)
    {
        for (Entry e : list)
        {
            convert(e, kind, extra, out);
        }

        out.writeByte(255);
    }

    private static void convert(Entry e, Kind kind, Extra extra, PacketBuffer out)
    {
        int i = e.index;

        // every entity
        switch (i)
        {
            case 0:
                int flags = e.asInt() & 255;

                if (kind.living())
                {
                    // 1.8 kept "using an item" (eating, drawing a bow, blocking) in the flags
                    byteValue(out, 6, (flags & 16) != 0 ? 1 : 0);
                }

                byteValue(out, 0, flags & ~16);
                return;

            case 1:
                varint(out, 1, e.asInt());
                return;

            case 2:
                if (e.value instanceof String)
                {
                    out.writeByte(2);
                    out.writeVarInt(STRING);
                    out.writeString((String)e.value);
                }

                return;

            case 3:
                bool(out, 3, e.asInt() != 0);
                return;

            case 4:
                bool(out, 4, e.asInt() != 0);
                return;

            default:
        }

        if (kind.living())
        {
            switch (i)
            {
                case 6:
                    floatValue(out, 7, e.asFloat());
                    return;

                case 7:
                    varint(out, 8, e.asInt());
                    return;

                case 8:
                    bool(out, 9, e.asInt() != 0);
                    return;

                case 9:
                    varint(out, 10, e.asInt());
                    return;

                case 15:
                    if (kind.mob())
                    {
                        byteValue(out, 11, e.asInt());
                    }

                    return;

                default:
            }
        }

        if (kind.ageable() && i == 12)
        {
            bool(out, 12, e.asInt() < 0);
            return;
        }

        switch (kind)
        {
            case PLAYER:
                if (i == 10)
                {
                    byteValue(out, 13, e.asInt());
                }
                else if (i == 17)
                {
                    floatValue(out, 11, e.asFloat());
                }
                else if (i == 18)
                {
                    varint(out, 12, e.asInt());
                }

                break;

            case PIG:
                if (i == 16)
                {
                    bool(out, 13, e.asInt() != 0);
                }

                break;

            case SHEEP:
                if (i == 16)
                {
                    byteValue(out, 13, e.asInt());
                }

                break;

            case VILLAGER:
                if (i == 16)
                {
                    varint(out, 13, e.asInt());
                }

                break;

            case RABBIT:
                if (i == 18)
                {
                    varint(out, 13, e.asInt());
                }

                break;

            case WOLF:
            case OCELOT:
                if (i == 16)
                {
                    byteValue(out, 13, e.asInt());
                }
                else if (kind == Kind.OCELOT && i == 18)
                {
                    varint(out, 15, e.asInt());
                }
                else if (kind == Kind.WOLF && i == 18)
                {
                    floatValue(out, 15, e.asFloat());
                }
                else if (kind == Kind.WOLF && i == 19)
                {
                    bool(out, 16, e.asInt() != 0);
                }
                else if (kind == Kind.WOLF && i == 20)
                {
                    varint(out, 17, e.asInt());
                }

                break;

            case HORSE:
            case OTHER_HORSE:
                if (i == 16)
                {
                    byteValue(out, 13, e.asInt());
                }
                else if (kind == Kind.HORSE && i == 20)
                {
                    varint(out, 15, e.asInt());
                }
                else if (kind == Kind.HORSE && i == 22)
                {
                    varint(out, 16, e.asInt());
                }

                break;

            case CREEPER:
                if (i == 16)
                {
                    varint(out, 12, e.asInt());
                }
                else if (i == 17)
                {
                    bool(out, 13, e.asInt() != 0);
                }

                break;

            case ZOMBIE:
                if (i == 12)
                {
                    bool(out, 12, e.asInt() != 0);
                }

                break;

            case SPIDER:
            case BLAZE:
            case IRON_GOLEM:
            case BAT:
                if (i == 16)
                {
                    byteValue(out, 12, e.asInt());
                }

                break;

            case ENDERMAN:
                if (i == 16 || i == 17)
                {
                    if (i == 16)
                    {
                        extra.carriedBlock = e.asInt();
                    }
                    else
                    {
                        extra.carriedData = e.asInt();
                    }

                    out.writeByte(12);
                    out.writeVarInt(BLOCK_STATE);
                    out.writeVarInt(extra.carriedBlock <= 0 ? 0 : (extra.carriedBlock & 4095) | (extra.carriedData & 15) << 12);
                }
                else if (i == 18)
                {
                    bool(out, 13, e.asInt() != 0);
                }

                break;

            case GHAST:
                if (i == 16)
                {
                    bool(out, 12, e.asInt() != 0);
                }

                break;

            case SLIME:
                if (i == 16)
                {
                    varint(out, 12, e.asInt());
                }

                break;

            case WITCH:
                if (i == 21)
                {
                    bool(out, 12, e.asInt() != 0);
                }

                break;

            case GUARDIAN:
                if (i == 16)
                {
                    bool(out, 12, (e.asInt() & 2) != 0);
                }
                else if (i == 17)
                {
                    varint(out, 13, e.asInt());
                }

                break;

            case WITHER:
                if (i >= 17 && i <= 20)
                {
                    varint(out, i - 5, e.asInt());
                }

                break;

            case ARMOR_STAND:
                if (i == 10)
                {
                    byteValue(out, 11, e.asInt());
                }
                else if (i >= 11 && i <= 16 && e.value instanceof float[])
                {
                    float[] r = (float[])e.value;
                    out.writeByte(i + 1);
                    out.writeVarInt(ROTATIONS);
                    out.writeFloat(r[0]);
                    out.writeFloat(r[1]);
                    out.writeFloat(r[2]);
                }

                break;

            case ITEM:
                if (i == 10)
                {
                    item(out, 6, e);
                }

                break;

            case ITEM_FRAME:
                if (i == 8)
                {
                    item(out, 6, e);
                }
                else if (i == 9)
                {
                    varint(out, 7, e.asInt());
                }

                break;

            case FIREWORK:
                if (i == 8)
                {
                    item(out, 6, e);
                }

                break;

            case ARROW:
                if (i == 16)
                {
                    byteValue(out, 6, e.asInt());
                }

                break;

            case MINECART:
                if (i >= 17 && i <= 21)
                {
                    if (i == 19)
                    {
                        floatValue(out, 8, e.asFloat());
                    }
                    else
                    {
                        varint(out, i - 11, e.asInt());
                    }
                }
                else if (i == 22)
                {
                    bool(out, 11, e.asInt() != 0);
                }

                break;

            case BOAT:
                if (i == 17 || i == 18)
                {
                    varint(out, i - 11, e.asInt());
                }
                else if (i == 19)
                {
                    floatValue(out, 8, e.asFloat());
                }

                break;

            default:
        }
    }

    /** The value at a 1.8 index, or null. */
    static Entry find(List<Entry> list, int index)
    {
        for (Entry e : list)
        {
            if (e.index == index)
            {
                return e;
            }
        }

        return null;
    }

    /** A 1.12 list with only a held item (thrown potions: 1.8 put the potion in the spawn data). */
    static void writeItemOnly(int index, byte[] item, PacketBuffer out)
    {
        out.writeByte(index);
        out.writeVarInt(ITEM);
        out.writeBytes(item);
        out.writeByte(255);
    }

    private static void item(PacketBuffer out, int index, Entry e)
    {
        if (e.value instanceof byte[])
        {
            out.writeByte(index);
            out.writeVarInt(ITEM);
            out.writeBytes((byte[])e.value);
        }
    }

    private static void byteValue(PacketBuffer out, int index, int value)
    {
        out.writeByte(index);
        out.writeVarInt(BYTE);
        out.writeByte(value);
    }

    private static void varint(PacketBuffer out, int index, int value)
    {
        out.writeByte(index);
        out.writeVarInt(VARINT);
        out.writeVarInt(value);
    }

    private static void floatValue(PacketBuffer out, int index, float value)
    {
        out.writeByte(index);
        out.writeVarInt(FLOAT);
        out.writeFloat(value);
    }

    private static void bool(PacketBuffer out, int index, boolean value)
    {
        out.writeByte(index);
        out.writeVarInt(BOOLEAN);
        out.writeBoolean(value);
    }
}
