package xyz.phanta.sor.core.communication;

import xyz.phanta.sor.core.util.ByteUtils;

public interface IPacketType<PKT extends IPacket> {

    byte getId();

    PKT deserializeBytes(ByteUtils.Reader data);

}
