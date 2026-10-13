package net.sythix.client.eagler;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NettyPacketDecoder;
import net.minecraft.network.NettyPacketEncoder;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TextComponentString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A connection to an Eaglercraft server (an address like wss://mc.example.com). The game's normal
 * packet pipeline runs on an in-memory channel; in front of it this class does Eaglercraft's own
 * login over the WebSocket, then hands every packet through LegacyProtocol, because Eaglercraft
 * servers speak Minecraft 1.8 and this game is 1.12. The browser connects directly, so no proxy
 * is needed.
 */
public final class EaglerConnection
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final List<EaglerConnection> OPEN = new CopyOnWriteArrayList<EaglerConnection>();
    private static final int GAME_PROTOCOL = 47;
    private final EaglerSocket socket;
    private final NetworkManager manager;
    private final EmbeddedChannel channel;
    private final Codec codec = new Codec();
    private final String address;
    private boolean finished;

    private EaglerConnection(String address)
    {
        this.address = address;
        this.socket = net.sythix.client.web.WebConnection.WEB ? new WebEaglerSocket(address) : new NettyEaglerSocket(address);
        this.manager = new NetworkManager(EnumPacketDirection.CLIENTBOUND);
        final boolean web = net.sythix.client.web.WebConnection.WEB;
        this.channel = new EmbeddedChannel(new ChannelInitializer<Channel>()
        {
            protected void initChannel(Channel ch)
            {
                if (web)
                {
                    ch.config().setAllocator(net.sythix.client.web.WebConnection.HeapAllocator.INSTANCE);
                }

                ch.pipeline().addLast("eaglercraft", EaglerConnection.this.codec).addLast("decoder", new NettyPacketDecoder(EnumPacketDirection.CLIENTBOUND)).addLast("encoder", new NettyPacketEncoder(EnumPacketDirection.SERVERBOUND)).addLast("packet_handler", EaglerConnection.this.manager);
            }
        });
    }

    /** True for addresses of Eaglercraft servers: ws://... or wss://... */
    public static boolean isEaglerAddress(String address)
    {
        String a = address == null ? "" : address.trim().toLowerCase();
        return a.startsWith("wss://") || a.startsWith("ws://");
    }

    /** Opens a connection; the returned manager is used like a normal one. */
    public static NetworkManager connect(String address)
    {
        EaglerConnection c = new EaglerConnection(address.trim());
        OPEN.add(c);
        LOGGER.info("[eaglercraft] connecting to {}", c.address);
        return c.manager;
    }

    /** Moves received messages into the game. Called every frame on the client thread. */
    public static void pump()
    {
        for (EaglerConnection c : OPEN)
        {
            synchronized (c)
            {
                c.receive();
            }
        }
    }

    private void receive()
    {
        if (this.finished)
        {
            OPEN.remove(this);
            return;
        }

        if (!this.channel.isOpen())
        {
            this.shut(null);
            return;
        }

        try
        {
            // a frame's worth of messages keeps big chunk bursts from stalling the picture
            long until = System.nanoTime() + 25000000L;
            EaglerSocket.Frame frame;

            while ((frame = this.socket.poll()) != null)
            {
                this.codec.received(frame);

                if (System.nanoTime() > until || this.finished)
                {
                    break;
                }
            }

            this.codec.tick();
        }
        catch (Throwable t)
        {
            LOGGER.warn("[eaglercraft] connection error", t);
            this.shut("Connection error: " + t);
            return;
        }

        if (this.socket.isClosed() && this.codec.mayClose())
        {
            String reason = this.socket.closeReason();
            LOGGER.info("[eaglercraft] the server closed the connection: {}", reason.isEmpty() ? "(no reason)" : reason);
            this.shut(reason.isEmpty() ? "Connection closed" : reason);
            return;
        }

        this.channel.runPendingTasks();
    }

    private void shut(String reason)
    {
        this.finished = true;
        OPEN.remove(this);
        this.socket.close();

        if (this.channel.isOpen() && reason != null)
        {
            this.manager.closeChannel(new TextComponentString(reason));
        }
    }

    /**
     * Sits in front of the packet decoder/encoder: Eaglercraft login and server list query, then
     * 1.8 <-> 1.12 translation.
     */
    private final class Codec extends ChannelDuplexHandler implements LegacyProtocol.Output
    {
        private ChannelHandlerContext context;
        private final LegacyProtocol protocol = new LegacyProtocol(this);
        /** 0 nothing yet, 1 server list query, 2 logging in, 3 playing. */
        private int mode;
        private String username = "";
        private JsonObject motd;
        private boolean waitingForIcon;
        private long queryStart;
        private long queryTime = 50L;
        private long pongTime = -1L;
        private long pongDue;
        private boolean statusDone;

        public void handlerAdded(ChannelHandlerContext ctx)
        {
            this.context = ctx;
        }

        /** Whether a closed socket should close the game's connection now. */
        boolean mayClose()
        {
            // a server list query closes the socket right after answering; finish the ping first
            return this.mode != 1 || this.statusDone;
        }

        // ---- game -> server ------------------------------------------------------------------------

        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
        {
            if (!(msg instanceof ByteBuf))
            {
                ctx.write(msg, promise);
                return;
            }

            ByteBuf buf = (ByteBuf)msg;

            try
            {
                synchronized (EaglerConnection.this)
                {
                    this.fromGame(ctx, new PacketBuffer(buf));
                }
            }
            finally
            {
                buf.release();
            }

            promise.setSuccess();
        }

        public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
        {
            EaglerConnection.this.socket.close();
            super.close(ctx, promise);
        }

        private void fromGame(ChannelHandlerContext ctx, PacketBuffer in) throws Exception
        {
            EnumConnectionState state = ctx.channel().attr(NetworkManager.PROTOCOL_ATTRIBUTE_KEY).get();

            if (state == EnumConnectionState.PLAY)
            {
                this.protocol.fromGame(in);
                return;
            }

            int id = in.readVarInt();

            if (state == EnumConnectionState.HANDSHAKING)
            {
                // the game's handshake says whether this is a server list query or a login
                in.readVarInt();
                in.readString(32767);
                in.readUnsignedShort();
                this.mode = in.readVarInt() == 1 ? 1 : 2;
            }
            else if (state == EnumConnectionState.STATUS)
            {
                if (id == 0)
                {
                    this.queryStart = System.currentTimeMillis();
                    EaglerConnection.this.socket.sendText("Accept: MOTD");
                }
                else if (id == 1)
                {
                    // answered here, after the time the server took to answer the query
                    this.pongTime = in.readLong();
                    this.pongDue = System.currentTimeMillis() + this.queryTime;
                }
            }
            else if (state == EnumConnectionState.LOGIN && id == 0)
            {
                this.username = in.readString(16);
                this.sendClientVersion();
            }
        }

        public void toServer(byte[] packet)
        {
            EaglerConnection.this.socket.send(packet);
        }

        // ---- server -> game ------------------------------------------------------------------------

        public void toGame(PacketBuffer packet)
        {
            this.context.fireChannelRead(packet);
        }

        void received(EaglerSocket.Frame frame) throws Exception
        {
            if (this.mode == 1)
            {
                this.statusMessage(frame);
            }
            else if (this.mode == 3)
            {
                if (!frame.text)
                {
                    this.protocol.fromServer(new PacketBuffer(Unpooled.wrappedBuffer(frame.data)));
                }
            }
            else if (this.mode == 2 && !frame.text)
            {
                this.loginMessage(frame.data);
            }
        }

        void tick()
        {
            this.protocol.flush();

            if (this.mode == 1)
            {
                if (this.waitingForIcon && (EaglerConnection.this.socket.isClosed() || System.currentTimeMillis() - this.queryStart > 3000L))
                {
                    this.sendStatus(null);
                }

                if (this.pongTime >= 0L && System.currentTimeMillis() >= this.pongDue)
                {
                    PacketBuffer out = packet(EnumConnectionState.STATUS, 1);
                    out.writeLong(this.pongTime);
                    this.pongTime = -1L;
                    this.statusDone = true;
                    this.toGame(out);
                }
            }
        }

        // ---- server list query ---------------------------------------------------------------------

        private void statusMessage(EaglerSocket.Frame frame)
        {
            if (frame.text && this.motd == null)
            {
                this.queryTime = Math.max(1L, System.currentTimeMillis() - this.queryStart);
                JsonObject response = new JsonParser().parse(frame.string()).getAsJsonObject();
                this.motd = response;
                JsonObject data = response.has("data") && response.get("data").isJsonObject() ? response.getAsJsonObject("data") : response;

                if (data.has("icon") && data.get("icon").getAsBoolean())
                {
                    this.waitingForIcon = true;
                }
                else
                {
                    this.sendStatus(null);
                }
            }
            else if (!frame.text && this.waitingForIcon && frame.data.length == 16384)
            {
                this.sendStatus(frame.data);
            }
        }

        private void sendStatus(byte[] icon)
        {
            this.waitingForIcon = false;
            JsonObject data = this.motd.has("data") && this.motd.get("data").isJsonObject() ? this.motd.getAsJsonObject("data") : this.motd;
            StringBuilder text = new StringBuilder();

            if (data.has("motd") && data.get("motd").isJsonArray())
            {
                for (JsonElement line : data.getAsJsonArray("motd"))
                {
                    if (text.length() > 0)
                    {
                        text.append('\n');
                    }

                    text.append(line.getAsString());
                }
            }

            JsonObject status = new JsonObject();
            JsonObject description = new JsonObject();
            description.addProperty("text", text.toString());
            status.add("description", description);
            JsonObject players = new JsonObject();
            players.addProperty("max", data.has("max") ? data.get("max").getAsInt() : 0);
            players.addProperty("online", data.has("online") ? data.get("online").getAsInt() : 0);
            JsonArray sample = new JsonArray();

            if (data.has("players") && data.get("players").isJsonArray())
            {
                for (JsonElement name : data.getAsJsonArray("players"))
                {
                    JsonObject p = new JsonObject();
                    p.addProperty("name", name.getAsString());
                    p.addProperty("id", new UUID(0L, 0L).toString());
                    sample.add(p);
                }
            }

            players.add("sample", sample);
            status.add("players", players);
            JsonObject version = new JsonObject();
            version.addProperty("name", "Eaglercraft");
            // shown as joinable: this client translates
            version.addProperty("protocol", 335);
            status.add("version", version);
            String favicon = icon == null ? null : favicon(icon);

            if (favicon != null)
            {
                status.addProperty("favicon", favicon);
            }

            PacketBuffer out = packet(EnumConnectionState.STATUS, 0);
            out.writeString(status.toString());
            this.toGame(out);
        }

        // ---- login ---------------------------------------------------------------------------------

        private void sendClientVersion()
        {
            PacketBuffer out = new PacketBuffer(Unpooled.buffer());
            out.writeByte(1);
            // "post-snapshot" client
            out.writeByte(2);
            // Eaglercraft protocols this client speaks: 2 and 3
            out.writeShort(2);
            out.writeShort(2);
            out.writeShort(3);
            // game protocols: 1.8
            out.writeShort(1);
            out.writeShort(GAME_PROTOCOL);
            ascii(out, "Sythix");
            ascii(out, "1.0");
            // no Eaglercraft account login; newer servers take the player name from here
            out.writeBoolean(false);
            ascii(out, this.username);
            this.sendRaw(out);
        }

        private void loginMessage(byte[] data)
        {
            PacketBuffer in = new PacketBuffer(Unpooled.wrappedBuffer(data));
            int type = in.readUnsignedByte();
            LOGGER.info("[eaglercraft] login step {}", type);

            switch (type)
            {
                case 2:
                {
                    in.readUnsignedShort();
                    in.readUnsignedShort();
                    in.skipBytes(in.readUnsignedByte());
                    in.skipBytes(in.readUnsignedByte());
                    int auth = in.readUnsignedByte();

                    if (auth != 0)
                    {
                        this.loginFailed("This server needs an Eaglercraft account password, which Sythix can't do yet");
                        return;
                    }

                    PacketBuffer out = new PacketBuffer(Unpooled.buffer());
                    out.writeByte(4);
                    ascii(out, this.username);
                    ascii(out, "default");
                    // no password
                    out.writeByte(0);
                    this.sendRaw(out);
                    break;
                }

                case 3:
                    this.loginFailed("This Eaglercraft server doesn't accept 1.8 clients");
                    break;

                case 5:
                {
                    in.skipBytes(in.readUnsignedByte());
                    UUID id = new UUID(in.readLong(), in.readLong());
                    this.loginUuid = id;
                    PacketBuffer out = new PacketBuffer(Unpooled.buffer());
                    out.writeByte(8);
                    this.sendRaw(out);
                    break;
                }

                case 6:
                    this.loginFailed(text(in));
                    break;

                case 9:
                {
                    // logged in: from here on every message is a 1.8 game packet
                    this.mode = 3;
                    PacketBuffer out = packet(EnumConnectionState.LOGIN, 2);
                    out.writeString((this.loginUuid == null ? new UUID(0L, 0L) : this.loginUuid).toString());
                    out.writeString(this.username);
                    this.toGame(out);
                    break;
                }

                case 255:
                {
                    in.readUnsignedByte();
                    this.loginFailed(text(in));
                    break;
                }

                default:
                    this.loginFailed("Unexpected Eaglercraft login message " + type);
            }
        }

        private UUID loginUuid;

        /** A message at the end of a login message: a 2-byte or (older servers) 1-byte length, then UTF-8. */
        private String text(PacketBuffer in)
        {
            int left = in.readableBytes();

            if (left >= 2 && in.getUnsignedShort(in.readerIndex()) == left - 2)
            {
                in.readUnsignedShort();
            }
            else if (left >= 1)
            {
                in.readUnsignedByte();
            }

            String message = in.toString(in.readerIndex(), in.readableBytes(), StandardCharsets.UTF_8);

            // some servers send a chat component
            if (message.startsWith("{"))
            {
                try
                {
                    return net.minecraft.util.text.ITextComponent.Serializer.jsonToComponent(message).getUnformattedText();
                }
                catch (RuntimeException e)
                {
                    return message;
                }
            }

            return message;
        }

        private void loginFailed(String reason)
        {
            if (reason.contains("online mode"))
            {
                // the server asks names of real Minecraft accounts for a Microsoft login
                reason = "This server thinks the name \"" + this.username + "\" belongs to a Minecraft account and wants a Microsoft login. Pick a different name in Profile and join again.";
            }

            LOGGER.info("[eaglercraft] login refused: {}", reason);
            PacketBuffer out = packet(EnumConnectionState.LOGIN, 0);
            out.writeString(net.minecraft.util.text.ITextComponent.Serializer.componentToJson(new TextComponentString(reason)));
            this.toGame(out);
            this.mode = 0;
        }

        private void sendRaw(PacketBuffer out)
        {
            byte[] data = new byte[out.readableBytes()];
            out.readBytes(data);
            EaglerConnection.this.socket.send(data);
        }
    }

    private static PacketBuffer packet(EnumConnectionState state, int id)
    {
        PacketBuffer out = new PacketBuffer(Unpooled.buffer());
        out.writeVarInt(id);
        return out;
    }

    private static void ascii(PacketBuffer out, String s)
    {
        byte[] b = s.getBytes(StandardCharsets.US_ASCII);
        out.writeByte(b.length);
        out.writeBytes(b);
    }

    /** A server icon (64x64 RGBA) as the data: URL the server list shows. */
    private static String favicon(byte[] rgba)
    {
        try
        {
            BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);

            for (int i = 0; i < 4096; ++i)
            {
                int j = i << 2;
                image.setRGB(i & 63, i >> 6, (rgba[j + 3] & 255) << 24 | (rgba[j] & 255) << 16 | (rgba[j + 1] & 255) << 8 | rgba[j + 2] & 255);
            }

            ByteArrayOutputStream png = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "PNG", png);
            return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(png.toByteArray());
        }
        catch (Throwable t)
        {
            // no icon then
            return null;
        }
    }
}
