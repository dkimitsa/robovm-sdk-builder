package org.robovm.sdk.dyld.bytereader.impl;

import org.robovm.sdk.dyld.bytereader.ByteReader;

import java.util.Arrays;

/**
 * Reader that uses address map and implements virtual addresses withing flat reader
 */
public class MappedByteReader extends WrapByteReader {

    public static class MappingEntry {
        private final long startPos;
        private final long endPos;
        private final long targetStartPos;

        public MappingEntry(long startPos, long endPos, long targetStartPos) {
            this.startPos = startPos;
            this.endPos = endPos;
            this.targetStartPos = targetStartPos;
        }
    }

    private final MappingEntry[] mappingEntries;
    private MappingEntry activeMappingEntry;
    private long mappedPosition;
    private final long mappingStartOffset;
    private long mappingLimit;

    public MappedByteReader(ByteReader reader, MappingEntry[] entries) {
        super(reader);
        this.mappingEntries = Arrays.copyOf(entries, entries.length);
        Arrays.sort(mappingEntries, (o1, o2) -> {
            long d = o1.startPos - o2.startPos;
            return d < 0 ? -1 : (d > 0 ? 1 : 0);
        });
        this.mappingStartOffset = 0;
        this.mappingLimit = entries[entries.length - 1].endPos + 1;
        this.setPosition(entries[0].startPos);
    }

    private MappedByteReader(ByteReader wrappedReader, MappingEntry[] mappingEntries, long startOffset, long limit) {
        super(wrappedReader);
        this.mappingEntries = mappingEntries;
        this.mappingStartOffset = startOffset;
        this.mappingLimit = limit;
        this.setPosition(0);
    }


    @Override
    protected void sanityRead(int size) {
        // after previous read position could be after mapped section
        activeMappingEntry = findRegion(mappedPosition);
        if (activeMappingEntry == null)
            throw new IllegalArgumentException("there is no region to read addr @" + Long.toHexString(mappedPosition));

        // check for cross-boundary read
        if (mappedPosition + size > activeMappingEntry.endPos)
            throw new IllegalArgumentException("cross boundary read at addr @" + Long.toHexString(mappedPosition));

        // assume that read will be successful and move move pointer
        mappedPosition += size;
    }

    @Override
    public ByteReader slice() {
        return new MappedByteReader(wrappedReader, mappingEntries, mappedPosition, remaining());
    }

    @Override
    public ByteReader slice(long sliceLimit) {
        if (position() + sliceLimit > remaining())
            throw new IllegalArgumentException();
        return new MappedByteReader(wrappedReader, mappingEntries, mappedPosition, sliceLimit);
    }

    @Override
    public ByteReader sliceAt(long offset) {
        if (offset < 0)
            throw new IllegalArgumentException();
        if (offset > limit())
            throw new IllegalArgumentException();
        return new MappedByteReader(wrappedReader, mappingEntries, mappingStartOffset + offset, limit() - offset);
    }

    @Override
    public ByteReader sliceAt(long offset, long sliceLimit) {
        if (offset < 0)
            throw new IllegalArgumentException();
        if (offset + sliceLimit > remaining())
            throw new IllegalArgumentException();
        return new MappedByteReader(wrappedReader, mappingEntries, mappingStartOffset + offset, sliceLimit);
    }

    @Override
    public void setPosition(long pos) {
        if (pos < 0 || pos > mappingLimit)
            throw new IllegalArgumentException();

        activeMappingEntry = findRegion(mappingStartOffset + pos);
        if (activeMappingEntry == null)
            throw new IllegalArgumentException("there is no region to read addr @" + Long.toHexString(pos));

        mappedPosition = mappingStartOffset + pos;
        super.setPosition((mappedPosition - activeMappingEntry.startPos) + activeMappingEntry.targetStartPos);
    }


    @Override
    public long position() {
        return mappedPosition - mappingStartOffset;
    }

    @Override
    public void limit(long size) {
        if (size > mappingLimit)
            throw new IllegalArgumentException();
        mappingLimit = size;
    }

    @Override
    public long limit() {
        return mappingLimit;
    }

    private MappingEntry findRegion(long addr) {
        if (activeMappingEntry != null && isAddrInRegion(addr, activeMappingEntry))
            return activeMappingEntry;

        // use binary search
        int left = 0;
        int right = mappingEntries.length - 1;
        while (right >= left) {
            int middle = (left + right) / 2;
            MappingEntry r = mappingEntries[middle];
            if (addr < r.startPos) {
                right = middle - 1;
            } else if (addr > r.endPos) {
                left = middle + 1;
            } else {
                return r;
            }
        }

        return null;
    }

    private boolean isAddrInRegion(long addr, MappingEntry activeBlock) {
        return addr >= activeBlock.startPos && addr < activeBlock.endPos;
    }
}
