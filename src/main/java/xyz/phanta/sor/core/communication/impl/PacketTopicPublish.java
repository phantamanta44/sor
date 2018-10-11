package xyz.phanta.sor.core.communication.impl;

import xyz.phanta.sor.api.exchange.SorMessageType;
import xyz.phanta.sor.api.message.ISorMessage;
import xyz.phanta.sor.core.communication.IPacket;
import xyz.phanta.sor.core.communication.IPacketType;
import xyz.phanta.sor.core.util.ByteUtils;
import xyz.phanta.sor.core.data.serialization.SorSerialization;

public class PacketTopicPublish implements IPacketType<PacketTopicPublish.Packet> {

    @Override
    public byte getId() {
        return 0x02;
    }

    @Override
    public Packet deserializeBytes(ByteUtils.Reader data) {
        return new Packet(data.readString(), data.readBytes(data.readVarPrecision()));
    }

    public static class Packet implements IPacket {

        private final String topicName;
        private final byte[] data;

        private Packet(String topicName, byte[] data) {
            this.topicName = topicName;
            this.data = data;
        }

        public Packet(String topicName, ISorMessage<?> message) {
            this(topicName, SorSerialization.serializeMessage(message));
        }

        @Override
        public ByteUtils.Writer serializeBytes() {
            return ByteUtils.writer()
                    .writeString(topicName)
                    .writeVarPrecision(data.length)
                    .writeBytes(data);
        }

        public String getTopicName() {
            return topicName;
        }

        public <MSG> MSG deserializeBody(SorMessageType<MSG> type) {
            return SorSerialization.deserializeMessage(type, data);
        }

    }

}
