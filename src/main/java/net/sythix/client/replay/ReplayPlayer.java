package net.sythix.client.replay;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.*;
import net.minecraft.util.EnumHand;
import net.minecraft.world.GameType;
import net.sythix.client.core.Notifications;
import net.sythix.client.ui.SythixTheme;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Plays a replay back: the recorded server packets are fed into a local connection, so the world,
 * players, mobs, chat and sounds come back exactly as they were. You watch as a free camera
 * (spectator); your own recorded player is shown as a separate figure.
 */
public final class ReplayPlayer
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int AVATAR_ID = -7351;
    private static final EntityEquipmentSlot[] SLOTS = EntityEquipmentSlot.values();
    private static final float[] SPEEDS = new float[] {0.25F, 0.5F, 1.0F, 2.0F, 4.0F, 8.0F};

    private static File file;
    private static DataInputStream in;
    private static JsonObject meta;
    private static long duration;
    private static List<Long> markers = new ArrayList<Long>();
    private static NetworkManager network;
    private static NetHandlerPlayClient handler;
    private static boolean active;
    private static boolean started;
    private static boolean paused;
    private static boolean ended;
    private static int speedIndex = 2;
    private static double time;
    private static long seekTarget = -1L;
    private static int ownId = Integer.MIN_VALUE;
    private static boolean cameraPlaced;
    private static boolean cameraNearAvatar;
    private static EntityOtherPlayerMP avatar;
    private static ItemStack[] equipment;
    // Next entry, read ahead
    private static int nextTime;
    private static int nextId;
    private static byte[] nextData;

    private ReplayPlayer()
    {
    }

    public static boolean isActive()
    {
        return active;
    }

    public static boolean isPaused()
    {
        return paused || ended;
    }

    public static long getTime()
    {
        return (long)time;
    }

    public static long getDuration()
    {
        return duration;
    }

    public static float getSpeed()
    {
        return SPEEDS[speedIndex];
    }

    public static List<Long> getMarkers()
    {
        return markers;
    }

    /** Opens a replay (leaving the current world first). */
    public static void play(File replay)
    {
        play(replay, 0L);
    }

    private static void play(File replay, long startAt)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.world != null)
        {
            mc.world.sendQuittingDisconnectingPacket();
            mc.loadWorld((WorldClient)null);
        }

        close();

        try
        {
            in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(replay), 65536), 65536));

            if (!ReplayFormat.MAGIC.equals(in.readUTF()))
            {
                throw new IllegalStateException("not a Sythix replay");
            }

            int version = in.readInt();

            if (version > ReplayFormat.VERSION)
            {
                throw new IllegalStateException("made by a newer Sythix version");
            }

            meta = new JsonParser().parse(in.readUTF()).getAsJsonObject();
        }
        catch (Exception e)
        {
            close();
            Notifications.post("Replay failed", "Could not open " + replay.getName() + ": " + e.getMessage(), Notifications.Kind.ERROR);
            return;
        }

        file = replay;
        JsonObject summary = ReplayFormat.summary(replay);
        duration = summary.has("duration") ? summary.get("duration").getAsLong() : 0L;
        markers = new ArrayList<Long>();

        if (summary.has("markers"))
        {
            summary.getAsJsonArray("markers").forEach(m -> markers.add(Long.valueOf(m.getAsLong())));
        }

        // A local connection that goes nowhere: everything the game "sends" is dropped
        network = new NetworkManager(EnumPacketDirection.CLIENTBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(network);
        network.setConnectionState(EnumConnectionState.PLAY);
        handler = new NetHandlerPlayClient(mc, null, network, mc.getSession().getProfile());
        network.setNetHandler(handler);
        mc.setNetworkManagerSythix(network);
        active = true;
        started = false;
        paused = false;
        ended = false;
        speedIndex = 2;
        time = 0.0D;
        seekTarget = startAt > 0L ? startAt : -1L;
        ownId = Integer.MIN_VALUE;
        cameraPlaced = false;
        cameraNearAvatar = startAt > 0L;
        avatar = null;
        equipment = null;
        nextData = null;
        readNext();
        LOGGER.info("[Sythix] Playing replay {}", replay.getName());
    }

    private static void readNext()
    {
        try
        {
            nextTime = in.readInt();
            nextId = in.readInt();
            int length = in.readInt();

            if (length < 0 || length > 64 * 1024 * 1024)
            {
                throw new IllegalStateException("bad entry");
            }

            nextData = new byte[length];
            in.readFully(nextData);
        }
        catch (EOFException e)
        {
            nextData = null;
        }
        catch (Exception e)
        {
            // A replay cut off by a crash still plays up to that point
            LOGGER.warn("[Sythix] Replay ends early: {}", e.toString());
            nextData = null;
        }

        if (nextData == null)
        {
            duration = Math.max(duration, (long)time);
        }
    }

    private static void close()
    {
        try
        {
            if (in != null)
            {
                in.close();
            }
        }
        catch (Exception ignored)
        {
        }

        in = null;
        active = false;
        handler = null;
        network = null;
        avatar = null;
    }

    /** Leaves the replay and goes back to the menu. */
    public static void exit()
    {
        Minecraft mc = Minecraft.getMinecraft();
        close();

        if (mc.world != null)
        {
            mc.loadWorld((WorldClient)null);
        }

        mc.displayGuiScreen(new net.minecraft.client.gui.GuiMainMenu());
    }

    /** Start of every client tick. */
    public static void tick()
    {
        if (!active)
        {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();

        // The player left with the pause menu (or the world closed)
        if (started && mc.world == null)
        {
            close();
            return;
        }

        if (seekTarget >= 0L)
        {
            // Fast-forward in slices so the game keeps drawing (and can show progress)
            long budget = System.nanoTime() + 40000000L;

            while (nextData != null && nextTime <= seekTarget && System.nanoTime() < budget)
            {
                processNext(mc);
            }

            time = Math.max(time, Math.min(seekTarget, nextData == null ? duration : nextTime));

            if (nextData == null || nextTime > seekTarget)
            {
                time = seekTarget;
                seekTarget = -1L;
            }

            return;
        }

        if (!paused && !ended)
        {
            time += 50.0D * (double)getSpeed();
        }

        while (nextData != null && nextTime <= time)
        {
            processNext(mc);
        }

        if (nextData == null && !ended)
        {
            ended = true;
            Notifications.post("Replay finished", "Press the back key to rewind, or Esc > Disconnect to leave.", Notifications.Kind.INFO);
        }

        keepCamera(mc);
    }

    private static void processNext(Minecraft mc)
    {
        int id = nextId;
        byte[] data = nextData;
        readNext();

        try
        {
            if (id == ReplayFormat.SELF)
            {
                self(mc, new PacketBuffer(Unpooled.wrappedBuffer(data)));
            }
            else if (id == ReplayFormat.EQUIPMENT)
            {
                PacketBuffer buf = new PacketBuffer(Unpooled.wrappedBuffer(data));
                equipment = new ItemStack[SLOTS.length];

                for (int i = 0; i < SLOTS.length; ++i)
                {
                    equipment[i] = buf.readItemStack();
                }

                applyEquipment();
            }
            else if (id >= 0)
            {
                Packet<?> packet = EnumConnectionState.PLAY.getPacket(EnumPacketDirection.CLIENTBOUND, id);

                if (packet == null)
                {
                    return;
                }

                packet.readPacketData(new PacketBuffer(Unpooled.wrappedBuffer(data)));

                if (allow(mc, packet))
                {
                    ((Packet<INetHandler>)packet).processPacket(handler);
                    after(mc, packet);
                }
            }
        }
        catch (Throwable t)
        {
            // One bad packet must not end the replay
            LOGGER.debug("[Sythix] Replay entry {} failed: {}", Integer.valueOf(id), t.toString());
        }
    }

    /** Packets that would change the camera, open menus or show your old health are left out. */
    private static boolean allow(Minecraft mc, Packet<?> p)
    {
        if (p instanceof SPacketJoinGame)
        {
            ownId = ((SPacketJoinGame)p).getPlayerId();
            return true;
        }

        if (p instanceof SPacketPlayerPosLook)
        {
            // Only the first one, to place the camera where the recording started
            boolean first = !cameraPlaced;
            cameraPlaced = true;
            return first;
        }

        if (p instanceof SPacketChangeGameState)
        {
            int state = ((SPacketChangeGameState)p).getGameState();
            return state != 3 && state != 4;
        }

        if (p instanceof SPacketUpdateHealth || p instanceof SPacketCombatEvent || p instanceof SPacketPlayerAbilities || p instanceof SPacketSetSlot || p instanceof SPacketWindowItems || p instanceof SPacketOpenWindow || p instanceof SPacketCloseWindow || p instanceof SPacketSignEditorOpen || p instanceof SPacketCamera || p instanceof SPacketHeldItemChange || p instanceof SPacketResourcePackSend || p instanceof SPacketCustomPayload || p instanceof SPacketKeepAlive || p instanceof SPacketDisconnect || p instanceof SPacketCooldown || p instanceof SPacketRecipeBook || p instanceof SPacketAdvancementInfo || p instanceof SPacketStatistics || p instanceof SPacketMoveVehicle || p instanceof SPacketSelectAdvancementsTab || p instanceof SPacketWindowProperty || p instanceof SPacketConfirmTransaction || p instanceof SPacketSetExperience || p instanceof SPacketUseBed)
        {
            return false;
        }

        if (mc.world == null)
        {
            return true;
        }

        // Things that happened to your own player belong to the recorded figure, not the camera
        if (p instanceof SPacketEntityVelocity)
        {
            return ((SPacketEntityVelocity)p).getEntityID() != ownId;
        }

        if (p instanceof SPacketEntityMetadata)
        {
            return ((SPacketEntityMetadata)p).getEntityId() != ownId;
        }

        if (p instanceof SPacketEntityProperties)
        {
            return ((SPacketEntityProperties)p).getEntityId() != ownId;
        }

        if (p instanceof SPacketEntityEffect)
        {
            return ((SPacketEntityEffect)p).getEntityId() != ownId;
        }

        if (p instanceof SPacketRemoveEntityEffect)
        {
            return ((SPacketRemoveEntityEffect)p).getEntity(mc.world) != mc.player;
        }

        if (p instanceof SPacketEntityStatus)
        {
            return ((SPacketEntityStatus)p).getEntity(mc.world) != mc.player;
        }

        if (p instanceof SPacketSetPassengers)
        {
            return ((SPacketSetPassengers)p).getEntityId() != ownId;
        }

        return true;
    }

    private static void after(Minecraft mc, Packet<?> p)
    {
        if (p instanceof SPacketJoinGame || p instanceof SPacketRespawn)
        {
            started = true;
            avatar = null;
            makeCamera(mc);
        }
    }

    private static void makeCamera(Minecraft mc)
    {
        if (mc.playerController != null)
        {
            mc.playerController.setGameType(GameType.SPECTATOR);
        }

        if (mc.player != null)
        {
            mc.player.capabilities.allowFlying = true;
            mc.player.capabilities.isFlying = true;
            mc.player.capabilities.disableDamage = true;
        }
    }

    private static void keepCamera(Minecraft mc)
    {
        if (mc.player != null && mc.playerController != null && !mc.playerController.isSpectator())
        {
            makeCamera(mc);
        }
    }

    /** Your recorded player: a separate figure driven by the SELF records. */
    private static void self(Minecraft mc, PacketBuffer buf)
    {
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        float yaw = buf.readFloat();
        float pitch = buf.readFloat();
        float headYaw = buf.readFloat();
        float bodyYaw = buf.readFloat();
        int flags = buf.readByte();

        if (mc.world == null)
        {
            return;
        }

        if (avatar == null || avatar.world != mc.world)
        {
            String name = meta.has("name") ? meta.get("name").getAsString() : "Player";
            UUID uuid;

            try
            {
                uuid = UUID.fromString(meta.get("uuid").getAsString());
            }
            catch (Exception e)
            {
                uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            avatar = new EntityOtherPlayerMP(mc.world, new GameProfile(uuid, name));
            avatar.setPositionAndRotation(x, y, z, yaw, pitch);
            mc.world.addEntityToWorld(AVATAR_ID, avatar);
            applyEquipment();

            // Start the camera a few blocks behind and above your recorded player, looking at it
            if (!cameraNearAvatar && mc.player != null)
            {
                cameraNearAvatar = true;
                double rad = Math.toRadians((double)yaw);
                mc.player.setPositionAndRotation(x + Math.sin(rad) * 4.0D, y + 2.5D, z - Math.cos(rad) * 4.0D, yaw, 25.0F);
            }
        }

        avatar.setPositionAndRotationDirect(x, y, z, yaw, pitch, 3, false);
        avatar.setRotationYawHead(headYaw);
        avatar.renderYawOffset = bodyYaw;
        avatar.setSneaking((flags & 1) != 0);
        avatar.setSprinting((flags & 2) != 0);
        avatar.setInvisible((flags & 32) != 0);

        if ((flags & 4) != 0)
        {
            avatar.swingArm(EnumHand.MAIN_HAND);
        }

        if ((flags & 8) != 0)
        {
            avatar.performHurtAnimation();
        }
    }

    private static void applyEquipment()
    {
        if (avatar != null && equipment != null)
        {
            for (int i = 0; i < SLOTS.length; ++i)
            {
                avatar.setItemStackToSlot(SLOTS[i], equipment[i].copy());
            }
        }
    }

    // ---- Controls ---------------------------------------------------------------------------------

    public static void togglePause()
    {
        if (ended)
        {
            return;
        }

        paused = !paused;
    }

    public static void faster()
    {
        speedIndex = Math.min(SPEEDS.length - 1, speedIndex + 1);
    }

    public static void slower()
    {
        speedIndex = Math.max(0, speedIndex - 1);
    }

    /** Jumps to a time; going back restarts the replay and fast-forwards. */
    public static void seek(long target)
    {
        if (!active)
        {
            return;
        }

        target = Math.max(0L, duration > 0L ? Math.min(duration, target) : target);

        if (target < (long)time)
        {
            boolean wasPaused = paused;
            play(file, Math.max(1L, target));
            paused = wasPaused;
        }
        else
        {
            seekTarget = target;
            ended = false;
        }
    }

    public static void nextMarker()
    {
        for (Long m : markers)
        {
            if (m.longValue() > (long)time + 500L)
            {
                seek(m.longValue());
                return;
            }
        }
    }

    // ---- On-screen controls -----------------------------------------------------------------------

    public static void renderOverlay(ScaledResolution res, String keysHint)
    {
        if (!active)
        {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc.fontRenderer;
        int w = res.getScaledWidth();
        // Top centre, so chat and the hotbar area stay visible
        int barWidth = Math.min(w - 40, Math.max(260, font.getStringWidth(keysHint) + 4));
        int left = (w - barWidth) / 2;
        int right = left + barWidth;
        int top = 20;
        SythixTheme.drawRect((float)(left - 6), (float)(top - 16), (float)(right + 6), (float)(top + 20), 0xB00B0B0E);
        long total = Math.max(1L, Math.max(duration, (long)time));
        float progress = Math.min(1.0F, (float)time / (float)total);
        SythixTheme.drawRect((float)left, (float)top, (float)right, (float)(top + 4), 0xFF2A2A33);
        SythixTheme.drawGradientH((float)left, (float)top, (float)left + (float)(right - left) * progress, (float)(top + 4), SythixTheme.RED, SythixTheme.BLUE);

        for (Long m : markers)
        {
            float mx = (float)left + (float)(right - left) * Math.min(1.0F, (float)m.longValue() / (float)total);
            SythixTheme.drawRect(mx - 1.0F, (float)(top - 2), mx + 1.0F, (float)(top + 6), 0xFFFFD020);
        }

        String state = seekTarget >= 0L ? "Seeking..." : (ended ? "Ended" : (paused ? "Paused" : "Playing"));
        String text = state + "  " + ReplayFormat.time((long)time) + " / " + ReplayFormat.time(total) + "  " + (getSpeed() == (int)getSpeed() ? String.valueOf((int)getSpeed()) : String.valueOf(getSpeed())) + "x";
        font.drawString("§c●§r Replay  " + text, (float)left, (float)(top - 11), 0xFFFFFFFF, true);
        font.drawString(keysHint, (float)left, (float)(top + 9), SythixTheme.TEXT_MUTED, false);
    }
}
