package org.robovm.sdk.dyld.bytereader.impl;

import org.robovm.sdk.dyld.bytereader.ByteReader;

import java.nio.ByteOrder;

/**
 * Sublayer for byte readers that will do address/position mapping and will redirect io to wrapped reader
 * there is a method sanityRead that is being called in every io operations and subclasses are
 * responsible to do all validations/manipulations there
 */
public abstract class WrapByteReader extends ByteReader {
    protected final ByteReader wrappedReader;

    public WrapByteReader(ByteReader reader) {
        this.wrappedReader = reader;
    }

    abstract protected void sanityRead(int size);

    @Override
    public void get(byte[] bytes) {
        sanityRead(bytes.length);
        wrappedReader.get(bytes);
    }

    @Override
    public void get(byte[] bytes, int offset, int size) {
        sanityRead(size);
        wrappedReader.get(bytes, offset, size);
    }

    @Override
    public byte readByte() {
        sanityRead(1);
        return wrappedReader.readByte();
    }

    @Override
    public char getChar() {
        sanityRead(2);
        return wrappedReader.getChar();
    }

    @Override
    public short readShort() {
        sanityRead(2);
        return wrappedReader.readShort();
    }

    @Override
    public int readInt32() {
        sanityRead(4);
        return wrappedReader.readInt32();
    }

    @Override
    public long readLong() {
        sanityRead(8);
        return wrappedReader.readLong();
    }

    @Override
    public float getFloat() {
        sanityRead(4);
        return wrappedReader.getFloat();
    }

    @Override
    public double getDouble() {
        sanityRead(8);
        return wrappedReader.getDouble();
    }

    /**
     * this method has to be overridden in subclass to provide proper mapping
     */
    @Override
    public void setPosition(long offset) {
        wrappedReader.setPosition(offset);
    }

    @Override
    public void order(ByteOrder order) {
        wrappedReader.order(order);
    }
}
