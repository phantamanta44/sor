package xyz.phanta.sor.core.data.serialization;

import xyz.phanta.sor.core.util.ByteUtils;

@FunctionalInterface
public interface ISerializeFunc<T> {

    void serialize(ByteUtils.Writer data, T obj) throws Exception;

}
