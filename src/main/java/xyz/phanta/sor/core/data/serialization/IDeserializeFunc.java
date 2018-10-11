package xyz.phanta.sor.core.data.serialization;

import xyz.phanta.sor.core.util.ByteUtils;

@FunctionalInterface
public interface IDeserializeFunc<T> {

    T deserialize(ByteUtils.Reader data) throws Exception;

}
