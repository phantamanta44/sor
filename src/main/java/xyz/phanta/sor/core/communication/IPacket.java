package xyz.phanta.sor.core.communication;

import xyz.phanta.sor.core.util.ByteUtils;

public interface IPacket {

    ByteUtils.Writer serializeBytes();

}
