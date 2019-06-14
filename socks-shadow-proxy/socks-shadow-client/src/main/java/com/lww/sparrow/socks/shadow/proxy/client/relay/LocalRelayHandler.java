package com.lww.sparrow.socks.shadow.proxy.client.relay;

import com.lww.sparrow.sockes.shadow.proxy.entity.ConnectMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * remote -> client
 * 发送给客户端
 */
@Slf4j
public final class LocalRelayHandler extends ChannelInboundHandlerAdapter {


	private final Channel relayChannel;
	private final Socks5AddressType socks5AddressType;

	public LocalRelayHandler(Channel relayChannel, Socks5AddressType socks5AddressType) {
		this.relayChannel = relayChannel;
		this.socks5AddressType = socks5AddressType;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		try {
			if (relayChannel.isActive()) {
				log.debug("get remote message" + relayChannel);
				ByteBuf byteBuff = (ByteBuf) msg;
				if (byteBuff.readableBytes() == ConnectMessage.getLength()) {
                    if (ConnectMessage.isConnectMessage(byteBuff.readShort()) && ConnectMessage.isSuccessConnectMessage(byteBuff.readByte())) {
                        relayChannel.writeAndFlush(new DefaultSocks5CommandResponse(
                                Socks5CommandStatus.SUCCESS,
                                socks5AddressType));
                    } else {
                        relayChannel.writeAndFlush(new DefaultSocks5CommandResponse(
                                Socks5CommandStatus.FAILURE,
                                socks5AddressType));
                    }
                    return;
                }
                if (!byteBuff.hasArray()) {
                    relayChannel.writeAndFlush(msg);
				}
			}
		} catch (Exception e) {
            log.error("receive remoteServer data error", e);
		}
	}

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (relayChannel.isActive()) {
            relayChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
