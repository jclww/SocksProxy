package com.lww.sparrow.socks.shadow.proxy.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5AddressDecoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;

import java.util.List;

public class ShadowSocksEncoder extends ReplayingDecoder<ShadowSocksEncoder.State> {
    private final Socks5AddressDecoder addressDecoder;

    public ShadowSocksEncoder() {
        this(Socks5AddressDecoder.DEFAULT);
    }
    public ShadowSocksEncoder(Socks5AddressDecoder addressDecoder) {
        super(ShadowSocksEncoder.State.INIT);
        if (addressDecoder == null) {
            throw new NullPointerException("addressDecoder");
        }
        this.addressDecoder = addressDecoder;
    }
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try {
            switch (state()) {
                case INIT: {
                    final Socks5AddressType dstAddrType = Socks5AddressType.valueOf(in.readByte());
                    final String dstAddr = addressDecoder.decodeAddress(dstAddrType, in);
                    final int dstPort = in.readUnsignedShort();
                    System.out.println(dstAddrType + dstAddr + dstPort);
                    out.add(new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, dstAddrType, dstAddr, dstPort));
                    checkpoint(ShadowSocksEncoder.State.SUCCESS);
                }
                case SUCCESS: {
                    int readableBytes = actualReadableBytes();
                    if (readableBytes > 0) {
                        out.add(in.readRetainedSlice(readableBytes));
                    }
                    break;
                }
                case FAILURE: {
                    in.skipBytes(actualReadableBytes());
                    break;
                }
                default: {
                    System.out.println("default");
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    enum State {
        INIT,
        SUCCESS,
        FAILURE
    }
}
