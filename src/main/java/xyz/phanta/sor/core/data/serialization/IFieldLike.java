package xyz.phanta.sor.core.data.serialization;

import java.lang.reflect.Field;

interface IFieldLike {

    Class<?> getType();

    byte getByte(Object obj) throws IllegalAccessException;

    void setByte(Object obj, byte value) throws IllegalAccessException;

    char getChar(Object obj) throws IllegalAccessException;

    void setChar(Object obj, char value) throws IllegalAccessException;

    short getShort(Object obj) throws IllegalAccessException;

    void setShort(Object obj, short value) throws IllegalAccessException;

    int getInt(Object obj) throws IllegalAccessException;

    void setInt(Object obj, int value) throws IllegalAccessException;

    long getLong(Object obj) throws IllegalAccessException;

    void setLong(Object obj, long value) throws IllegalAccessException;

    float getFloat(Object obj) throws IllegalAccessException;

    void setFloat(Object obj, float value) throws IllegalAccessException;

    double getDouble(Object obj) throws IllegalAccessException;

    void setDouble(Object obj, double value) throws IllegalAccessException;

    boolean getBoolean(Object obj) throws IllegalAccessException;

    void setBoolean(Object obj, boolean value) throws IllegalAccessException;

    Object get(Object obj) throws IllegalAccessException;

    void set(Object obj, Object value) throws IllegalAccessException;
    
    class Wrapper implements IFieldLike {

        private final Field field;

        Wrapper(Field field) {
            this.field = field;
        }

        @Override
        public Class<?> getType() {
            return field.getType();
        }

        @Override
        public Object get(Object obj) throws IllegalAccessException {
            return field.get(obj);
        }

        @Override
        public boolean getBoolean(Object obj) throws IllegalAccessException {
            return field.getBoolean(obj);
        }

        @Override
        public byte getByte(Object obj) throws IllegalAccessException {
            return field.getByte(obj);
        }

        @Override
        public char getChar(Object obj) throws IllegalAccessException {
            return field.getChar(obj);
        }

        @Override
        public short getShort(Object obj) throws IllegalAccessException {
            return field.getShort(obj);
        }

        @Override
        public int getInt(Object obj) throws IllegalAccessException {
            return field.getInt(obj);
        }

        @Override
        public long getLong(Object obj) throws IllegalAccessException {
            return field.getLong(obj);
        }

        @Override
        public float getFloat(Object obj) throws IllegalAccessException {
            return field.getFloat(obj);
        }

        @Override
        public double getDouble(Object obj) throws IllegalAccessException {
            return field.getDouble(obj);
        }

        @Override
        public void set(Object obj, Object value) throws IllegalAccessException {
            field.set(obj, value);
        }

        @Override
        public void setBoolean(Object obj, boolean z) throws IllegalAccessException {
            field.setBoolean(obj, z);
        }

        @Override
        public void setByte(Object obj, byte b) throws IllegalAccessException {
            field.setByte(obj, b);
        }

        @Override
        public void setChar(Object obj, char c) throws IllegalAccessException {
            field.setChar(obj, c);
        }

        @Override
        public void setShort(Object obj, short s) throws IllegalAccessException {
            field.setShort(obj, s);
        }

        @Override
        public void setInt(Object obj, int i) throws IllegalAccessException {
            field.setInt(obj, i);
        }

        @Override
        public void setLong(Object obj, long l) throws IllegalAccessException {
            field.setLong(obj, l);
        }

        @Override
        public void setFloat(Object obj, float f) throws IllegalAccessException {
            field.setFloat(obj, f);
        }

        @Override
        public void setDouble(Object obj, double d) throws IllegalAccessException {
            field.setDouble(obj, d);
        }

    }

}
