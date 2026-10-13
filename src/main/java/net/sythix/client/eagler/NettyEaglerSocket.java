package net.sythix.client.eagler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/** A WebSocket for the desktop build, with Netty (already part of the game). */
final class NettyEaglerSocket implements EaglerSocket
{
    private static NioEventLoopGroup group;
    private final ConcurrentLinkedQueue<Frame> received = new ConcurrentLinkedQueue<Frame>();
    private final List<WebSocketFrame> waiting = new ArrayList<WebSocketFrame>();
    private volatile boolean open;
    private volatile boolean closed;
    private volatile String reason = "";
    private Channel channel;

    NettyEaglerSocket(String url)
    {
        try
        {
            final URI uri = new URI(url);
            final boolean secure = "wss".equalsIgnoreCase(uri.getScheme());
            final String host = uri.getHost();
            final int port = uri.getPort() > 0 ? uri.getPort() : (secure ? 443 : 80);
            final SslContext ssl = secure ? SslContextBuilder.forClient().build() : null;
            DefaultHttpHeaders headers = new DefaultHttpHeaders();
            // like a browser on this server's own site
            headers.add("Origin", (secure ? "https://" : "http://") + host);
            final WebSocketClientProtocolHandler protocol = new WebSocketClientProtocolHandler(WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, true, headers, 8 * 1024 * 1024), true);
            ChannelFuture future = new Bootstrap().group(group()).channel(NioSocketChannel.class).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000).handler(new ChannelInitializer<SocketChannel>()
            {
                protected void initChannel(SocketChannel ch)
                {
                    if (ssl != null)
                    {
                        ch.pipeline().addLast(ssl.newHandler(ch.alloc(), host, port));
                    }

                    ch.pipeline().addLast(new HttpClientCodec(), new HttpObjectAggregator(65536), protocol, new Receiver());
                }
            }).connect(host, port);
            this.channel = future.channel();
            future.addListener(f ->
            {
                if (!f.isSuccess())
                {
                    this.fail("Can't connect to " + host + (f.cause() == null ? "" : ": " + f.cause().getMessage()));
                }
            });
            this.channel.closeFuture().addListener(f -> this.fail(""));
        }
        catch (Exception e)
        {
            this.fail("Bad server address: " + e.getMessage());
        }
    }

    private static synchronized NioEventLoopGroup group()
    {
        if (group == null)
        {
            group = new NioEventLoopGroup(1, r ->
            {
                Thread t = new Thread(r, "Eaglercraft WebSocket");
                t.setDaemon(true);
                return t;
            });
        }

        return group;
    }

    private void fail(String why)
    {
        if (this.reason.isEmpty() && why != null && !why.isEmpty())
        {
            this.reason = why;
        }

        this.closed = true;
    }

    private final class Receiver extends SimpleChannelInboundHandler<WebSocketFrame>
    {
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
        {
            if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE)
            {
                synchronized (NettyEaglerSocket.this.waiting)
                {
                    NettyEaglerSocket.this.open = true;

                    for (WebSocketFrame frame : NettyEaglerSocket.this.waiting)
                    {
                        ctx.channel().write(frame);
                    }

                    NettyEaglerSocket.this.waiting.clear();
                    ctx.channel().flush();
                }
            }

            super.userEventTriggered(ctx, evt);
        }

        protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame)
        {
            if (frame instanceof BinaryWebSocketFrame || frame instanceof TextWebSocketFrame)
            {
                ByteBuf content = frame.content();
                byte[] data = new byte[content.readableBytes()];
                content.readBytes(data);
                NettyEaglerSocket.this.received.add(new Frame(frame instanceof TextWebSocketFrame, data));
            }
            else if (frame instanceof CloseWebSocketFrame)
            {
                NettyEaglerSocket.this.fail(((CloseWebSocketFrame)frame).reasonText());
            }
        }

        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
        {
            NettyEaglerSocket.this.fail(cause.getMessage());
            ctx.close();
        }
    }

    private void write(WebSocketFrame frame)
    {
        if (this.channel == null || this.closed)
        {
            frame.release();
            return;
        }

        synchronized (this.waiting)
        {
            if (this.open)
            {
                this.channel.writeAndFlush(frame);
            }
            else
            {
                this.waiting.add(frame);
            }
        }
    }

    public void send(byte[] data)
    {
        this.write(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(data)));
    }

    public void sendText(String text)
    {
        this.write(new TextWebSocketFrame(text));
    }

    public Frame poll()
    {
        return this.received.poll();
    }

    public boolean isClosed()
    {
        // only once everything received has been read
        return this.closed && this.received.isEmpty();
    }

    public String closeReason()
    {
        return this.reason;
    }

    public void close()
    {
        if (this.channel != null)
        {
            this.channel.close();
        }

        this.closed = true;
    }
}
