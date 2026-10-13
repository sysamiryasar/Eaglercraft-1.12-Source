package net.sythix.client.eagler;

import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

/**
 * The game protocol between a 1.8 server (every Eaglercraft server speaks 1.8) and this 1.12
 * client. Packets from the server are rewritten into 1.12 packets, packets from the game into 1.8
 * ones. Most packets only moved to a new number; the ones that changed shape are rebuilt here.
 */
final class LegacyProtocol
{
    /** Where translated packets go. */
    interface Output
    {
        /** A 1.12 packet for the game: varint id + data. */
        void toGame(PacketBuffer packet);

        /** A 1.8 packet for the server: varint id + data. */
        void toServer(byte[] packet);
    }

    private static final EnumConnectionState PLAY = EnumConnectionState.PLAY;
    private final Output output;
    private final Map<Integer, Tracked> entities = new HashMap<Integer, Tracked>();
    private final Map<Integer, String> windows = new HashMap<Integer, String>();
    private final Set<UUID> listed = new HashSet<UUID>();
    private final byte[][] hotbar = new byte[9][];
    private int heldSlot;
    private int playerId;
    private int dimension;
    private int teleportId;
    private String resourcePackHash = "";
    private byte[] pendingAttack;
    private long lastPlacement;

    /** What the translator remembers about an entity. */
    private static final class Tracked
    {
        LegacyMetadata.Kind kind;
        final LegacyMetadata.Extra extra = new LegacyMetadata.Extra();
        int vehicle = -1;

        Tracked(LegacyMetadata.Kind kind)
        {
            this.kind = kind;
        }
    }

    LegacyProtocol(Output output)
    {
        this.output = output;
    }

    // ---- helpers ---------------------------------------------------------------------------------

    /** A new 1.12 packet of the given class. */
    private static PacketBuffer game(Class<?> packet)
    {
        PacketBuffer out = new PacketBuffer(Unpooled.buffer());
        int id = PLAY.getPacketIdOf(EnumPacketDirection.CLIENTBOUND, packet);

        if (id < 0)
        {
            throw new IllegalStateException("No 1.12 packet id for " + packet.getSimpleName());
        }

        out.writeVarInt(id);
        return out;
    }

    /** A new 1.8 packet with the given id. */
    private static PacketBuffer server(int id)
    {
        PacketBuffer out = new PacketBuffer(Unpooled.buffer());
        out.writeVarInt(id);
        return out;
    }

    private void send(PacketBuffer out)
    {
        this.output.toGame(out);
    }

    private void sendServer(PacketBuffer out)
    {
        byte[] data = new byte[out.readableBytes()];
        out.readBytes(data);

        if (this.pendingAttack != null)
        {
            byte[] attack = this.pendingAttack;
            this.pendingAttack = null;
            this.output.toServer(attack);
        }

        this.output.toServer(data);
    }

    /** Copies the rest of a packet unchanged under its 1.12 number. */
    private void same(Class<?> packet, PacketBuffer in)
    {
        PacketBuffer out = game(packet);
        out.writeBytes(in);
        this.send(out);
    }

    private void sameToServer(int id, PacketBuffer in)
    {
        PacketBuffer out = server(id);
        out.writeBytes(in);
        this.sendServer(out);
    }

    private static UUID uuid(int entityId)
    {
        return new UUID(0x5E37A1C0FFEEL, entityId);
    }

    private Tracked tracked(int id)
    {
        Tracked t = this.entities.get(id);

        if (t == null)
        {
            t = new Tracked(LegacyMetadata.Kind.OTHER);
            this.entities.put(id, t);
        }

        return t;
    }

    private static void copyItem(PacketBuffer in, PacketBuffer out) throws IOException
    {
        LegacyItems.toModern(in, out);
    }

    /** One converted 1.12 stack, as bytes. */
    private static byte[] modernItem(PacketBuffer in) throws IOException
    {
        PacketBuffer item = new PacketBuffer(Unpooled.buffer());
        LegacyItems.toModern(in, item);
        byte[] bytes = new byte[item.readableBytes()];
        item.readBytes(bytes);
        return bytes;
    }

    // ---- server -> game ----------------------------------------------------------------------------

    /** Translates one 1.8 packet from the server (varint id + data). */
    void fromServer(PacketBuffer in) throws IOException
    {
        int id = in.readVarInt();

        switch (id)
        {
            case 0x00:
                this.same(SPacketKeepAlive.class, in);
                break;

            case 0x01:
                this.joinGame(in);
                break;

            case 0x02:
                this.same(SPacketChat.class, in);
                break;

            case 0x03:
                this.same(SPacketTimeUpdate.class, in);
                break;

            case 0x04:
                this.equipment(in);
                break;

            case 0x05:
                this.same(SPacketSpawnPosition.class, in);
                break;

            case 0x06:
                this.same(SPacketUpdateHealth.class, in);
                break;

            case 0x07:
            {
                this.dimension = in.getInt(in.readerIndex());
                this.same(SPacketRespawn.class, in);
                break;
            }

            case 0x08:
                this.positionLook(in);
                break;

            case 0x09:
                this.heldSlot = in.getByte(in.readerIndex());
                this.same(SPacketHeldItemChange.class, in);
                break;

            case 0x0A:
                this.same(SPacketUseBed.class, in);
                break;

            case 0x0B:
            {
                int entity = in.readVarInt();
                int type = in.readUnsignedByte();

                // 1.8 type 3 was "eat food"; 1.12 uses 3 for an off hand swing
                if (type != 3)
                {
                    PacketBuffer out = game(SPacketAnimation.class);
                    out.writeVarInt(entity);
                    out.writeByte(type);
                    this.send(out);
                }

                break;
            }

            case 0x0C:
                this.spawnPlayer(in);
                break;

            case 0x0D:
            {
                PacketBuffer out = game(SPacketCollectItem.class);
                out.writeVarInt(in.readVarInt());
                out.writeVarInt(in.readVarInt());
                out.writeVarInt(1);
                this.send(out);
                break;
            }

            case 0x0E:
                this.spawnObject(in);
                break;

            case 0x0F:
                this.spawnMob(in);
                break;

            case 0x10:
            {
                int entity = in.readVarInt();
                PacketBuffer out = game(SPacketSpawnPainting.class);
                out.writeVarInt(entity);
                out.writeUniqueId(uuid(entity));
                out.writeString(in.readString(13));
                out.writeBlockPos(in.readBlockPos());
                out.writeByte(in.readUnsignedByte());
                this.send(out);
                break;
            }

            case 0x11:
            {
                PacketBuffer out = game(SPacketSpawnExperienceOrb.class);
                out.writeVarInt(in.readVarInt());
                out.writeDouble(in.readInt() / 32.0D);
                out.writeDouble(in.readInt() / 32.0D);
                out.writeDouble(in.readInt() / 32.0D);
                out.writeShort(in.readShort());
                this.send(out);
                break;
            }

            case 0x12:
                this.same(SPacketEntityVelocity.class, in);
                break;

            case 0x13:
            {
                int[] ids = in.readVarIntArray();

                for (int e : ids)
                {
                    this.entities.remove(e);
                }

                PacketBuffer out = game(SPacketDestroyEntities.class);
                out.writeVarIntArray(ids);
                this.send(out);
                break;
            }

            case 0x14:
                this.same(SPacketEntity.class, in);
                break;

            case 0x15:
            {
                PacketBuffer out = game(SPacketEntity.S15PacketEntityRelMove.class);
                out.writeVarInt(in.readVarInt());
                out.writeShort(in.readByte() * 128);
                out.writeShort(in.readByte() * 128);
                out.writeShort(in.readByte() * 128);
                out.writeBoolean(in.readBoolean());
                this.send(out);
                break;
            }

            case 0x16:
                this.same(SPacketEntity.S16PacketEntityLook.class, in);
                break;

            case 0x17:
            {
                PacketBuffer out = game(SPacketEntity.S17PacketEntityLookMove.class);
                out.writeVarInt(in.readVarInt());
                out.writeShort(in.readByte() * 128);
                out.writeShort(in.readByte() * 128);
                out.writeShort(in.readByte() * 128);
                out.writeByte(in.readByte());
                out.writeByte(in.readByte());
                out.writeBoolean(in.readBoolean());
                this.send(out);
                break;
            }

            case 0x18:
            {
                PacketBuffer out = game(SPacketEntityTeleport.class);
                out.writeVarInt(in.readVarInt());
                out.writeDouble(in.readInt() / 32.0D);
                out.writeDouble(in.readInt() / 32.0D);
                out.writeDouble(in.readInt() / 32.0D);
                out.writeByte(in.readByte());
                out.writeByte(in.readByte());
                out.writeBoolean(in.readBoolean());
                this.send(out);
                break;
            }

            case 0x19:
                this.same(SPacketEntityHeadLook.class, in);
                break;

            case 0x1A:
                this.same(SPacketEntityStatus.class, in);
                break;

            case 0x1B:
                this.attach(in);
                break;

            case 0x1C:
            {
                int entity = in.readVarInt();
                Tracked t = this.tracked(entity);
                List<LegacyMetadata.Entry> list = LegacyMetadata.read(in);
                PacketBuffer out = game(SPacketEntityMetadata.class);
                out.writeVarInt(entity);
                LegacyMetadata.write(list, t.kind, t.extra, out);
                this.send(out);
                break;
            }

            case 0x1D:
            {
                PacketBuffer out = game(SPacketEntityEffect.class);
                out.writeVarInt(in.readVarInt());
                out.writeByte(in.readByte());
                out.writeByte(in.readByte());
                out.writeVarInt(in.readVarInt());
                // 1.8: "hide particles"; 1.12: bit 2 = show particles
                out.writeByte(in.readBoolean() ? 0 : 2);
                this.send(out);
                break;
            }

            case 0x1E:
                this.same(SPacketRemoveEntityEffect.class, in);
                break;

            case 0x1F:
                this.same(SPacketSetExperience.class, in);
                break;

            case 0x20:
                this.same(SPacketEntityProperties.class, in);
                break;

            case 0x21:
                this.chunk(in);
                break;

            case 0x22:
            {
                PacketBuffer out = game(SPacketMultiBlockChange.class);
                out.writeInt(in.readInt());
                out.writeInt(in.readInt());
                int count = in.readVarInt();
                out.writeVarInt(count);

                for (int i = 0; i < count; ++i)
                {
                    out.writeShort(in.readShort());
                    out.writeVarInt(LegacyChunks.modernId(in.readVarInt()));
                }

                this.send(out);
                break;
            }

            case 0x23:
            {
                PacketBuffer out = game(SPacketBlockChange.class);
                out.writeBlockPos(in.readBlockPos());
                out.writeVarInt(LegacyChunks.modernId(in.readVarInt()));
                this.send(out);
                break;
            }

            case 0x24:
                this.same(SPacketBlockAction.class, in);
                break;

            case 0x25:
                this.same(SPacketBlockBreakAnim.class, in);
                break;

            case 0x26:
                this.chunkBulk(in);
                break;

            case 0x27:
                this.same(SPacketExplosion.class, in);
                break;

            case 0x28:
                this.effect(in);
                break;

            case 0x29:
                this.sound(in);
                break;

            case 0x2A:
                this.same(SPacketParticles.class, in);
                break;

            case 0x2B:
                this.same(SPacketChangeGameState.class, in);
                break;

            case 0x2C:
            {
                PacketBuffer out = game(SPacketSpawnGlobalEntity.class);
                out.writeVarInt(in.readVarInt());
                out.writeByte(in.readByte());
                out.writeDouble(in.readInt() / 32.0D);
                out.writeDouble(in.readInt() / 32.0D);
                out.writeDouble(in.readInt() / 32.0D);
                this.send(out);
                break;
            }

            case 0x2D:
                this.openWindow(in);
                break;

            case 0x2E:
            {
                this.windows.remove((int)in.getUnsignedByte(in.readerIndex()));
                this.same(SPacketCloseWindow.class, in);
                break;
            }

            case 0x2F:
                this.setSlot(in);
                break;

            case 0x30:
                this.windowItems(in);
                break;

            case 0x31:
                this.same(SPacketWindowProperty.class, in);
                break;

            case 0x32:
                this.same(SPacketConfirmTransaction.class, in);
                break;

            case 0x33:
            {
                // 1.12 has no sign packet: signs update like other block entities
                BlockPos pos = in.readBlockPos();
                NBTTagCompound tag = new NBTTagCompound();
                tag.setString("id", "minecraft:sign");
                tag.setInteger("x", pos.getX());
                tag.setInteger("y", pos.getY());
                tag.setInteger("z", pos.getZ());

                for (int i = 1; i <= 4; ++i)
                {
                    tag.setString("Text" + i, in.readString(32767));
                }

                PacketBuffer out = game(SPacketUpdateTileEntity.class);
                out.writeBlockPos(pos);
                out.writeByte(9);
                out.writeCompoundTag(tag);
                this.send(out);
                break;
            }

            case 0x34:
            {
                PacketBuffer out = game(SPacketMaps.class);
                out.writeVarInt(in.readVarInt());
                out.writeByte(in.readByte());
                // 1.12: "tracking position"
                out.writeBoolean(true);
                out.writeBytes(in);
                this.send(out);
                break;
            }

            case 0x35:
                this.same(SPacketUpdateTileEntity.class, in);
                break;

            case 0x36:
                this.same(SPacketSignEditorOpen.class, in);
                break;

            case 0x37:
                this.same(SPacketStatistics.class, in);
                break;

            case 0x38:
                this.playerList(in);
                break;

            case 0x39:
                this.same(SPacketPlayerAbilities.class, in);
                break;

            case 0x3A:
                this.same(SPacketTabComplete.class, in);
                break;

            case 0x3B:
                this.same(SPacketScoreboardObjective.class, in);
                break;

            case 0x3C:
                this.same(SPacketUpdateScore.class, in);
                break;

            case 0x3D:
                this.same(SPacketDisplayObjective.class, in);
                break;

            case 0x3E:
                this.teams(in);
                break;

            case 0x3F:
                this.pluginMessage(in);
                break;

            case 0x40:
                this.same(SPacketDisconnect.class, in);
                break;

            case 0x41:
                this.same(SPacketServerDifficulty.class, in);
                break;

            case 0x42:
                this.same(SPacketCombatEvent.class, in);
                break;

            case 0x43:
                this.same(SPacketCamera.class, in);
                break;

            case 0x44:
                this.same(SPacketWorldBorder.class, in);
                break;

            case 0x45:
                this.title(in);
                break;

            case 0x47:
                this.same(SPacketPlayerListHeaderFooter.class, in);
                break;

            case 0x48:
            {
                String url = in.readString(32767);
                this.resourcePackHash = in.readString(40);
                PacketBuffer out = game(SPacketResourcePackSend.class);
                out.writeString(url);
                out.writeString(this.resourcePackHash);
                this.send(out);
                break;
            }

            default:
                // 0x46 set compression and 0x49 entity NBT have no use here
        }
    }

    private void joinGame(PacketBuffer in)
    {
        this.playerId = in.readInt();
        int gameMode = in.readUnsignedByte();
        this.dimension = in.readByte();
        PacketBuffer out = game(SPacketJoinGame.class);
        out.writeInt(this.playerId);
        out.writeByte(gameMode);
        out.writeInt(this.dimension);
        out.writeBytes(in);
        this.send(out);
        this.entities.put(this.playerId, new Tracked(LegacyMetadata.Kind.PLAYER));

        // 1.8 has no attack cooldown: keep the 1.12 one from showing or weakening hits
        PacketBuffer speed = game(SPacketEntityProperties.class);
        speed.writeVarInt(this.playerId);
        speed.writeInt(1);
        speed.writeString("generic.attackSpeed");
        speed.writeDouble(1024.0D);
        speed.writeVarInt(0);
        this.send(speed);
    }

    private void positionLook(PacketBuffer in)
    {
        PacketBuffer out = game(SPacketPlayerPosLook.class);
        out.writeDouble(in.readDouble());
        out.writeDouble(in.readDouble());
        out.writeDouble(in.readDouble());
        out.writeFloat(in.readFloat());
        out.writeFloat(in.readFloat());
        out.writeByte(in.readUnsignedByte());
        out.writeVarInt(++this.teleportId);
        this.send(out);
    }

    private void equipment(PacketBuffer in) throws IOException
    {
        int entity = in.readVarInt();
        int slot = in.readShort();
        PacketBuffer out = game(SPacketEntityEquipment.class);
        out.writeVarInt(entity);
        // 1.8: held, boots, leggings, chestplate, helmet; 1.12: main hand, off hand, feet, legs, chest, head
        out.writeVarInt(slot == 0 ? 0 : slot + 1);
        copyItem(in, out);
        this.send(out);
    }

    private void spawnPlayer(PacketBuffer in) throws IOException
    {
        int entity = in.readVarInt();
        UUID id = in.readUniqueId();
        double x = in.readInt() / 32.0D;
        double y = in.readInt() / 32.0D;
        double z = in.readInt() / 32.0D;
        byte yaw = in.readByte();
        byte pitch = in.readByte();
        in.readShort();
        List<LegacyMetadata.Entry> list = LegacyMetadata.read(in);
        Tracked t = new Tracked(LegacyMetadata.Kind.PLAYER);
        this.entities.put(entity, t);
        boolean unlisted = !this.listed.contains(id);

        if (unlisted)
        {
            // 1.12 needs every player in the tab list before it can show them (server NPCs may not be)
            this.playerListEntry(0, id, "NPC");
        }

        PacketBuffer out = game(SPacketSpawnPlayer.class);
        out.writeVarInt(entity);
        out.writeUniqueId(id);
        out.writeDouble(x);
        out.writeDouble(y);
        out.writeDouble(z);
        out.writeByte(yaw);
        out.writeByte(pitch);
        LegacyMetadata.write(list, t.kind, t.extra, out);
        this.send(out);

        if (unlisted)
        {
            this.playerListEntry(4, id, null);
        }
    }

    private void playerListEntry(int action, UUID id, String name)
    {
        PacketBuffer out = game(SPacketPlayerListItem.class);
        out.writeVarInt(action);
        out.writeVarInt(1);
        out.writeUniqueId(id);

        if (action == 0)
        {
            out.writeString(name);
            out.writeVarInt(0);
            out.writeVarInt(0);
            out.writeVarInt(0);
            out.writeBoolean(false);
        }

        this.send(out);
    }

    private void playerList(PacketBuffer in)
    {
        // same format; only remember who is listed
        int start = in.readerIndex();
        int action = in.readVarInt();
        int count = in.readVarInt();

        try
        {
            for (int i = 0; i < count; ++i)
            {
                UUID id = in.readUniqueId();

                if (action == 0)
                {
                    this.listed.add(id);
                    in.readString(16);
                    int properties = in.readVarInt();

                    for (int p = 0; p < properties; ++p)
                    {
                        in.readString(32767);
                        in.readString(32767);

                        if (in.readBoolean())
                        {
                            in.readString(32767);
                        }
                    }

                    in.readVarInt();
                    in.readVarInt();

                    if (in.readBoolean())
                    {
                        in.readString(32767);
                    }
                }
                else if (action == 4)
                {
                    this.listed.remove(id);
                }
                else if (action == 1 || action == 2)
                {
                    in.readVarInt();
                }
                else if (action == 3 && in.readBoolean())
                {
                    in.readString(32767);
                }
            }
        }
        catch (RuntimeException e)
        {
            // still pass it on unchanged
        }

        in.readerIndex(start);
        this.same(SPacketPlayerListItem.class, in);
    }

    /** 1.8 spawn object type -> what its data looks like. */
    private static LegacyMetadata.Kind objectKind(int type)
    {
        switch (type)
        {
            case 1:
                return LegacyMetadata.Kind.BOAT;

            case 2:
                return LegacyMetadata.Kind.ITEM;

            case 10:
                return LegacyMetadata.Kind.MINECART;

            case 60:
                return LegacyMetadata.Kind.ARROW;

            case 71:
                return LegacyMetadata.Kind.ITEM_FRAME;

            case 73:
                return LegacyMetadata.Kind.POTION;

            case 76:
                return LegacyMetadata.Kind.FIREWORK;

            case 78:
                return LegacyMetadata.Kind.ARMOR_STAND;

            default:
                return LegacyMetadata.Kind.OTHER;
        }
    }

    private void spawnObject(PacketBuffer in) throws IOException
    {
        int entity = in.readVarInt();
        int type = in.readByte();
        double x = in.readInt() / 32.0D;
        double y = in.readInt() / 32.0D;
        double z = in.readInt() / 32.0D;
        byte pitch = in.readByte();
        byte yaw = in.readByte();
        int data = in.readInt();
        short vx = 0;
        short vy = 0;
        short vz = 0;

        if (data > 0)
        {
            vx = in.readShort();
            vy = in.readShort();
            vz = in.readShort();
        }

        LegacyMetadata.Kind kind = objectKind(type);
        this.entities.put(entity, new Tracked(kind));

        if (type == 78)
        {
            // 1.9+ spawns armor stands as mobs (type 30)
            PacketBuffer out = game(SPacketSpawnMob.class);
            out.writeVarInt(entity);
            out.writeUniqueId(uuid(entity));
            out.writeVarInt(30);
            out.writeDouble(x);
            out.writeDouble(y);
            out.writeDouble(z);
            out.writeByte(yaw);
            out.writeByte(pitch);
            out.writeByte(yaw);
            out.writeShort(vx);
            out.writeShort(vy);
            out.writeShort(vz);
            out.writeByte(255);
            this.send(out);
            return;
        }

        PacketBuffer out = game(SPacketSpawnObject.class);
        out.writeVarInt(entity);
        out.writeUniqueId(uuid(entity));
        out.writeByte(type);
        out.writeDouble(x);
        out.writeDouble(y);
        out.writeDouble(z);
        out.writeByte(pitch);
        out.writeByte(yaw);
        out.writeInt(type == 73 ? 0 : data);
        out.writeShort(vx);
        out.writeShort(vy);
        out.writeShort(vz);
        this.send(out);

        if (type == 73)
        {
            // 1.8 put the thrown potion in the spawn data, 1.12 in the entity data
            PacketBuffer legacy = new PacketBuffer(Unpooled.buffer());
            legacy.writeShort(373);
            legacy.writeByte(1);
            legacy.writeShort(data | 16384);
            legacy.writeByte(0);
            PacketBuffer meta = game(SPacketEntityMetadata.class);
            meta.writeVarInt(entity);
            LegacyMetadata.writeItemOnly(6, modernItem(legacy), meta);
            this.send(meta);
        }
    }

    private void spawnMob(PacketBuffer in) throws IOException
    {
        int entity = in.readVarInt();
        int type = in.readUnsignedByte();
        double x = in.readInt() / 32.0D;
        double y = in.readInt() / 32.0D;
        double z = in.readInt() / 32.0D;
        byte yaw = in.readByte();
        byte pitch = in.readByte();
        byte headPitch = in.readByte();
        short vx = in.readShort();
        short vy = in.readShort();
        short vz = in.readShort();
        List<LegacyMetadata.Entry> list = LegacyMetadata.read(in);
        LegacyMetadata.Kind kind = LegacyMetadata.Kind.MOB;
        LegacyMetadata.Entry e;

        switch (type)
        {
            case 50:
                kind = LegacyMetadata.Kind.CREEPER;
                break;

            case 51:
                kind = LegacyMetadata.Kind.SKELETON;
                e = LegacyMetadata.find(list, 13);

                if (e != null && e.asInt() == 1)
                {
                    type = 5;
                }

                break;

            case 52:
            case 59:
                kind = LegacyMetadata.Kind.SPIDER;
                break;

            case 54:
                kind = LegacyMetadata.Kind.ZOMBIE;
                e = LegacyMetadata.find(list, 13);

                if (e != null && e.asInt() == 1)
                {
                    type = 27;
                }

                break;

            case 57:
                kind = LegacyMetadata.Kind.ZOMBIE;
                break;

            case 55:
            case 62:
                kind = LegacyMetadata.Kind.SLIME;
                break;

            case 56:
                kind = LegacyMetadata.Kind.GHAST;
                break;

            case 58:
                kind = LegacyMetadata.Kind.ENDERMAN;
                break;

            case 61:
                kind = LegacyMetadata.Kind.BLAZE;
                break;

            case 64:
                kind = LegacyMetadata.Kind.WITHER;
                break;

            case 65:
                kind = LegacyMetadata.Kind.BAT;
                break;

            case 66:
                kind = LegacyMetadata.Kind.WITCH;
                break;

            case 68:
                kind = LegacyMetadata.Kind.GUARDIAN;
                e = LegacyMetadata.find(list, 16);

                if (e != null && (e.asInt() & 4) != 0)
                {
                    type = 4;
                }

                break;

            case 90:
                kind = LegacyMetadata.Kind.PIG;
                break;

            case 91:
                kind = LegacyMetadata.Kind.SHEEP;
                break;

            case 92:
            case 93:
            case 96:
                kind = LegacyMetadata.Kind.ANIMAL;
                break;

            case 95:
                kind = LegacyMetadata.Kind.WOLF;
                break;

            case 98:
                kind = LegacyMetadata.Kind.OCELOT;
                break;

            case 99:
                kind = LegacyMetadata.Kind.IRON_GOLEM;
                break;

            case 100:
                kind = LegacyMetadata.Kind.HORSE;
                e = LegacyMetadata.find(list, 19);
                int horse = e == null ? 0 : e.asInt();

                if (horse >= 1 && horse <= 4)
                {
                    // donkey, mule, zombie horse, skeleton horse became their own mobs
                    type = new int[] {100, 31, 32, 29, 28}[horse];
                    kind = LegacyMetadata.Kind.OTHER_HORSE;
                }

                break;

            case 101:
                kind = LegacyMetadata.Kind.RABBIT;
                break;

            case 120:
                kind = LegacyMetadata.Kind.VILLAGER;
                break;

            default:
        }

        Tracked t = new Tracked(kind);
        this.entities.put(entity, t);
        PacketBuffer out = game(SPacketSpawnMob.class);
        out.writeVarInt(entity);
        out.writeUniqueId(uuid(entity));
        out.writeVarInt(type);
        out.writeDouble(x);
        out.writeDouble(y);
        out.writeDouble(z);
        out.writeByte(yaw);
        out.writeByte(pitch);
        out.writeByte(headPitch);
        out.writeShort(vx);
        out.writeShort(vy);
        out.writeShort(vz);
        LegacyMetadata.write(list, kind, t.extra, out);
        this.send(out);
    }

    private void attach(PacketBuffer in)
    {
        int entity = in.readInt();
        int vehicle = in.readInt();
        boolean leash = in.readBoolean();

        if (leash)
        {
            PacketBuffer out = game(SPacketEntityAttach.class);
            out.writeInt(entity);
            out.writeInt(vehicle);
            this.send(out);
            return;
        }

        // 1.8: "entity now rides vehicle (-1: nothing)"; 1.12: "vehicle now carries these entities"
        Tracked t = this.tracked(entity);

        if (t.vehicle != -1 && t.vehicle != vehicle)
        {
            PacketBuffer out = game(SPacketSetPassengers.class);
            out.writeVarInt(t.vehicle);
            out.writeVarIntArray(new int[0]);
            this.send(out);
        }

        t.vehicle = vehicle;

        if (vehicle != -1)
        {
            PacketBuffer out = game(SPacketSetPassengers.class);
            out.writeVarInt(vehicle);
            out.writeVarIntArray(new int[] {entity});
            this.send(out);
        }
    }

    private void chunk(PacketBuffer in)
    {
        int x = in.readInt();
        int z = in.readInt();
        boolean full = in.readBoolean();
        int mask = in.readUnsignedShort();
        int size = in.readVarInt();

        if (full && mask == 0)
        {
            // 1.8 unloads a chunk with an empty full chunk
            PacketBuffer out = game(SPacketUnloadChunk.class);
            out.writeInt(x);
            out.writeInt(z);
            this.send(out);
            in.skipBytes(Math.min(size, in.readableBytes()));
            return;
        }

        boolean sky = size == LegacyChunks.size(mask, full, true);
        this.sendChunk(x, z, full, mask, LegacyChunks.convert(in, mask, full, sky));
    }

    private void chunkBulk(PacketBuffer in)
    {
        boolean sky = in.readBoolean();
        int count = in.readVarInt();
        int[] xs = new int[count];
        int[] zs = new int[count];
        int[] masks = new int[count];

        for (int i = 0; i < count; ++i)
        {
            xs[i] = in.readInt();
            zs[i] = in.readInt();
            masks[i] = in.readUnsignedShort();
        }

        for (int i = 0; i < count; ++i)
        {
            this.sendChunk(xs[i], zs[i], true, masks[i], LegacyChunks.convert(in, masks[i], true, sky));
        }
    }

    private void sendChunk(int x, int z, boolean full, int mask, byte[] data)
    {
        PacketBuffer out = game(SPacketChunkData.class);
        out.writeInt(x);
        out.writeInt(z);
        out.writeBoolean(full);
        out.writeVarInt(mask);
        out.writeVarInt(data.length);
        out.writeBytes(data);
        // block entities come in their own packets on 1.8
        out.writeVarInt(0);
        this.send(out);
    }

    /** 1.8 world event id -> 1.12 id (sounds were renumbered in 1.9). */
    private static int effectId(int id)
    {
        switch (id)
        {
            case 1003:
                return 1006;

            case 1004:
                return 1009;

            case 1005:
                return 1010;

            case 1007:
                return 1015;

            case 1008:
            case 1009:
                return 1016;

            case 1010:
                return 1019;

            case 1011:
                return 1020;

            case 1012:
                return 1021;

            case 1013:
                return 1023;

            case 1014:
                return 1024;

            case 1015:
                return 1025;

            case 1016:
                return 1026;

            case 1017:
                return 1027;

            case 1018:
                return 1028;

            case 1020:
                return 1029;

            case 1021:
                return 1030;

            case 1022:
                return 1031;

            default:
                return id;
        }
    }

    private void effect(PacketBuffer in)
    {
        int id = in.readInt();
        BlockPos pos = in.readBlockPos();
        int data = in.readInt();
        boolean global = in.readBoolean();

        if (id == 2002)
        {
            // splash potion: 1.8 sends the potion, 1.12 its color
            data = PotionUtils.getPotionColor(PotionType.getPotionTypeForName(LegacyItems.potionName(data)));
        }

        PacketBuffer out = game(SPacketEffect.class);
        out.writeInt(effectId(id));
        out.writeBlockPos(pos);
        out.writeInt(data);
        out.writeBoolean(global);
        this.send(out);
    }

    private void sound(PacketBuffer in)
    {
        String legacy = in.readString(256);
        int x = in.readInt();
        int y = in.readInt();
        int z = in.readInt();
        float volume = in.readFloat();
        int pitch = in.readUnsignedByte();
        String name = LegacySounds.name(legacy);

        if (name == null)
        {
            return;
        }

        PacketBuffer out = game(SPacketCustomSound.class);
        out.writeString(name);
        out.writeVarInt(LegacySounds.category(legacy));
        out.writeInt(x);
        out.writeInt(y);
        out.writeInt(z);
        out.writeFloat(volume);
        out.writeFloat(pitch / 63.0F);
        this.send(out);
    }

    private boolean brewing(int window)
    {
        return "minecraft:brewing_stand".equals(this.windows.get(window));
    }

    private void openWindow(PacketBuffer in)
    {
        int window = in.readUnsignedByte();
        String type = in.readString(32);
        String title = in.readString(32767);
        int slots = in.readUnsignedByte();
        this.windows.put(window, type);
        PacketBuffer out = game(SPacketOpenWindow.class);
        out.writeByte(window);
        out.writeString(type);
        out.writeString(title);
        // 1.9 added the brewing stand's fuel slot
        out.writeByte(type.equals("minecraft:brewing_stand") ? slots + 1 : slots);
        out.writeBytes(in);
        this.send(out);
    }

    private void setSlot(PacketBuffer in) throws IOException
    {
        int window = in.readByte();
        int slot = in.readShort();

        if (this.brewing(window) && slot >= 4)
        {
            ++slot;
        }

        PacketBuffer item = new PacketBuffer(Unpooled.buffer());
        int start = in.readerIndex();
        copyItem(in, item);

        if (window == 0 && slot >= 36 && slot <= 44)
        {
            this.hotbar[slot - 36] = legacyBytes(in, start);
        }

        PacketBuffer out = game(SPacketSetSlot.class);
        out.writeByte(window);
        out.writeShort(slot);
        out.writeBytes(item);
        this.send(out);
    }

    /** The 1.8 bytes of the stack that was just read (from start to in's reader index). */
    private static byte[] legacyBytes(PacketBuffer in, int start)
    {
        byte[] bytes = new byte[in.readerIndex() - start];
        in.getBytes(start, bytes);
        return bytes;
    }

    private void windowItems(PacketBuffer in) throws IOException
    {
        int window = in.readUnsignedByte();
        int count = in.readShort();
        boolean brewing = this.brewing(window);
        PacketBuffer out = game(SPacketWindowItems.class);
        out.writeByte(window);
        out.writeShort(brewing && count >= 4 ? count + 1 : count);

        for (int i = 0; i < count; ++i)
        {
            if (brewing && i == 4)
            {
                // the 1.12 fuel slot
                out.writeShort(-1);
            }

            int start = in.readerIndex();
            copyItem(in, out);

            if (window == 0 && i >= 36 && i <= 44)
            {
                this.hotbar[i - 36] = legacyBytes(in, start);
            }
        }

        this.send(out);
    }

    private void teams(PacketBuffer in)
    {
        PacketBuffer out = game(SPacketTeams.class);
        out.writeString(in.readString(16));
        int mode = in.readByte();
        out.writeByte(mode);

        if (mode == 0 || mode == 2)
        {
            out.writeString(in.readString(32));
            out.writeString(in.readString(16));
            out.writeString(in.readString(16));
            out.writeByte(in.readByte());
            out.writeString(in.readString(32));
            // 1.9 added the collision rule
            out.writeString("always");
            out.writeByte(in.readByte());
        }

        out.writeBytes(in);
        this.send(out);
    }

    private void title(PacketBuffer in)
    {
        int action = in.readVarInt();
        PacketBuffer out = game(SPacketTitle.class);
        // 1.8: title, subtitle, times, clear, reset; 1.12 put "action bar" third
        out.writeVarInt(action >= 2 ? action + 1 : action);
        out.writeBytes(in);
        this.send(out);
    }

    private void pluginMessage(PacketBuffer in) throws IOException
    {
        String channel = in.readString(20);
        PacketBuffer out = game(SPacketCustomPayload.class);
        out.writeString(channel);

        if (channel.equals("MC|BOpen"))
        {
            // 1.12 says which hand holds the book
            out.writeVarInt(0);
        }
        else if (channel.equals("MC|TrList"))
        {
            // villager trades: items changed, and 1.12 adds the use counts
            out.writeInt(in.readInt());
            int count = in.readUnsignedByte();
            out.writeByte(count);

            for (int i = 0; i < count; ++i)
            {
                copyItem(in, out);
                copyItem(in, out);
                boolean second = in.readBoolean();
                out.writeBoolean(second);

                if (second)
                {
                    copyItem(in, out);
                }

                out.writeBoolean(in.readBoolean());
                out.writeInt(0);
                out.writeInt(7);
            }
        }
        else
        {
            out.writeBytes(in);
        }

        this.send(out);
    }

    // ---- game -> server ----------------------------------------------------------------------------

    /** Translates one 1.12 packet from the game (varint id + data) into 1.8 packets for the server. */
    void fromGame(PacketBuffer in) throws IOException
    {
        int id = in.readVarInt();
        Class<?> type = PLAY.getPacketClassOf(EnumPacketDirection.SERVERBOUND, id);

        if (type == null)
        {
            return;
        }

        if (type == CPacketKeepAlive.class)
        {
            this.sameToServer(0x00, in);
        }
        else if (type == CPacketChatMessage.class)
        {
            String text = in.readString(256);
            PacketBuffer out = server(0x01);
            // 1.8 chat is at most 100 characters
            out.writeString(text.length() > 100 ? text.substring(0, 100) : text);
            this.sendServer(out);
        }
        else if (type == CPacketUseEntity.class)
        {
            this.useEntity(in);
        }
        else if (type == CPacketPlayer.class)
        {
            this.sameToServer(0x03, in);
        }
        else if (type == CPacketPlayer.Position.class)
        {
            this.sameToServer(0x04, in);
        }
        else if (type == CPacketPlayer.Rotation.class)
        {
            this.sameToServer(0x05, in);
        }
        else if (type == CPacketPlayer.PositionRotation.class)
        {
            this.sameToServer(0x06, in);
        }
        else if (type == CPacketPlayerDigging.class)
        {
            int action = in.readVarInt();

            // 6: swap hands (no off hand on 1.8)
            if (action <= 5)
            {
                PacketBuffer out = server(0x07);
                out.writeByte(action);
                out.writeBlockPos(in.readBlockPos());
                out.writeByte(in.readUnsignedByte());
                this.sendServer(out);
            }
        }
        else if (type == CPacketPlayerTryUseItemOnBlock.class)
        {
            BlockPos pos = in.readBlockPos();
            int face = in.readVarInt();
            int hand = in.readVarInt();
            float fx = in.readFloat();
            float fy = in.readFloat();
            float fz = in.readFloat();

            if (hand == 0)
            {
                this.lastPlacement = System.currentTimeMillis();
                PacketBuffer out = server(0x08);
                out.writeBlockPos(pos);
                out.writeByte(face);
                this.writeHeld(out);
                out.writeByte((int)(fx * 16.0F));
                out.writeByte((int)(fy * 16.0F));
                out.writeByte((int)(fz * 16.0F));
                this.sendServer(out);
            }
        }
        else if (type == CPacketPlayerTryUseItem.class)
        {
            int hand = in.readVarInt();

            // after a block click 1.12 also "uses" the item; 1.8 did both with the block click
            if (hand == 0 && System.currentTimeMillis() - this.lastPlacement > 40L)
            {
                PacketBuffer out = server(0x08);
                out.writeLong(-1L);
                out.writeByte(255);
                this.writeHeld(out);
                out.writeByte(0);
                out.writeByte(0);
                out.writeByte(0);
                this.sendServer(out);
            }
        }
        else if (type == CPacketHeldItemChange.class)
        {
            this.heldSlot = in.getShort(in.readerIndex());
            this.sameToServer(0x09, in);
        }
        else if (type == CPacketAnimation.class)
        {
            if (in.readVarInt() == 0)
            {
                // 1.8 clients swing before they hit; keep that order for the server
                byte[] attack = this.pendingAttack;
                this.pendingAttack = null;
                this.output.toServer(new byte[] {0x0A});

                if (attack != null)
                {
                    this.output.toServer(attack);
                }
            }
        }
        else if (type == CPacketEntityAction.class)
        {
            int entity = in.readVarInt();
            int action = in.readVarInt();
            int jump = in.readVarInt();
            // 1.12 added "stop horse jump" (6) and "start elytra" (8); "open inventory" moved to 7
            int legacy = action <= 5 ? action : (action == 7 ? 6 : -1);

            if (legacy >= 0)
            {
                PacketBuffer out = server(0x0B);
                out.writeVarInt(entity);
                out.writeVarInt(legacy);
                out.writeVarInt(jump);
                this.sendServer(out);
            }
        }
        else if (type == CPacketInput.class)
        {
            this.sameToServer(0x0C, in);
        }
        else if (type == CPacketCloseWindow.class)
        {
            this.windows.remove((int)in.getUnsignedByte(in.readerIndex()));
            this.sameToServer(0x0D, in);
        }
        else if (type == CPacketClickWindow.class)
        {
            int window = in.readUnsignedByte();
            int slot = in.readShort();

            if (this.brewing(window))
            {
                if (slot == 4)
                {
                    // the fuel slot does not exist on 1.8
                    return;
                }

                if (slot > 4)
                {
                    --slot;
                }
            }

            PacketBuffer out = server(0x0E);
            out.writeByte(window);
            out.writeShort(slot);
            out.writeByte(in.readByte());
            out.writeShort(in.readShort());
            out.writeByte(in.readVarInt());
            LegacyItems.toLegacy(in, out);
            this.sendServer(out);
        }
        else if (type == CPacketConfirmTransaction.class)
        {
            this.sameToServer(0x0F, in);
        }
        else if (type == CPacketCreativeInventoryAction.class)
        {
            PacketBuffer out = server(0x10);
            out.writeShort(in.readShort());
            LegacyItems.toLegacy(in, out);
            this.sendServer(out);
        }
        else if (type == CPacketEnchantItem.class)
        {
            this.sameToServer(0x11, in);
        }
        else if (type == CPacketUpdateSign.class)
        {
            PacketBuffer out = server(0x12);
            out.writeBlockPos(in.readBlockPos());

            for (int i = 0; i < 4; ++i)
            {
                // 1.8 sign lines are chat components
                ITextComponent line = new TextComponentString(in.readString(384));
                out.writeString(ITextComponent.Serializer.componentToJson(line));
            }

            this.sendServer(out);
        }
        else if (type == CPacketPlayerAbilities.class)
        {
            this.sameToServer(0x13, in);
        }
        else if (type == CPacketTabComplete.class)
        {
            PacketBuffer out = server(0x14);
            out.writeString(in.readString(32767));
            in.readBoolean();
            boolean hasPos = in.readBoolean();
            out.writeBoolean(hasPos);

            if (hasPos)
            {
                out.writeBlockPos(in.readBlockPos());
            }

            this.sendServer(out);
        }
        else if (type == CPacketClientSettings.class)
        {
            PacketBuffer out = server(0x15);
            out.writeString(in.readString(16));
            out.writeByte(in.readByte());
            out.writeByte(in.readVarInt());
            out.writeBoolean(in.readBoolean());
            out.writeByte(in.readUnsignedByte());
            this.sendServer(out);
        }
        else if (type == CPacketClientStatus.class)
        {
            this.sameToServer(0x16, in);
        }
        else if (type == CPacketCustomPayload.class)
        {
            this.sameToServer(0x17, in);
        }
        else if (type == CPacketSpectate.class)
        {
            this.sameToServer(0x18, in);
        }
        else if (type == CPacketResourcePackStatus.class)
        {
            PacketBuffer out = server(0x19);
            out.writeString(this.resourcePackHash);
            out.writeVarInt(in.readVarInt());
            this.sendServer(out);
        }

        // teleport confirmations, vehicle moves, boat paddles, recipe book and advancement packets
        // have no 1.8 version and are left out
    }

    private void useEntity(PacketBuffer in)
    {
        int target = in.readVarInt();
        int action = in.readVarInt();
        PacketBuffer out = server(0x02);
        out.writeVarInt(target);
        out.writeVarInt(action);

        if (action == 2)
        {
            out.writeFloat(in.readFloat());
            out.writeFloat(in.readFloat());
            out.writeFloat(in.readFloat());
        }

        if ((action == 0 || action == 2) && in.readVarInt() != 0)
        {
            // off hand: 1.8 has none (and the main hand already sent this)
            return;
        }

        if (action == 1)
        {
            // hold the hit until the swing that follows it, see CPacketAnimation
            if (this.pendingAttack != null)
            {
                this.output.toServer(this.pendingAttack);
            }

            byte[] data = new byte[out.readableBytes()];
            out.readBytes(data);
            this.pendingAttack = data;
            return;
        }

        this.sendServer(out);
    }

    private void writeHeld(PacketBuffer out)
    {
        byte[] held = this.heldSlot >= 0 && this.heldSlot < 9 ? this.hotbar[this.heldSlot] : null;

        if (held == null)
        {
            out.writeShort(-1);
        }
        else
        {
            out.writeBytes(held);
        }
    }

    /** Sends a hit that is still waiting for its swing (called once per frame). */
    void flush()
    {
        if (this.pendingAttack != null)
        {
            byte[] attack = this.pendingAttack;
            this.pendingAttack = null;
            this.output.toServer(attack);
        }
    }
}
