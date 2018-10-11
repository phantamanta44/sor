package xyz.phanta.sor.core.communication.impl;

import xyz.phanta.sor.api.exchange.SorRequestType;
import xyz.phanta.sor.api.request.ISorResponse;
import xyz.phanta.sor.core.communication.IPacket;
import xyz.phanta.sor.core.communication.IPacketType;
import xyz.phanta.sor.core.util.ByteUtils;
import xyz.phanta.sor.core.data.serialization.SorSerialization;

import javax.annotation.Nullable;

public class PacketServiceResponse implements IPacketType<PacketServiceResponse.Packet> {

    @Override
    public byte getId() {
        return 0x04;
    }

    @Override
    public Packet deserializeBytes(ByteUtils.Reader data) {
        return new Packet(data.readShort(), data.readBytes(data.readVarPrecision()));
    }

    public static class Packet implements IPacket {

        private final short id;
        private final byte[] data;

        private Packet(short id, byte[] data) {
            this.id = id;
            this.data = data;
        }

        public Packet(short id, ISorResponse<?> response) {
            this(id, SorSerialization.serializeResponse(response));
        }

        @Override
        public ByteUtils.Writer serializeBytes() {
            return ByteUtils.writer()
                    .writeShort(id)
                    .writeVarPrecision(data.length)
                    .writeBytes(data);
        }

        public short getRequestId() {
            return id;
        }

        @Nullable
        public <RES> RES deserializeBody(SorRequestType<?, RES> type) {
            return SorSerialization.deserializeResponse(type, data);
        }

    }

}
