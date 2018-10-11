package xyz.phanta.sor.core.communication.impl;

import xyz.phanta.sor.core.communication.IPacket;
import xyz.phanta.sor.core.communication.IPacketType;
import xyz.phanta.sor.core.util.ByteUtils;

public class PacketTopicSubscribe implements IPacketType<PacketTopicSubscribe.Packet> {

    @Override
    public byte getId() {
        return 0x00;
    }

    @Override
    public Packet deserializeBytes(ByteUtils.Reader data) {
        try {
            return new Packet(data.readString(), Class.forName(data.readString()));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static class Packet implements IPacket {

        private final String topicName;
        private final Class<?> msgType;

        public Packet(String topicName, Class<?> msgType) {
            this.topicName = topicName;
            this.msgType = msgType;
        }

        @Override
        public ByteUtils.Writer serializeBytes() {
            return ByteUtils.writer()
                    .writeString(topicName)
                    .writeString(msgType.getCanonicalName());
        }

        public String getTopicName() {
            return topicName;
        }

        public Class<?> getMessageType() {
            return msgType;
        }

    }

}
