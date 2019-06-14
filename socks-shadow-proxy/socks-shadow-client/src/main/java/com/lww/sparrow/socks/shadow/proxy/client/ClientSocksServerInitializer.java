package com.lww.sparrow.socks.shadow.proxy.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.InetSocketAddress;

public class ClientSocksServerInitializer extends ChannelInitializer<SocketChannel> {
    private final InetSocketAddress serverAddress;

    public ClientSocksServerInitializer(InetSocketAddress serverAddress) {
        this.serverAddress = serverAddress;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(
                new LoggingHandler(LogLevel.DEBUG),
                new SocksPortUnificationServerHandler(),
                ClientSocksHandler.INSTANCE);
    }
}
