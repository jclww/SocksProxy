package com.lww.sparrow.socks.shadow.proxy.server;

import com.lww.sparrow.sockes.shadow.proxy.entity.ConnectMessage;
import com.lww.sparrow.socks.shadow.proxy.server.relay.RelayHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;

@ChannelHandler.Sharable
public class ShadowSocksServerHandler extends SimpleChannelInboundHandler<SocksMessage> {
    private final Bootstrap b = new Bootstrap();

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final SocksMessage message) throws Exception {
        final Socks5CommandRequest request = (Socks5CommandRequest) message;
        Promise<Channel> promise = ctx.executor().newPromise();
        promise.addListener(
                (FutureListener<Channel>) future -> {
                    final Channel outboundChannel = future.getNow();
                    if (future.isSuccess()) {
                        ctx.channel().writeAndFlush(ConnectMessage.SUCCESS.encodeAsByteBuf())
                                .addListener((ChannelFutureListener) channelFuture -> {
                                    ctx.pipeline().remove(ShadowSocksServerHandler.this);
                                    outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()));
                                    ctx.pipeline().addLast(new RelayHandler(outboundChannel));
                                });
                    } else {
                        ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                                Socks5CommandStatus.FAILURE, request.dstAddrType()));
                        SocksServerUtils.closeOnFlush(ctx.channel());
                    }
                });

        final Channel inboundChannel = ctx.channel();
        b.group(inboundChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new DirectClientHandler(promise));

        b.connect(request.dstAddr(), request.dstPort()).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                // Connection established use handler provided results
            } else {
                // Close the connection if the connection attempt has failed.
                ctx.channel().writeAndFlush(
                        new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
                SocksServerUtils.closeOnFlush(ctx.channel());
            }
        });
    }
}
