package xyz.phanta.sor.core.data.serialization;

public interface ISerializer<T> extends ISerializeFunc<T>, IDeserializeFunc<T> {

    Class<T> getType();

}
