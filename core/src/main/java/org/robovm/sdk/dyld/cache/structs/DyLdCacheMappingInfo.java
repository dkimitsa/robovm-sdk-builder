package org.robovm.sdk.dyld.cache.structs;

import org.robovm.sdk.dyld.bytereader.ByteReader;

public class DyLdCacheMappingInfo {
    private long address;       //    uint64_t	address;
    private long size;          //    uint64_t	size;
    private long fileOffset;    //    uint64_t	fileOffset;
    private long maxProt;       //    uint32_t	maxProt;
    private long initProt;      //    uint32_t	initProt;

    public DyLdCacheMappingInfo(ByteReader reader) {
        read(reader);
    }

    private DyLdCacheMappingInfo read(ByteReader reader) {
        this.address = reader.readLong();
        this.size = reader.readLong();
        this.fileOffset = reader.readLong();
        this.maxProt = reader.readUnsignedInt32();
        this.initProt = reader.readUnsignedInt32();
        return this;
    }

    public long getAddress() {
        return address;
    }

    public long getSize() {
        return size;
    }

    public long getFileOffset() {
        return fileOffset;
    }

    public static int ITEM_SIZE() {
        return 8 + 8 + 8 + 4 + 4;
    }

    private static ByteReader.ObjectReader<DyLdCacheMappingInfo> objectReader = new ByteReader.ObjectReader<DyLdCacheMappingInfo>() {
        @Override
        public Class<DyLdCacheMappingInfo> objectClass() {
            return DyLdCacheMappingInfo.class;
        }

        @Override
        public int objectSize() {
            return 8 + 8 + 8 + 4 + 4;
        }

        @Override
        public DyLdCacheMappingInfo readObject(ByteReader reader, DyLdCacheMappingInfo object) {
            return object == null ? new DyLdCacheMappingInfo(reader) : object.read(reader);
        }
    };

    public static ByteReader.ObjectReader<DyLdCacheMappingInfo> OBJECT_READER() {
        return objectReader;
    }
}
