/*
 * Copyright 2016 Justin Shapcott.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.robovm.sdk.dyld.macho.structs;


import org.robovm.sdk.dyld.bytereader.ByteReader;

/**
 * @author Demyan Kimitsa
 * mach-o fat arch entry definition
 */
public class FatArch {
    private int cputype;
    private int cpusubtype;
    private long offset;
    private long size;
    private long align;
    private long reserved;

    public FatArch(ByteReader reader, boolean is64b) {
        read(reader, is64b);
    }

    private FatArch read(ByteReader reader, boolean is64b) {
        return is64b ? read64(reader) : read32(reader);
    }

    public FatArch read32(ByteReader reader) {
        //cpu_type_t	cputype;	/* cpu specifier (int) */
        cputype = reader.readInt32();
        //cpu_subtype_t	cpusubtype;	/* machine specifier (int) */
        cpusubtype = reader.readInt32();
        //uint32_t	offset;		/* file offset to this object file */
        offset = reader.readUnsignedInt32();
        //uint32_t	size;		/* size of this object file */
        size = reader.readUnsignedInt32();
        //uint32_t	align;		/* alignment as a power of 2 */
        align = reader.readUnsignedInt32();

        return this;
    }

    public FatArch read64(ByteReader reader) {
        //cpu_type_t	cputype;	/* cpu specifier (int) */
        cputype = reader.readInt32();
        //cpu_subtype_t	cpusubtype;	/* machine specifier (int) */
        cpusubtype = reader.readInt32();
        //uint64_t	offset;		/* file offset to this object file */
        offset = reader.readLong();
        //uint64_t	size;		/* size of this object file */
        size = reader.readLong();
        //uint32_t	align;		/* alignment as a power of 2 */
        align = reader.readUnsignedInt32();
        //uint32_t	reserved;	/* reserved */
        reserved = reader.readUnsignedInt32();

        return this;
    }

    public int cputype() {
        return cputype;
    }

    public int cpusubtype() {
        return cpusubtype;
    }

    public long offset() {
        return offset;
    }

    public long size() {
        return size;
    }

    public long align() {
        return align;
    }


    private static class ArchReader implements ByteReader.ObjectReader<FatArch> {
        private final boolean is64b;

        public ArchReader(boolean is64b) {
            this.is64b = is64b;
        }

        @Override
        public Class<FatArch> objectClass() {
            return FatArch.class;
        }

        @Override
        public int objectSize() {
            return is64b ? (4 + 4 + 8 + 8 + 4 + 4) : (4 + 4 + 4 + 4 + 4);
        }

        @Override
        public FatArch readObject(ByteReader reader, FatArch object) {
            return object == null ? new FatArch(reader, is64b) : object.read(reader, is64b);
        }
    };

    private static ByteReader.ObjectReader<FatArch> objectReader64 = new ArchReader(true);
    private static ByteReader.ObjectReader<FatArch> objectReader32 = new ArchReader(false);
    public static ByteReader.ObjectReader<FatArch> OBJECT_READER(boolean is64b) {
        return is64b ? objectReader64 : objectReader32;
    }
}
