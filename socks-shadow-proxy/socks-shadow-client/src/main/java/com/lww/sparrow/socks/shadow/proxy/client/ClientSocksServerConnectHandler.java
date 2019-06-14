package com.lww.sparrow.socks.shadow.proxy.client;

import com.lww.sparrow.socks.shadow.proxy.client.relay.LocalRelayHandler;
import com.lww.sparrow.socks.shadow.proxy.client.relay.RemoteRelayHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v4.DefaultSocks4CommandResponse;
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest;
import io.netty.handler.codec.socksx.v4.Socks4CommandStatus;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.CharsetUtil;
import io.netty.util.NetUtil;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;


/**
 * local 处理请求
 *
 * @author liweiwei
 */
@ChannelHandler.Sharable
public final class ClientSocksServerConnectHandler extends SimpleChannelInboundHandler<SocksMessage> {

    private final Bootstrap b = new Bootstrap();

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final SocksMessage message) throws Exception {
        if (message instanceof Socks4CommandRequest) {
            // socket4不支持
            ctx.channel().writeAndFlush(new DefaultSocks4CommandResponse(Socks4CommandStatus.REJECTED_OR_FAILED));
            SocksServerUtils.closeOnFlush(ctx.channel());

        } else if (message instanceof Socks5CommandRequest) {
            final Socks5CommandRequest request = (Socks5CommandRequest) message;

            Promise<Channel> promise = ctx.executor().newPromise();
            promise.addListener((FutureListener<Channel>) future -> {
                final Channel outboundChannel = future.getNow();
                if (future.isSuccess()) {
                    ctx.channel().newSucceededFuture().addListener((ChannelFutureListener) channelFuture -> {
                        ctx.pipeline().remove(ClientSocksServerConnectHandler.this);
                        outboundChannel.pipeline().addLast(new LocalRelayHandler(ctx.channel(), request.dstAddrType()));
                        ctx.pipeline().addLast(new RemoteRelayHandler(outboundChannel));
                        // 需要发送host & host到server
                        sendConnectRemoteMessage(request, outboundChannel);
                    });
                } else {
                    ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
                    SocksServerUtils.closeOnFlush(ctx.channel());
                }
            });

            final Channel inboundChannel = ctx.channel();
            b.group(inboundChannel.eventLoop())
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new DirectClientHandler(promise));

            b.connect(getRemoteAddr(request), getRemotePort(request)).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    // Connection established use handler provided results
                } else {
                    // Close the connection if the connection attempt has failed.
                    ctx.channel().writeAndFlush(
                            new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
                    SocksServerUtils.closeOnFlush(ctx.channel());
                }
            });
        } else {
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        SocksServerUtils.closeOnFlush(ctx.channel());
    }

    /**
     * 发送local请求的真实host & port
     * 用来server建立链接
     *
     * @param request
     * @param outboundChannel
     */
    private void sendConnectRemoteMessage(Socks5CommandRequest request, Channel outboundChannel) {
        ByteBuf buff = encodeAsByteBuf(request);
        if (outboundChannel.isActive()) {
            outboundChannel.writeAndFlush(Unpooled.wrappedBuffer(buff));
        }
    }

    private ByteBuf encodeAsByteBuf(Socks5CommandRequest request) {
        ByteBuf byteBuf = Unpooled.buffer();

        byteBuf.writeByte(request.dstAddrType().byteValue());
        switch (request.dstAddrType().byteValue()) {
            case 0x01:
                byteBuf.writeBytes(NetUtil.createByteArrayFromIpAddressString(request.dstAddr()));
                byteBuf.writeShort(request.dstPort());
                break;
            case 0x03:
                byteBuf.writeByte(request.dstAddr().length());
                byteBuf.writeBytes(request.dstAddr().getBytes(CharsetUtil.US_ASCII));
                byteBuf.writeShort(request.dstPort());
                break;
            case 0x04:
                byteBuf.writeBytes(NetUtil.createByteArrayFromIpAddressString(request.dstAddr()));
                byteBuf.writeShort(request.dstPort());
                break;
            default:
                ;
        }
        return byteBuf;
    }

    public String getRemoteAddr(Socks5CommandRequest request) {
        return System.getProperty("remote_host", "127.0.0.1");
    }

    public Integer getRemotePort(Socks5CommandRequest request) {
        return Integer.parseInt(System.getProperty("remote_port", "10086"));
    }
}