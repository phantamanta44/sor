package xyz.phanta.sor.core.communication.impl;

import xyz.phanta.sor.api.exchange.SorRequestType;
import xyz.phanta.sor.api.request.ISorRequest;
import xyz.phanta.sor.core.communication.IPacket;
import xyz.phanta.sor.core.communication.IPacketType;
import xyz.phanta.sor.core.util.ByteUtils;
import xyz.phanta.sor.core.data.serialization.SorSerialization;

public class PacketServiceRequest implements IPacketType<PacketServiceRequest.Packet> {

    @Override
    public byte getId() {
        return 0x03;
    }

    @Override
    public Packet deserializeBytes(ByteUtils.Reader data) {
        return new Packet(data.readShort(), data.readString(), data.readBytes(data.readVarPrecision()));
    }

    public static class Packet implements IPacket {

        private final short id;
        private final String serviceName;
        private final byte[] data;

        private Packet(short id, String serviceName, byte[] data) {
            this.id = id;
            this.serviceName = serviceName;
            this.data = data;
        }

        public Packet(short id, String serviceName, ISorRequest<?, ?> request) {
            this(id, serviceName, SorSerialization.serializeRequest(request));
        }

        @Override
        public ByteUtils.Writer serializeBytes() {
            return ByteUtils.writer()
                    .writeString(serviceName)
                    .writeVarPrecision(data.length)
                    .writeBytes(data);
        }

        public short getRequestId() {
            return id;
        }

        public String getServiceName() {
            return serviceName;
        }

        public <REQ> REQ deserializeBody(SorRequestType<REQ, ?> type) {
            return SorSerialization.deserializeRequest(type, data);
        }

    }

}
