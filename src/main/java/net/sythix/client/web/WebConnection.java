package net.sythix.client.web;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NettyPacketDecoder;
import net.minecraft.network.NettyPacketEncoder;
import net.minecraft.network.NettyVarint21FrameDecoder;
import net.minecraft.network.NettyVarint21FrameEncoder;
import net.minecraft.network.NetworkManager;

/**
 * A server connection for the browser build. Browsers cannot open game (TCP) connections, so the
 * game talks to a small proxy over a WebSocket (web/proxy/sythix_proxy.py), and the proxy talks to
 * the Minecraft server. Inside the game it is a normal NetworkManager: the same packet pipeline
 * runs on an in-memory Netty channel, and the raw bytes go through the WebSocket.
 */
public final class WebConnection
{
    public static final boolean WEB = Boolean.getBoolean("sythix.web");
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();
    private static final List<WebConnection> OPEN = new CopyOnWriteArrayList<WebConnection>();
    private final int socket;
    private final EmbeddedChannel channel;
    private final NetworkManager manager;
    private final byte[] buffer = new byte[65536];
    private boolean closed;

    private WebConnection(int socket, EmbeddedChannel channel, NetworkManager manager)
    {
        this.socket = socket;
        this.channel = channel;
        this.manager = manager;
    }

    /** True when a multiplayer proxy is set on the start page. */
    public static boolean hasProxy()
    {
        return !WebSocketBridge.proxyUrl().isEmpty();
    }

    /** Connects to host:port through the proxy. The proxy looks up SRV records for port 25565, like vanilla. */
    public static NetworkManager connect(String host, int port) throws IOException
    {
        String proxy = WebSocketBridge.proxyUrl();

        if (proxy.isEmpty())
        {
            throw new IOException("Multiplayer in the browser needs a proxy - set one on the start page");
        }

        if (proxy.endsWith("/"))
        {
            proxy = proxy.substring(0, proxy.length() - 1);
        }

        final int socket = WebSocketBridge.open(proxy + "/" + URLEncoder.encode(host + ":" + port, "UTF-8"));
        final NetworkManager manager = new NetworkManager(EnumPacketDirection.CLIENTBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(new ChannelInitializer<Channel>()
        {
            protected void initChannel(Channel ch)
            {
                // plain heap buffers: Netty's pooled direct buffers need JVM internals CheerpJ lacks
                ch.config().setAllocator(HeapAllocator.INSTANCE);
                ch.pipeline().addLast("websocket", new Bridge(socket)).addLast("splitter", new NettyVarint21FrameDecoder()).addLast("decoder", new NettyPacketDecoder(EnumPacketDirection.CLIENTBOUND)).addLast("prepender", new NettyVarint21FrameEncoder()).addLast("encoder", new NettyPacketEncoder(EnumPacketDirection.SERVERBOUND)).addLast("packet_handler", manager);
            }
        });
        OPEN.add(new WebConnection(socket, channel, manager));
        return manager;
    }

    /** Moves received bytes into the game. Called every frame on the client thread. */
    public static void pump()
    {
        for (WebConnection c : OPEN)
        {
            c.receive();
        }
    }

    private void receive()
    {
        if (this.closed)
        {
            OPEN.remove(this);
            return;
        }

        if (!this.channel.isOpen())
        {
            this.shut();
            return;
        }

        // a few reads per frame keep big chunk bursts from stalling the picture
        for (int i = 0; i < 16; ++i)
        {
            int n = WebSocketBridge.read(this.socket, this.buffer);

            if (n > 0)
            {
                if (!this.channel.isOpen())
                {
                    // the game closed the connection while reading
                    this.shut();
                    return;
                }

                try
                {
                    this.channel.writeInbound(Unpooled.copiedBuffer(this.buffer, 0, n));
                }
                catch (Throwable t)
                {
                    LOGGER.warn("[web] connection error", t);
                    this.manager.closeChannel(new net.minecraft.util.text.TextComponentString("Connection error: " + t));
                    this.shut();
                    return;
                }
            }
            else
            {
                if (n < 0)
                {
                    this.shut();
                }

                break;
            }
        }

        this.channel.runPendingTasks();
    }

    private void shut()
    {
        this.closed = true;
        OPEN.remove(this);
        String reason = WebSocketBridge.closeReason(this.socket);
        WebSocketBridge.close(this.socket);

        if (this.channel.isOpen())
        {
            // tell the player why (for example "Can't reach play.example.com")
            this.manager.closeChannel(new net.minecraft.util.text.TextComponentString(reason.isEmpty() ? "Connection closed" : reason));
        }
    }

    /** Always hands out heap buffers, even when Netty asks for direct ones. */
    public static final class HeapAllocator extends io.netty.buffer.AbstractByteBufAllocator
    {
        public static final HeapAllocator INSTANCE = new HeapAllocator();
        private static final io.netty.buffer.UnpooledByteBufAllocator HEAP = new io.netty.buffer.UnpooledByteBufAllocator(false);

        HeapAllocator()
        {
            super(false);
        }

        protected ByteBuf newHeapBuffer(int initialCapacity, int maxCapacity)
        {
            return HEAP.heapBuffer(initialCapacity, maxCapacity);
        }

        protected ByteBuf newDirectBuffer(int initialCapacity, int maxCapacity)
        {
            return HEAP.heapBuffer(initialCapacity, maxCapacity);
        }

        public boolean isDirectBufferPooled()
        {
            return false;
        }
    }

    /** Sends the encoded bytes over the WebSocket and closes it with the channel. */
    private static final class Bridge extends ChannelDuplexHandler
    {
        private final int socket;

        Bridge(int socket)
        {
            this.socket = socket;
        }

        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
        {
            if (msg instanceof ByteBuf)
            {
                ByteBuf buf = (ByteBuf)msg;

                try
                {
                    byte[] data = new byte[buf.readableBytes()];
                    buf.readBytes(data);
                    WebSocketBridge.send(this.socket, data, data.length);
                }
                finally
                {
                    buf.release();
                }

                promise.setSuccess();
            }
            else
            {
                ctx.write(msg, promise);
            }
        }

        public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
        {
            WebSocketBridge.close(this.socket);
            super.close(ctx, promise);
        }
    }
}
