package org.robovm.sdk.dyld.bytereader;


import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.nio.ByteOrder;
import java.util.Iterator;

public abstract class ByteReader {
    protected ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

    abstract public ByteReader slice();
    abstract public ByteReader slice(long sliceLimit);
    abstract public ByteReader sliceAt(long offset);
    abstract public ByteReader sliceAt(long offset, long sliceLimit);

    abstract public void get(byte[] bytes);
    abstract public void get(byte[] bytes, int offset, int size);
    abstract public byte readByte();
    abstract public char getChar();
    abstract public short readShort();
    abstract public int readInt32();
    abstract public long readLong();
    abstract public float getFloat();
    abstract public double getDouble();

    public int readUnsignedInt16() {
        int res = readShort();
        res += 0x10000L;
        res &= 0xFFFF;
        return res;
    }


    public long readUnsignedInt32() {
        long res = readInt32();
        res += 0x100000000L;
        res &= 0xFFFFFFFFL;
        return res;
    }


    public String readStringZ() {
        // reads null terminated string
        StringBuilder sb = new StringBuilder();
        for (byte b = readByte(); b != 0; b = readByte())
            sb.append((char) b);
        return sb.toString();
    }

    /**
     * peeks string at position, doesn't change current pos
     * @param at offset to read string at
     * @return string read at specified offset
     */
    public String readStringZ(long at) {
        long oldPos = position();
        setPosition(at);
        String s = readStringZ();
        setPosition(oldPos);
        return s;
    }

    public String readString(int size) {
        try {
            byte[] byteArray = new byte[size];
            get(byteArray);
            String res;

            // get real string len
            int strLen;
            strLen = 0;
            while (strLen < size && byteArray[strLen] != 0) {
                strLen++;
            }
            res = new String(byteArray, 0, strLen, "UTF-8");
            return res;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public String readStringWithLen() {
        int stringLen = readInt32();
        if (stringLen == 0)
            return "";
        return readString(stringLen);
    }


    abstract public void order(ByteOrder order);

    abstract public void setPosition(long offset);

    abstract public long position();

    abstract public void limit(long size);

    abstract public long limit();

    public long remaining() {
        return limit() - position();
    }

    public void skip(long bytesToSkip) {
        setPosition(position() + bytesToSkip);
    }


    /**
     * Interface that allows to read object from byte reader
     * @param <T> of object to be read
     */
    public interface ObjectReader<T> {
        Class<T> objectClass();
        int objectSize();
        T readObject(ByteReader reader, T object);
    }


    /**
     * @author Demyan Kimitsa
     * Reads array elements from byte-buffer
     */
    public static class ArrayReader<T> implements Iterable<T> {
        private final ByteReader bufferReader;
        private final long readerOffset;
        private final ObjectReader<T> objectReader;
        private final int size;
        private final boolean reuseElement;
        private T lastAccessedObject;

        public T[] all() {
            if (reuseElement)
                throw new Error("all() shall be used only if reuseelement is false");
            @SuppressWarnings("unchecked")
            T[] res = (T[]) Array.newInstance(objectReader.getClass(), size);
            for (int idx = 0; idx < size; idx++)
                res[idx] = get(idx);
            return res;
        }

        public ArrayReader(ByteReader bufferReader, ObjectReader<T> objectReader, boolean reuseElement) {
            this.bufferReader = bufferReader;
            this.readerOffset = bufferReader.position();
            this.objectReader = objectReader;
            this.size = (int)(bufferReader.limit() / objectReader.objectSize());
            this.reuseElement = reuseElement;
        }

        public ArrayReader(ByteReader bufferReader, ObjectReader<T> objectReader) {
            this(bufferReader, objectReader, true);
        }

        public ArrayReader(ByteReader bufferReader, long offset, int size, ObjectReader<T> objectReader, boolean reuseElement) {
            this.bufferReader = bufferReader;
            this.readerOffset = offset;
            this.objectReader = objectReader;
            this.size = size;
            this.reuseElement = reuseElement;
        }


        public T get(int index) {
            if (index >= size)
                throw new IllegalArgumentException();

            int elementSize = objectReader.objectSize();
            bufferReader.setPosition(readerOffset + elementSize * index);
            lastAccessedObject = objectReader.readObject(bufferReader, reuseElement ? lastAccessedObject : null);
            if (bufferReader.position() - readerOffset - elementSize * index != elementSize)
                throw new IllegalStateException("Object size specified doesn't match actual read from reader");
            return lastAccessedObject;
        }

        public int size() {
            return size;
        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<T>() {
                int idx = 0;

                @Override
                public boolean hasNext() {
                    return idx < size;
                }

                @Override
                public T next() {
                    return get(idx++);
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}
