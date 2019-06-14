package com.lww.sparrow.socks.shadow.proxy.client;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.InetSocketAddress;

/**
 * local 本地监听1080端口
 * 使用socks5协议
 * 再与服务端通讯
 *
 * @author liweiwei
 */
public class ClientSocksServer {
    static final int PORT = Integer.parseInt(System.getProperty("port", "1080"));
    static final String REMOTE_HOST = System.getProperty("remote_host", "127.0.0.1");
    static final int REMOTE_PORT = Integer.parseInt(System.getProperty("remote_port", "10086"));

    static final InetSocketAddress address = new InetSocketAddress(REMOTE_HOST, REMOTE_PORT);


    public static void main(String[] args) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ClientSocksServerInitializer(address));
            b.bind(PORT).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
