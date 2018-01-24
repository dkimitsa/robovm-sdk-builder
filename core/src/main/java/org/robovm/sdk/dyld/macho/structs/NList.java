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
import org.robovm.sdk.dyld.macho.MachOConsts;

/**
 * @author Demyan Kimitsa
 * nlist structure definition
 */
public class NList {
    private long n_strx;
    private byte n_type;
    private byte n_sect;
    private int n_desc;
    private long n_value;

    public NList(ByteReader reader, boolean is64b) {
        read(reader, is64b);
    }

    private NList read(ByteReader reader, boolean is64b) {
        return is64b ? read64(reader) : read32(reader);
    }

    private NList read64(ByteReader reader) {
        //uint32_t n_strx;	/* index into the string table */
        n_strx = reader.readUnsignedInt32();
        //uint8_t n_type;		/* type flag, see below */
        n_type = reader.readByte();
        //uint8_t n_sect;		/* section number or NO_SECT */
        n_sect = reader.readByte();
        //int16_t n_desc;		/* see <mach-o/stab.h> */
        n_desc = reader.readUnsignedInt16();
        //uint64_t n_value;	/* value of this symbol (or stab offset) */
        n_value = reader.readLong();

        return this;
    }

    private NList read32(ByteReader reader) {
        //uint32_t n_strx;	/* index into the string table */
        n_strx = reader.readUnsignedInt32();
        //uint8_t n_type;		/* type flag, see below */
        n_type = reader.readByte();
        //uint8_t n_sect;		/* section number or NO_SECT */
        n_sect = reader.readByte();
        //int16_t n_desc;		/* see <mach-o/stab.h> */
        n_desc = reader.readUnsignedInt16();
        // uint32_t n_value;	/* value of this symbol (or stab offset) */
        n_value = reader.readUnsignedInt32();

        return this;
    }

    public long n_strx() {
        return n_strx;
    }

    public byte n_type() {
        return n_type;
    }

    public byte n_sect() {
        return n_sect;
    }

    public int n_desc() {
        return n_desc;
    }

    public long n_value() {
        return n_value;
    }

    public boolean isTypeStab() {
        return (n_type & MachOConsts.nlist.N_STAB) != 0;
    }

    public boolean isTypeStabGlobalSymb() {
        return n_type == MachOConsts.stab.N_GSYM;
    }

    public boolean isTypePrivateExternal() {
        return (n_type & MachOConsts.nlist.N_PEXT) != 0;
    }

    public boolean isTypeExternal() {
        return (n_type & MachOConsts.nlist.N_EXT) != 0;
    }

    public boolean isTypeUndfined() {
        return (n_type & MachOConsts.nlist.N_TYPE) == MachOConsts.nlist.N_TYPE_UNDF;
    }

    public boolean isTypeAbsolute() {
        return (n_type & MachOConsts.nlist.N_TYPE) == MachOConsts.nlist.N_TYPE_ABS;
    }

    public boolean isTypeSectDefined() {
        return (n_type & MachOConsts.nlist.N_TYPE) == MachOConsts.nlist.N_TYPE_SECT;
    }

    public boolean isTypePreboundUndefined() {
        return (n_type & MachOConsts.nlist.N_TYPE) == MachOConsts.nlist.N_TYPE_PBUD;
    }

    public boolean isTypeIndirect() {
        return (n_type & MachOConsts.nlist.N_TYPE) == MachOConsts.nlist.N_TYPE_INDR;
    }

    public boolean isWeakDefined() {
        return (n_desc & MachOConsts.nlist.N_WEAK_DEF) == MachOConsts.nlist.N_WEAK_DEF;
    }

    private static class NListReader implements ByteReader.ObjectReader<NList> {
        private final boolean is64b;

        public NListReader(boolean is64b) {
            this.is64b = is64b;
        }

        @Override
        public Class<NList> objectClass() {
            return NList.class;
        }

        @Override
        public int objectSize() {
            return is64b ? (4 + 1 + 1 + 2 + 8) : (4 + 1 + 1 + 2 + 4);
        }

        @Override
        public NList readObject(ByteReader reader, NList object) {
            return object == null ? new NList(reader, is64b) : object.read(reader, is64b);
        }
    };

    private static ByteReader.ObjectReader<NList> objectReader64 = new NListReader(true);
    private static ByteReader.ObjectReader<NList> objectReader32 = new NListReader(false);
    public static ByteReader.ObjectReader<NList> OBJECT_READER(boolean is64b) {
        return is64b ? objectReader64 : objectReader32;
    }
}
