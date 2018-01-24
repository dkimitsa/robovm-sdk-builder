package org.robovm.sdk.dyld.macho.structs;


import org.robovm.sdk.dyld.bytereader.ByteReader;

/**
 * @author Demyan Kimitsa
 * mach-o section entry definition
 */
public class Section {
    private String sectname;
    private String segname;
    private long addr;
    private long size;
    private long offset;
    private long align;
    private long reloff;
    private long nreloc;
    private long flags;
    private long reserved1;
    private long reserved2;
    private long reserved3;

    public Section(ByteReader reader, boolean is64b) {
        read(reader, is64b);
    }

    private Section read(ByteReader reader, boolean is64b) {
        return is64b ? read64(reader) : read32(reader);
    }

    private Section read32(ByteReader reader) {
        //char		sectname[16];	/* name of this section */
        sectname = reader.readString(16);
        //char		segname[16];	/* segment this section goes in */
        segname = reader.readString(16);
        //uint32_t	addr;		/* memory address of this section */
        addr = reader.readInt32();
        //uint32_t	size;		/* size in bytes of this section */
        size = reader.readInt32();
        //uint32_t	offset;		/* file offset of this section */
        offset = reader.readInt32();
        //uint32_t	align;		/* section alignment (power of 2) */
        align = reader.readInt32();
        //uint32_t	reloff;		/* file offset of relocation entries */
        reloff = reader.readInt32();
        //uint32_t	nreloc;		/* number of relocation entries */
        nreloc = reader.readInt32();
        //uint32_t	flags;		/* flags (section type and attributes)*/
        flags = reader.readInt32();
        //uint32_t	reserved1;	/* reserved (for offset or index) */
        reserved1 = reader.readInt32();
        //uint32_t	reserved2;	/* reserved (for count or sizeof) */
        reserved2 = reader.readInt32();

        return this;
    }

    private Section read64(ByteReader reader) {
        //char		sectname[16];	/* name of this section */
        sectname = reader.readString(16);
        //char		segname[16];	/* segment this section goes in */
        segname = reader.readString(16);
        //uint64_t	addr;		/* memory address of this section */
        addr = reader.readLong();
        //uint64_t	size;		/* size in bytes of this section */
        size = reader.readLong();
        //uint32_t	offset;		/* file offset of this section */
        offset = reader.readUnsignedInt32();
        //uint32_t	align;		/* section alignment (power of 2) */
        align = reader.readUnsignedInt32();
        //uint32_t	reloff;		/* file offset of relocation entries */
        reloff = reader.readUnsignedInt32();
        //uint32_t	nreloc;		/* number of relocation entries */
        nreloc = reader.readUnsignedInt32();
        //uint32_t	flags;		/* flags (section type and attributes)*/
        flags = reader.readUnsignedInt32();
        //uint32_t	reserved1;	/* reserved (for offset or index) */
        reserved1 = reader.readUnsignedInt32();
        //uint32_t	reserved2;	/* reserved (for count or sizeof) */
        reserved2 = reader.readUnsignedInt32();
        //uint32_t	reserved3;	/* reserved */
        reserved3 = reader.readUnsignedInt32();

        return this;
    }

    public String sectname() {
        return sectname;
    }

    public String segname() {
        return segname;
    }

    public long addr() {
        return addr;
    }

    public long size() {
        return size;
    }

    public long offset() {
        return offset;
    }

    public long align() {
        return align;
    }

    public long reloff() {
        return reloff;
    }

    public long nreloc() {
        return nreloc;
    }

    public long flags() {
        return flags;
    }

    private static class SectionReader implements ByteReader.ObjectReader<Section> {
        private final boolean is64b;

        public SectionReader(boolean is64b) {
            this.is64b = is64b;
        }

        @Override
        public Class<Section> objectClass() {
            return Section.class;
        }

        @Override
        public int objectSize() {
            return is64b ? (16 + 16 + 8 + 8 + 4 * 8) : (16 + 16 + 4 * 9);
        }

        @Override
        public Section readObject(ByteReader reader, Section object) {
            return object == null ? new Section(reader, is64b) : object.read(reader, is64b);
        }
    };

    private static ByteReader.ObjectReader<Section> objectReader64 = new SectionReader(true);
    private static ByteReader.ObjectReader<Section> objectReader32 = new SectionReader(false);
    public static ByteReader.ObjectReader<Section> OBJECT_READER(boolean is64b) {
        return is64b ? objectReader64 : objectReader32;
    }
}
