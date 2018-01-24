package org.robovm.sdk.dyld.cache.structs;

import org.robovm.sdk.dyld.bytereader.ByteReader;

public class DyLdCacheImageInfo {
    private long address;   //    uint64_t	address;
    private long modTime;   //    uint64_t	modTime;
    private long inode;     //    uint64_t	inode;
    private long pathFileOffset;    //    uint32_t	pathFileOffset;
    private long pad;               //    uint32_t	pad;

    private DyLdCacheImageInfo(ByteReader reader) {
        read(reader);
    }

    private DyLdCacheImageInfo read(ByteReader reader) {
        this.address = reader.readLong();
        this.modTime = reader.readLong();
        this.inode = reader.readLong();
        this.pathFileOffset = reader.readUnsignedInt32();
        this.pad = reader.readUnsignedInt32();

        return this;
    }

    public long getAddress() {
        return address;
    }

    public long getModTime() {
        return modTime;
    }

    public long getInode() {
        return inode;
    }

    public long getPathFileOffset() {
        return pathFileOffset;
    }

    private static ByteReader.ObjectReader<DyLdCacheImageInfo> objectReader = new ByteReader.ObjectReader<DyLdCacheImageInfo>() {
        @Override
        public Class<DyLdCacheImageInfo> objectClass() {
            return DyLdCacheImageInfo.class;
        }

        @Override
        public int objectSize() {
            return 8 + 8 + 8 + 4 + 4;
        }

        @Override
        public DyLdCacheImageInfo readObject(ByteReader reader, DyLdCacheImageInfo object) {
            return object == null ? new DyLdCacheImageInfo(reader) : object.read(reader);
        }
    };

    public static ByteReader.ObjectReader<DyLdCacheImageInfo> OBJECT_READER() {
        return objectReader;
    }
}
