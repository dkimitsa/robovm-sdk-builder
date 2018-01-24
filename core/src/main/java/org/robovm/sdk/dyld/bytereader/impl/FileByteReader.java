package org.robovm.sdk.dyld.bytereader.impl;


import org.robovm.sdk.dyld.bytereader.ByteReader;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FileByteReader extends ByteReader {
    private final RandomAccessFile file;
    private long position;
    private long limit;
    private long fileStartOffset;

    // buffering stuff
    private byte readBuffer[];
    private long readBufferPos;
    private ByteBuffer readByteBuffer;

    public FileByteReader(RandomAccessFile file) {
        this.file = file;
        try {
            this.limit = file.length();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FileByteReader(RandomAccessFile file, ByteOrder byteOrder, long limit, long fileStartOffset) {
        this.file = file;
        this.byteOrder = byteOrder;
        this.limit = limit;
        this.fileStartOffset = fileStartOffset;
    }

    public FileByteReader(FileByteReader fileReader) {
        this(fileReader.file, fileReader.byteOrder, fileReader.limit, fileReader.fileStartOffset);
    }

    public void close() throws IOException {
        file.close();
    }

    @Override
    public FileByteReader slice() {
        return new FileByteReader(file, byteOrder, limit - position, fileStartOffset + position );
    }

    @Override
    public FileByteReader slice(long sliceLimit) {
        if (sliceLimit > limit - position)
            throw new RuntimeException("Slicing behind the limit!");
        return new FileByteReader(file, byteOrder, sliceLimit, fileStartOffset + position );
    }

    @Override
    public ByteReader sliceAt(long offset) {
        if (offset < 0 || offset >= limit)
            throw new RuntimeException("Slicing behind the limit!");
        return new FileByteReader(file, byteOrder, limit - offset, fileStartOffset + offset);
    }

    @Override
    public FileByteReader sliceAt(long offset, long sliceLimit) {
        if (offset < 0 || sliceLimit < 0 || offset + sliceLimit > limit)
            throw new RuntimeException("Slicing behind the limit!");
        return new FileByteReader(file, byteOrder, sliceLimit, fileStartOffset + offset);
    }


    @Override
    public void get(byte[] bytes) {
        sanityReadSize(bytes.length);
        try {
            synchronized (file) {
                file.seek(fileStartOffset + position);
                position += bytes.length;
                file.read(bytes);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void get(byte[] bytes, int offset, int size) {
        sanityReadSize(size);
        try {
            synchronized (file) {
                file.seek(fileStartOffset + position);
                position += size;
                file.read(bytes, offset, size);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public byte readByte() {
        sanityReadSize(1);
        try {
            synchronized (file) {
                long readPos = fileStartOffset + position;
                position += 1;

                // only buffered read
                int readBufferOffset = prepareBufferedRead(readPos, 1);
                return readBuffer[readBufferOffset];
            }
        } catch (IOException e) {
            position -= 1; // revert position back
            throw new RuntimeException(e);
        }
    }

    @Override
    public char getChar() {
        sanityReadSize(2);
        try {
            synchronized (file) {
                long readPos = fileStartOffset + position;
                position += 2;

                // only buffered read
                int readBufferOffset = prepareBufferedRead(readPos, 2);
                readByteBuffer.position(readBufferOffset);
                char c = readByteBuffer.getChar();
                return byteOrder == ByteOrder.BIG_ENDIAN ? c : Character.reverseBytes(c);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public short readShort() {
        sanityReadSize(2);
        try {
            synchronized (file) {
                long readPos = fileStartOffset + position;
                position += 2;

                // only buffered read
                int readBufferOffset = prepareBufferedRead(readPos, 2);
                readByteBuffer.position(readBufferOffset);
                short s = readByteBuffer.getShort();
                return byteOrder == ByteOrder.BIG_ENDIAN ? s : Short.reverseBytes(s);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public int readInt32() {
        sanityReadSize(4);
        try {
            synchronized (file) {
                long readPos = fileStartOffset + position;
                position += 4;

                // only buffered read
                int readBufferOffset = prepareBufferedRead(readPos, 4);
                readByteBuffer.position(readBufferOffset);
                int i = readByteBuffer.getInt();
                return byteOrder == ByteOrder.BIG_ENDIAN ? i : Integer.reverseBytes(i);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public long readLong() {
        sanityReadSize(8);
        try {
            synchronized (file) {
                long readPos = fileStartOffset + position;
                position += 8;

                // only buffered read
                int readBufferOffset = prepareBufferedRead(readPos, 8);
                readByteBuffer.position(readBufferOffset);
                long l = readByteBuffer.getLong();
                return byteOrder == ByteOrder.BIG_ENDIAN ? l : Long.reverseBytes(l);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public float getFloat() {
        sanityReadSize(4);
        try {
            synchronized (file) {
                file.seek(fileStartOffset + position);
                position += 4;
                float f = file.readFloat();
                return byteOrder == ByteOrder.BIG_ENDIAN ? f : Float.intBitsToFloat(Integer.reverse(Float.floatToIntBits(f)));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public double getDouble() {
        sanityReadSize(8);
        try {
            synchronized (file) {
                file.seek(fileStartOffset + position);
                position += 8;
                double d = file.readDouble();
                return byteOrder == ByteOrder.BIG_ENDIAN ? d : Double.longBitsToDouble(Long.reverse(Double.doubleToLongBits(d)));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void order(ByteOrder order) {
        this.byteOrder = order;
    }


    @Override
    public void setPosition(long offset) {
        if (position < 0 || position > limit)
            throw new IllegalArgumentException();
        this.position = offset;
    }


    @Override
    public long position() {
        return position;
    }


    @Override
    public void limit(long size) {
        if (size < 0 || size > limit)
            throw new IllegalArgumentException();
        limit = size;
    }


    @Override
    public long limit() {
        return this.limit;
    }


    private void sanityReadSize(long size) {
        if (position + size > limit)
            throw new BufferUnderflowException();
    }

    private int prepareBufferedRead(long readPos, int readLength) throws IOException {
        // only buffered read
        long readBufferOffset;
        if (readBuffer != null) {
            readBufferOffset = readPos - readBufferPos;
            if (readBufferOffset >= 0 && readBufferOffset + readLength <= readBuffer.length)
                return (int) readBufferOffset;
        }

        // buffer miss, read buff
        long bytesAvailable = file.length() - readPos;
        bytesAvailable = Math.min(bytesAvailable, 2048);

        // reallocate buffer if needed
        readBufferPos = readPos;
        if (readBuffer == null || readBuffer.length != bytesAvailable) {
            readBuffer = new byte[(int) bytesAvailable];
            readByteBuffer = ByteBuffer.wrap(readBuffer);
        }
        file.seek(readPos);
        file.read(readBuffer);
        return 0;
    }
}
