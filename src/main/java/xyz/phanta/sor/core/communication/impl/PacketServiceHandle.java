package xyz.phanta.sor.core.communication.impl;

import xyz.phanta.sor.core.communication.IPacket;
import xyz.phanta.sor.core.communication.IPacketType;
import xyz.phanta.sor.core.util.ByteUtils;

public class PacketServiceHandle implements IPacketType<PacketServiceHandle.Packet> {

    @Override
    public byte getId() {
        return 0x01;
    }

    @Override
    public Packet deserializeBytes(ByteUtils.Reader data) {
        try {
            return new Packet(data.readString(), Class.forName(data.readString()), Class.forName(data.readString()));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static class Packet implements IPacket {

        private final String serviceName;
        private final Class<?> reqType;
        private final Class<?> resType;

        public Packet(String serviceName, Class<?> reqType, Class<?> resType) {
            this.serviceName = serviceName;
            this.reqType = reqType;
            this.resType = resType;
        }

        @Override
        public ByteUtils.Writer serializeBytes() {
            return ByteUtils.writer()
                    .writeString(serviceName)
                    .writeString(reqType.getCanonicalName())
                    .writeString(resType.getCanonicalName());
        }

        public String getServiceName() {
            return serviceName;
        }

        public Class<?> getRequestType() {
            return reqType;
        }

        public Class<?> getResponseType() {
            return resType;
        }

    }

}
