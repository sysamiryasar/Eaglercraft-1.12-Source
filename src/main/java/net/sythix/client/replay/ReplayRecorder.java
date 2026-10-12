package net.sythix.client.replay;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.buffer.Unpooled;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketConfirmTransaction;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraft.network.play.server.SPacketDisconnect;
import net.minecraft.network.play.server.SPacketKeepAlive;
import net.minecraft.network.play.server.SPacketTabComplete;
import net.sythix.client.core.Notifications;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Records a play session: every packet the server sends (on the network thread) plus your own
 * movement, equipment and markers (on the game thread).
 */
public final class ReplayRecorder
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final EntityEquipmentSlot[] SLOTS = EntityEquipmentSlot.values();
    private static DataOutputStream out;
    private static File file;
    private static long start;
    private static long lastTime;
    private static String server;
    private static final List<Long> MARKERS = new ArrayList<Long>();
    private static ItemStack[] lastEquipment;
    private static boolean wasSwinging;
    private static int lastHurt;
    private static long bytes;
    private static boolean sawWorld;

    private ReplayRecorder()
    {
    }

    public static synchronized boolean isRecording()
    {
        return out != null;
    }

    public static synchronized long elapsed()
    {
        return out == null ? 0L : System.currentTimeMillis() - start;
    }

    /** Starts a new file. Called when the server's join packet arrives. */
    public static synchronized void start(String serverName)
    {
        if (out != null)
        {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        sawWorld = false;
        server = serverName;
        file = new File(ReplayFormat.folder(), new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date()) + ReplayFormat.EXTENSION);

        try
        {
            out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(file), 65536), 65536));
            out.writeUTF(ReplayFormat.MAGIC);
            out.writeInt(ReplayFormat.VERSION);
            JsonObject meta = new JsonObject();
            meta.addProperty("name", mc.getSession().getUsername());
            meta.addProperty("uuid", mc.getSession().getProfile().getId() == null ? "" : mc.getSession().getProfile().getId().toString());
            meta.addProperty("server", serverName);
            meta.addProperty("started", Long.valueOf(System.currentTimeMillis()));
            meta.addProperty("minecraft", "1.12.2");
            out.writeUTF(meta.toString());
            start = System.currentTimeMillis();
            lastTime = 0L;
            bytes = 0L;
            MARKERS.clear();
            lastEquipment = null;
            LOGGER.info("[Sythix] Recording replay {}", file.getName());
        }
        catch (Exception e)
        {
            LOGGER.error("[Sythix] Could not start replay", e);
            out = null;
        }
    }

    private static boolean skip(Packet<?> packet)
    {
        return packet instanceof SPacketKeepAlive || packet instanceof SPacketCustomPayload || packet instanceof SPacketDisconnect || packet instanceof SPacketTabComplete || packet instanceof SPacketConfirmTransaction;
    }

    /** A server packet arrived (network thread). */
    public static void onPacket(Packet<?> packet)
    {
        if (!isRecording() || skip(packet))
        {
            return;
        }

        try
        {
            Integer id = EnumConnectionState.PLAY.getPacketId(EnumPacketDirection.CLIENTBOUND, packet);

            if (id == null)
            {
                return;
            }

            PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
            packet.writePacketData(buf);
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            buf.release();
            write(id.intValue(), data);
        }
        catch (Exception e)
        {
            LOGGER.warn("[Sythix] Replay could not store {}: {}", packet.getClass().getSimpleName(), e.toString());
        }
    }

    private static synchronized void write(int id, byte[] data)
    {
        if (out == null)
        {
            return;
        }

        try
        {
            lastTime = System.currentTimeMillis() - start;
            out.writeInt((int)lastTime);
            out.writeInt(id);
            out.writeInt(data.length);
            out.write(data);
            bytes += data.length + 12;
        }
        catch (Exception e)
        {
            LOGGER.error("[Sythix] Replay write failed, stopping", e);
            closeQuietly();
        }
    }

    /** Every client tick: your own player's state. */
    public static void tick()
    {
        if (!isRecording())
        {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP p = mc.player;

        if (mc.world == null)
        {
            // the join packet starts recording on the network thread, a moment before the world
            // exists here; only a world that was there and is gone means the player left
            if (sawWorld)
            {
                stop();
            }

            return;
        }

        sawWorld = true;

        if (p == null)
        {
            return;
        }

        PacketBuffer buf = new PacketBuffer(Unpooled.buffer(48));
        buf.writeDouble(p.posX);
        buf.writeDouble(p.posY);
        buf.writeDouble(p.posZ);
        buf.writeFloat(p.rotationYaw);
        buf.writeFloat(p.rotationPitch);
        buf.writeFloat(p.rotationYawHead);
        buf.writeFloat(p.renderYawOffset);
        int flags = 0;
        boolean swinging = p.isSwingInProgress;

        if (p.isSneaking())
        {
            flags |= 1;
        }

        if (p.isSprinting())
        {
            flags |= 2;
        }

        if (swinging && (!wasSwinging || p.swingProgressInt == 0))
        {
            flags |= 4;
        }

        if (p.hurtTime > lastHurt)
        {
            flags |= 8;
        }

        if (p.isElytraFlying())
        {
            flags |= 16;
        }

        if (p.isInvisible())
        {
            flags |= 32;
        }

        wasSwinging = swinging;
        lastHurt = p.hurtTime;
        buf.writeByte(flags);
        byte[] self = new byte[buf.readableBytes()];
        buf.readBytes(self);
        buf.release();
        write(ReplayFormat.SELF, self);

        ItemStack[] now = new ItemStack[SLOTS.length];
        boolean changed = lastEquipment == null;

        for (int i = 0; i < SLOTS.length; ++i)
        {
            now[i] = p.getItemStackFromSlot(SLOTS[i]);
            changed |= lastEquipment != null && !ItemStack.areItemStacksEqual(now[i], lastEquipment[i]);
        }

        if (changed)
        {
            lastEquipment = new ItemStack[SLOTS.length];
            PacketBuffer eq = new PacketBuffer(Unpooled.buffer());

            for (int i = 0; i < SLOTS.length; ++i)
            {
                lastEquipment[i] = now[i].copy();
                eq.writeItemStack(now[i]);
            }

            byte[] data = new byte[eq.readableBytes()];
            eq.readBytes(data);
            eq.release();
            write(ReplayFormat.EQUIPMENT, data);
        }
    }

    /** Marks this moment (shown on the replay timeline). */
    public static void marker()
    {
        if (isRecording())
        {
            synchronized (ReplayRecorder.class)
            {
                MARKERS.add(Long.valueOf(System.currentTimeMillis() - start));
            }

            write(ReplayFormat.MARKER, new byte[0]);
        }
    }

    private static void closeQuietly()
    {
        try
        {
            if (out != null)
            {
                out.close();
            }
        }
        catch (Exception ignored)
        {
        }

        out = null;
    }

    /** Finishes the file and writes the summary. */
    public static synchronized void stop()
    {
        if (out == null)
        {
            return;
        }

        closeQuietly();
        long duration = lastTime;
        JsonObject summary = new JsonObject();
        summary.addProperty("server", server);
        summary.addProperty("started", Long.valueOf(start));
        summary.addProperty("duration", Long.valueOf(duration));
        summary.addProperty("player", Minecraft.getMinecraft().getSession().getUsername());
        JsonArray markers = new JsonArray();

        for (Long m : MARKERS)
        {
            markers.add(m);
        }

        summary.add("markers", markers);

        try
        {
            if (duration < 2000L)
            {
                // Too short to be useful (for example an instant disconnect)
                file.delete();
                return;
            }

            FileUtils.writeStringToFile(ReplayFormat.summaryFile(file), summary.toString(), StandardCharsets.UTF_8);
            final String text = file.getName().replace(ReplayFormat.EXTENSION, "") + " - " + ReplayFormat.time(duration) + ", " + FileUtils.byteCountToDisplaySize(file.length());
            Minecraft.getMinecraft().addScheduledTask(() -> Notifications.post("Replay saved", text, Notifications.Kind.SUCCESS));
        }
        catch (Exception e)
        {
            LOGGER.error("[Sythix] Could not save replay summary", e);
        }
    }

    public static synchronized File current()
    {
        return file;
    }
}
