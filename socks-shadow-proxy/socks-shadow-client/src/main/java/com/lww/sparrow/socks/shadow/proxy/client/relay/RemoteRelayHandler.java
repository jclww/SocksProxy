package com.lww.sparrow.socks.shadow.proxy.client.relay;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;


/**
 * 发送本地监听的真实数据包到远程
 *
 * @author liweiwei
 */
@Slf4j
public final class RemoteRelayHandler extends ChannelInboundHandlerAdapter {

    private final Channel relayChannel;

    public RemoteRelayHandler(Channel relayChannel) {
        this.relayChannel = relayChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            if (relayChannel.isActive()) {
                relayChannel.writeAndFlush(msg);
            }
        } catch (Exception e) {
            log.error("send data to remoteServer error", e);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (relayChannel.isActive()) {
            relayChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
        log.info("outRelay channelInactive close");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.info("", cause);
        ctx.close();
    }

}
