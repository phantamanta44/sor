package xyz.phanta.sor.core.communication;

import io.github.classgraph.ClassGraph;
import xyz.phanta.sor.core.util.ByteUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PacketWrangler {

    private static final Map<Byte, IPacketType<?>> TYPES = new ClassGraph()
            .whitelistPackagesNonRecursive("xyz.phanta.sor.core.communication.impl").scan()
            .getClassesImplementing(IPacketType.class.getCanonicalName()).loadClasses().stream()
            .map(c -> {
                try {
                    return (IPacketType<?>)c.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e); // TODO fail more gracefully
                }
            }).collect(Collectors.toMap(IPacketType::getId, t -> t));

    private final SocketChannel channel;
    private final Consumer<IPacket> consumer;
    private final ByteBuffer accum = ByteBuffer.allocate(1024); // TODO is this sufficient?

    private int expected = -1;
    private SocketState state = SocketState.LENGTH_HIGH_BYTE;

    public PacketWrangler(SocketChannel channel, Consumer<IPacket> consumer) {
        this.channel = channel;
        this.consumer = consumer;
    }

    public synchronized void accept(ByteBuffer buf) {
        while (buf.hasRemaining()) {
            if (state == SocketState.LENGTH_HIGH_BYTE) {
                expected = ((int)buf.get() & 0xFF) << 8;
                state = SocketState.LENGTH_LOW_BYTE;
            } else if (state == SocketState.LENGTH_LOW_BYTE) {
                expected |= buf.get();
                state = SocketState.PACKET_DATA;
            } else {
                if (expected == -1) throw new IllegalStateException();
                accum.put(buf.get());
                if (accum.position() == expected) {
                    consumer.accept(decodePacket(accum));
                    accum.clear();
                    expected = -1;
                    state = SocketState.LENGTH_HIGH_BYTE;
                }
            }
        }
    }

    private IPacket decodePacket(ByteBuffer buf) {
        ByteUtils.Reader data = ByteUtils.reader(buf.array());
        byte packetId = data.readByte();
        IPacketType<?> type = TYPES.get(packetId);
        if (type == null) throw new IllegalStateException("Invalid packet ID: " + packetId);
        return type.deserializeBytes(data);
    }

    public void send(IPacket packet) throws IOException {
        byte[] data = packet.serializeBytes().toArray();
        ByteBuffer lengthBuf = ByteBuffer.allocate(2);
        lengthBuf.putShort((short)data.length);
        channel.write(lengthBuf);
        channel.write(ByteBuffer.wrap(data));
    }

    private enum SocketState {
        LENGTH_HIGH_BYTE, LENGTH_LOW_BYTE, PACKET_DATA
    }

}
