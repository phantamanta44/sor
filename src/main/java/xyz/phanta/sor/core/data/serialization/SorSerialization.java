package xyz.phanta.sor.core.data.serialization;

import xyz.phanta.sor.api.exchange.SorMessageType;
import xyz.phanta.sor.api.exchange.SorRequestType;
import xyz.phanta.sor.api.message.ISorMessage;
import xyz.phanta.sor.api.request.ISorRequest;
import xyz.phanta.sor.api.request.ISorResponse;
import xyz.phanta.sor.core.log.SorLog;
import xyz.phanta.sor.core.util.ByteUtils;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SorSerialization {

    private static final Map<Class<?>, SerializationMappings> serializationMappings = new ConcurrentHashMap<>();
    private static final Map<Class<?>, ISerializer<?>> serializers = new HashMap<>();

    static {
        registerSerializer(String.class, ByteUtils.Writer::writeString, ByteUtils.Reader::readString);
        registerSerializer(Class.class,
                (w, c) -> w.writeString(c.getCanonicalName()), r -> Class.forName(r.readString()));
        registerSerializer(BigInteger.class,
                (w, k) -> {
                    byte[] bytes = k.toByteArray();
                    w.writeVarPrecision(bytes.length).writeBytes(bytes);
                }, r -> new BigInteger(r.readBytes(r.readVarPrecision())));
        registerSerializer(BigDecimal.class,
                (w, k) -> {
                    byte[] bytes = k.toBigInteger().toByteArray();
                    w.writeVarPrecision(bytes.length).writeBytes(bytes);
                }, r -> new BigDecimal(new BigInteger(r.readBytes(r.readVarPrecision()))));
        registerSerializer(URL.class, (w, u) -> w.writeString(u.toString()), r -> new URL(r.readString()));
        registerSerializer(URI.class, (w, u) -> w.writeString(u.toString()), r -> URI.create(r.readString()));
        registerSerializer(UUID.class, (w, u) -> w.writeString(u.toString()), r -> UUID.fromString(r.readString()));
        registerSerializer(Date.class,
                (w, d) -> w.writeString(d.toInstant().toString()), r -> Date.from(Instant.parse(r.readString())));
        registerSerializer(Instant.class, (w, i) -> w.writeString(i.toString()), r -> Instant.parse(r.readString()));
        registerSerializer(Duration.class, (w, d) -> w.writeString(d.toString()), r -> Duration.parse(r.readString()));
        // TODO serializers for various useful classes
        // - List, Set, Map
    }

    @SuppressWarnings("unchecked")
    private static <T> SerializationMappings<T> mappingsFor(Class<?> type) {
        return (SerializationMappings<T>)serializationMappings.computeIfAbsent(type, SerializationMappings::new);
    }

    public static <MSG> byte[] serializeMessage(ISorMessage<MSG> message) {
        ByteUtils.Writer data = ByteUtils.writer();
        mappingsFor(message.getType().getMessageClass()).serialize(message.getBody(), data);
        return data.toArray();
    }

    public static <MSG> MSG deserializeMessage(SorMessageType<MSG> type, byte[] data) {
        return SorSerialization.<MSG>mappingsFor(type.getMessageClass()).deserialize(ByteUtils.reader(data));
    }

    public static <REQ> byte[] serializeRequest(ISorRequest<REQ, ?> request) {
        ByteUtils.Writer data = ByteUtils.writer();
        mappingsFor(request.getType().getRequestClass()).serialize(request.getBody(), data);
        return data.toArray();
    }

    public static <REQ> REQ deserializeRequest(SorRequestType<REQ, ?> type, byte[] data) {
        return SorSerialization.<REQ>mappingsFor(type.getRequestClass()).deserialize(ByteUtils.reader(data));
    }

    public static <RES> byte[] serializeResponse(ISorResponse<RES> response) {
        if (response.getBody() == null) return new byte[0];
        ByteUtils.Writer data = ByteUtils.writer();
        mappingsFor(response.getType().getResponseClass()).serialize(response.getBody(), data);
        return data.toArray();
    }

    @Nullable
    public static <RES> RES deserializeResponse(SorRequestType<?, RES> type, byte[] data) {
        return data.length > 0
                ? SorSerialization.<RES>mappingsFor(type.getResponseClass()).deserialize(ByteUtils.reader(data)) : null;
    }

    public static void registerSerializer(ISerializer<?> serializer) {
        serializers.put(serializer.getType(), serializer);
    }

    public static <T> void registerSerializer(Class<T> type, ISerializeFunc<T> serializer,
                                              IDeserializeFunc<T> deserializer) {
        registerSerializer(new ISerializer<T>() {
            @Override
            public Class<T> getType() {
                return type;
            }

            @Override
            public void serialize(ByteUtils.Writer data, T obj) throws Exception {
                serializer.serialize(data, obj);
            }

            @Override
            public T deserialize(ByteUtils.Reader data) throws Exception {
                return deserializer.deserialize(data);
            }
        });
    }

    @SuppressWarnings({ "unchecked" })
    private static <T> ISerializer<T> serializerFor(Class<T> type) {
        ISerializer<T> serializer = (ISerializer<T>)serializers.get(type);
        return serializer != null ? serializer
                : serializers.entrySet().stream()
                .filter(e -> e.getKey().isAssignableFrom(type))
                .map(e -> (ISerializer<T>)e.getValue())
                .findAny().orElseGet(() -> new RecursiveSerializer<>(type));
    }

    private static class SerializationMappings<T> {

        private final List<ISerializableField> fields;
        private final Class<T> type;

        SerializationMappings(Class<T> type) {
            this.fields = Arrays.stream(type.getDeclaredFields())
                    .filter(f -> !Modifier.isTransient(f.getModifiers()))
                    .sorted(Comparator.comparing(Field::getName))
                    .map(SerializationMappings::wrapField)
                    .collect(Collectors.toList());
            this.type = type;
        }

        void serialize(Object obj, ByteUtils.Writer data) {
            for (ISerializableField field : fields) {
                try {
                    field.serialize(obj, data);
                } catch (Exception e) {
                    SorLog.warn("Failed to serialize field %s#%s", type.getCanonicalName(), field);
                    e.printStackTrace(System.out);
                }
            }
        }

        T deserialize(ByteUtils.Reader data) {
            try {
                T obj = type.newInstance();
                for (ISerializableField field : fields) {
                    try {
                        field.deserialize(obj, data);
                    } catch (Exception e) {
                        SorLog.warn("Failed to deserialize field %s#%s", type.getCanonicalName(), field);
                    }
                }
                return obj;
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        private static ISerializableField wrapField(Field field) {
            field.setAccessible(true);
            Class<?> type = field.getType();
            if (type == Byte.TYPE) {
                return new SerializableByteField(field);
            } else if (type == Short.TYPE) {
                return new SerializableShortField(field);
            } else if (type == Integer.TYPE) {
                return new SerializableIntField(field);
            } else if (type == Long.TYPE) {
                return new SerializableLongField(field);
            } else if (type == Float.TYPE) {
                return new SerializableFloatField(field);
            } else if (type == Double.TYPE) {
                return new SerializableDoubleField(field);
            } else if (type == Boolean.TYPE) {
                return new SerializableBooleanField(field);
            } else if (type.isEnum()) {
                return new SerializableEnumField(field);
            } else if (type.isArray() || type == Character.TYPE) { // TODO array serialization
                throw new UnsupportedOperationException("Cannot serialize type: " + type); // TODO char serialization
            } else {
                return new SerializableReferenceField(field);
            }
        }

        private interface ISerializableField {

            void serialize(Object obj, ByteUtils.Writer data) throws Exception;

            void deserialize(Object obj, ByteUtils.Reader data) throws Exception;

        }

        private static class SerializableByteField implements ISerializableField {

            private final Field field;

            SerializableByteField(Field field) {
                this.field = field;
            }

            @Override
            public void serialize(Object obj, ByteUtils.Writer data) throws Exception {
                data.writeByte(field.getByte(obj));
            }

            @Override
            public void deserialize(Object obj, ByteUtils.Reader data) throws Exception {
                field.setByte(obj, data.readByte());
            }

        }

        private static class SerializableShortField implements ISerializableField {

            private final Field field;

            SerializableShortField(Field field) {
                this.field = field;
            }

            @Override
            public void serialize(Object obj, ByteUtils.Writer data) throws Exception {
                data.writeShort(field.getShort(obj));
            }

            @Override
            public void deserialize(Object obj, ByteUtils.Reader data) throws Exception {
                field.setShort(obj, data.readShort());
            }

        }

        private static class SerializableIntField implements ISerializableField {

            private final Field field;

            SerializableIntField(Field field) {
                this.field = field;
            }

            @Override
            public void serialize(Object obj, ByteUtils.Writer data) throws Exception {
                data.writeInt(field.getInt(obj));
            }

            @Override
            public void deserialize(Object obj, ByteUtils.Reader data) throws Exception {
                field.setInt(obj, data.readInt());
            }

        }

        private static class SerializableLongField implements ISerializableField {

            private final Field field;

            SerializableLongField(Field field) {
                this.field = field;
            }

            @Override
            public void serialize(Object obj, ByteUtils.Writer data) throws Exception {
                data.writeLong(field.getLong(obj));
            }

            @Override
            public void deserialize(Object obj, ByteUtils.Reader data) throws Exception {
                field.setLong(obj, data.readLong());
            }

        }

        private static class SerializableFloatField implements ISerializableField {

            private final Field field;

            SerializableFloatField(Field field) {
                this.field = field;
            }

            @Override
            public void serialize(Object obj, ByteUtils.Writer data) throws Exception {
                data.writeFloat(field.getFloat(obj));
            }

            @Override
            public void deserialize(Object obj, ByteUtils.Reader data) throws Exception {
                field.setFloat(obj, data.readFloat());
            }

        }

        private static class SerializableDoubleField implements ISerializableField {

            private final Field field;

            SerializableDoubleField(Field field) {
                this.field = field;
            }

            @Override
            public void serialize(Object obj, ByteUtils.Writer data) throws Exception {
                data.writeDouble(field.getDouble(obj));
            }

            @Override
            public void deserialize(Object obj, ByteUtils.Reader data) throws Exception {
                field.setDouble(obj, data.readDouble());
            }

        }

        private static class SerializableBooleanField implements ISerializableField {

            private final Field field;

            SerializableBooleanField(Field field) {
                this.field = field;
            }

            @Override
            public void serialize(Object obj, ByteUtils.Writer data) throws Exception {
                data.writeBool(field.getBoolean(obj));
            }

            @Override
            public void deserialize(Object obj, ByteUtils.Reader data) throws Exception {
                field.setBoolean(obj, data.readBool());
            }

        }

        private static class SerializableEnumField implements ISerializableField {

            private final Field field;

            SerializableEnumField(Field field) {
                this.field = field;
            }

            @Override
            public void serialize(Object obj, ByteUtils.Writer data) throws Exception {
                data.writeVarPrecision(((Enum)field.get(obj)).ordinal());
            }

            @Override
            public void deserialize(Object obj, ByteUtils.Reader data) throws Exception {
                field.set(obj, field.getType().getEnumConstants()[data.readVarPrecision()]);
            }

        }

        @SuppressWarnings("unchecked")
        private static class SerializableReferenceField<T> implements ISerializableField {

            private final Field field;
            private final ISerializer<T> serializer;

            SerializableReferenceField(Field field) {
                this.field = field;
                this.serializer = (ISerializer<T>)serializerFor(field.getType());
            }

            public void serialize(Object obj, ByteUtils.Writer data) throws Exception {
                serializer.serialize(data, (T)field.get(obj));
            }

            public void deserialize(Object obj, ByteUtils.Reader data) throws Exception {
                field.set(obj, serializer.deserialize(data));
            }

        }

    }

    private static class RecursiveSerializer<T> implements ISerializer<T> {

        private final Class<T> type;
        private final SerializationMappings<T> mappings;

        RecursiveSerializer(Class<T> type) {
            this.type = type;
            this.mappings = mappingsFor(type);
        }

        @Override
        public Class<T> getType() {
            return type;
        }

        @Override
        public T deserialize(ByteUtils.Reader data) {
            return mappings.deserialize(data);
        }

        @Override
        public void serialize(ByteUtils.Writer data, Object obj) {
            mappings.serialize(obj, data);
        }

    }

}
