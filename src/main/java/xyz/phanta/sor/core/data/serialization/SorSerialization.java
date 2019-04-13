package xyz.phanta.sor.core.data.serialization;

import xyz.phanta.sor.api.exchange.SorMessageType;
import xyz.phanta.sor.api.exchange.SorRequestType;
import xyz.phanta.sor.api.message.ISorMessage;
import xyz.phanta.sor.api.request.ISorRequest;
import xyz.phanta.sor.api.request.ISorResponse;
import xyz.phanta.sor.core.log.SorLog;
import xyz.phanta.sor.core.util.ByteUtils;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class SorSerialization {

    private static final Map<Class<?>, SerializationMappings> serializationMappings = new ConcurrentHashMap<>();
    private static final Map<Class<?>, ISerializer<?>> serializers = new HashMap<>();

    static {
        registerSerializer(String.class, ByteUtils.Writer::writeString, ByteUtils.Reader::readString);
        registerSerializer(Class.class,
                (w, c) -> w.writeString(c.getCanonicalName()), r -> Class.forName(r.readString()));
        registerSerializer(UUID.class, (w, u) -> w.writeString(u.toString()), r -> UUID.fromString(r.readString()));

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

        registerSerializer(Date.class,
                (w, d) -> w.writeString(d.toInstant().toString()), r -> Date.from(Instant.parse(r.readString())));
        registerSerializer(Instant.class, (w, i) -> w.writeString(i.toString()), r -> Instant.parse(r.readString()));
        registerSerializer(Duration.class, (w, d) -> w.writeString(d.toString()), r -> Duration.parse(r.readString()));
        
        registerSerializer(List.class, (w, l) -> {
            if (l.isEmpty()) {
                w.writeVarPrecision(0);
            } else {
                w.writeVarPrecision(l.size());
                Class<?> type = l.get(0).getClass();
                w.writeString(type.getCanonicalName());
                ISerializer serializer = serializerFor(type);
                for (Object o : l) {
                    serializer.serialize(w, o);
                }
            }
        }, r -> {
            int size = r.readVarPrecision();
            if (size == 0) return Collections.EMPTY_LIST;
            ISerializer serializer = serializerFor(Class.forName(r.readString()));
            List list = new ArrayList();
            while (size-- > 0) list.add(serializer.deserialize(r));
            return list;
        });
        registerSerializer(Set.class, (w, s) -> {
            if (s.isEmpty()) {
                w.writeVarPrecision(0);
            } else {
                w.writeVarPrecision(s.size());
                ISerializer serializer = null;
                for (Object o : s) {
                    if (serializer == null) {
                        Class<?> type = o.getClass();
                        w.writeString(type.getCanonicalName());
                        serializer = serializerFor(type);
                    }
                    serializer.serialize(w, o);
                }
            }
        }, r -> {
            int size = r.readVarPrecision();
            if (size == 0) return Collections.EMPTY_SET;
            ISerializer serializer = serializerFor(Class.forName(r.readString()));
            Set set = new HashSet();
            while (size-- > 0) set.add(serializer.deserialize(r));
            return set;
        });
        registerSerializer(Map.class, (w, m) -> {
            if (m.isEmpty()) {
                w.writeVarPrecision(0);
            } else {
                w.writeVarPrecision(m.size());
                ISerializer keySerializer = null;
                ISerializer valueSerializer = null;
                for (Object entryRaw : m.entrySet()) {
                    Map.Entry entry = (Map.Entry)entryRaw;
                    if (keySerializer == null) {
                        Class<?> keyType = entry.getKey().getClass();
                        Class<?> valueType = entry.getValue().getClass();
                        w.writeString(valueType.getCanonicalName());
                        keySerializer = serializerFor(keyType);
                        valueSerializer = serializerFor(valueType);
                    }
                    keySerializer.serialize(w, entry.getKey());
                    valueSerializer.serialize(w, entry.getValue());
                }
            }
        }, r -> {
            int size = r.readVarPrecision();
            if (size == 0) return Collections.EMPTY_MAP;
            ISerializer keySerializer = serializerFor(Class.forName(r.readString()));
            ISerializer valueSerializer = serializerFor(Class.forName(r.readString()));
            Map map = new HashMap();
            while (size-- > 0) map.put(keySerializer.deserialize(r), valueSerializer.deserialize(r));
            return map;
        });
    }

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
            return wrapField(new IFieldLike.Wrapper(field));
        }

        private static ISerializableField wrapField(IFieldLike field) {
            Class<?> type = field.getType();
            if (type == Byte.TYPE) {
                return new SerializableByteField(field);
            } else if (type == Character.TYPE) {
                return new SerializableCharField(field);
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
            } else if (type.isArray()) {
                return new SerializableArrayField(field, type.getComponentType());
            } else {
                return new SerializableReferenceField<>(field);
            }
        }

        private interface ISerializableField {

            void serialize(Object obj, ByteUtils.Writer data) throws Exception;

            void deserialize(Object obj, ByteUtils.Reader data) throws Exception;

        }

        private static class SerializableByteField implements ISerializableField {

            private final IFieldLike field;

            SerializableByteField(IFieldLike field) {
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

        private static class SerializableCharField implements ISerializableField {

            private final IFieldLike field;

            SerializableCharField(IFieldLike field) {
                this.field = field;
            }

            @Override
            public void serialize(Object obj, ByteUtils.Writer data) throws Exception {
                data.writeByte((byte)field.getChar(obj));
            }

            @Override
            public void deserialize(Object obj, ByteUtils.Reader data) throws Exception {
                field.setChar(obj, (char)data.readByte());
            }

        }

        private static class SerializableShortField implements ISerializableField {

            private final IFieldLike field;

            SerializableShortField(IFieldLike field) {
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

            private final IFieldLike field;

            SerializableIntField(IFieldLike field) {
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

            private final IFieldLike field;

            SerializableLongField(IFieldLike field) {
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

            private final IFieldLike field;

            SerializableFloatField(IFieldLike field) {
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

            private final IFieldLike field;

            SerializableDoubleField(IFieldLike field) {
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

            private final IFieldLike field;

            SerializableBooleanField(IFieldLike field) {
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

            private final IFieldLike field;

            SerializableEnumField(IFieldLike field) {
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

        private static class SerializableArrayField implements ISerializableField, IFieldLike {

            private final IFieldLike field;
            private final Class<?> componentType;
            private final ISerializableField subField;
            private final ThreadLocal<AtomicInteger> pointer = new ThreadLocal<>();

            SerializableArrayField(IFieldLike field, Class<?> componentType) {
                this.field = field;
                this.componentType = componentType;
                this.subField = wrapField(this);
            }

            @Override
            public void serialize(Object obj, ByteUtils.Writer data) throws Exception {
                int length = Array.getLength(field.get(obj));
                AtomicInteger threadPointer = getPointer();
                threadPointer.set(-1);
                while (threadPointer.incrementAndGet() < length) subField.serialize(obj, data);
            }

            @Override
            public void deserialize(Object obj, ByteUtils.Reader data) throws Exception {
                int length = Array.getLength(field.get(obj));
                AtomicInteger threadPointer = getPointer();
                threadPointer.set(-1);
                while (threadPointer.incrementAndGet() < length) subField.deserialize(obj, data);
            }

            private AtomicInteger getPointer() {
                AtomicInteger threadPointer = pointer.get();
                if (threadPointer == null) {
                    threadPointer = new AtomicInteger();
                    pointer.set(threadPointer);
                }
                return threadPointer;
            }

            @Override
            public Class<?> getType() {
                return componentType;
            }

            @Override
            public byte getByte(Object obj) {
                return Array.getByte(obj, pointer.get().get());
            }

            @Override
            public void setByte(Object obj, byte value) {
                Array.setByte(obj, pointer.get().get(), value);
            }

            @Override
            public char getChar(Object obj) {
                return Array.getChar(obj, pointer.get().get());
            }

            @Override
            public void setChar(Object obj, char value) {
                Array.setChar(obj, pointer.get().get(), value);
            }

            @Override
            public short getShort(Object obj) {
                return Array.getShort(obj, pointer.get().get());
            }

            @Override
            public void setShort(Object obj, short value) {
                Array.setShort(obj, pointer.get().get(), value);
            }

            @Override
            public int getInt(Object obj) {
                return Array.getInt(obj, pointer.get().get());
            }

            @Override
            public void setInt(Object obj, int value) {
                Array.setInt(obj, pointer.get().get(), value);
            }

            @Override
            public long getLong(Object obj) {
                return Array.getLong(obj, pointer.get().get());
            }

            @Override
            public void setLong(Object obj, long value) {
                Array.setLong(obj, pointer.get().get(), value);
            }

            @Override
            public float getFloat(Object obj) {
                return Array.getFloat(obj, pointer.get().get());
            }

            @Override
            public void setFloat(Object obj, float value) {
                Array.setFloat(obj, pointer.get().get(), value);
            }

            @Override
            public double getDouble(Object obj) {
                return Array.getDouble(obj, pointer.get().get());
            }

            @Override
            public void setDouble(Object obj, double value) {
                Array.setDouble(obj, pointer.get().get(), value);
            }

            @Override
            public boolean getBoolean(Object obj) {
                return Array.getBoolean(obj, pointer.get().get());
            }

            @Override
            public void setBoolean(Object obj, boolean value) {
                Array.setBoolean(obj, pointer.get().get(), value);
            }

            @Override
            public Object get(Object obj) {
                return Array.get(obj, pointer.get().get());
            }

            @Override
            public void set(Object obj, Object value) {
                Array.set(obj, pointer.get().get(), value);
            }

        }

        private static class SerializableReferenceField<T> implements ISerializableField {

            private final IFieldLike field;
            private final ISerializer<T> serializer;

            SerializableReferenceField(IFieldLike field) {
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
