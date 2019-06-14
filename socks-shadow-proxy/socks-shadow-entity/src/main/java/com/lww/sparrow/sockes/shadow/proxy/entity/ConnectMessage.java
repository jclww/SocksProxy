package com.lww.sparrow.sockes.shadow.proxy.entity;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.Serializable;

public class ConnectMessage implements Serializable {
    public static final ConnectMessage SUCCESS = new ConnectMessage((byte) 1);
    public static final ConnectMessage ERROR = new ConnectMessage((byte) 2);


    private static final int length = 3;

    private static final short prefix = (short) 0xcaca;
    private byte status;

    public ConnectMessage(byte status) {
        this.status = status;
    }

    public static int getLength() {
        return length;
    }

    public static boolean isConnectMessage(short msg) {
        return prefix == msg;
    }

    public static boolean isSuccessConnectMessage(byte status) {
        return ServerConnectStatus.SUCCESS.getStatus() == status;
    }

    public static boolean isERRORConnectMessage(byte status) {
        return ServerConnectStatus.ERROR.getStatus() == status;
    }

    public ByteBuf encodeAsByteBuf() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeShort(prefix);
        byteBuf.writeByte(status);
        return byteBuf;
    }

    enum ServerConnectStatus {
        SUCCESS((byte) 1),
        ERROR((byte) 2);

        private byte status;

        ServerConnectStatus(byte status) {
            this.status = status;
        }

        public byte getStatus() {
            return status;
        }
    }
}
