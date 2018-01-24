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
package org.robovm.sdk.dyld.macho.cmds;

import org.robovm.sdk.dyld.bytereader.ByteReader;

/**
 * @author Demyan Kimitsa
 * mach-o symtab command definition
 */
public class SymtabCommand {
    public final long symoff;
    public final long nsyms;
    public final long stroff;
    public final long strsize;

    public SymtabCommand(ByteReader reader) {
        //uint32_t	symoff;		/* symbol table offset */
        symoff = reader.readUnsignedInt32();
        //uint32_t	nsyms;		/* number of symbol table entries */
        nsyms = reader.readUnsignedInt32();
        //uint32_t	stroff;		/* string table offset */
        stroff = reader.readUnsignedInt32();
        //uint32_t	strsize;	/* string table size in bytes */
        strsize = reader.readUnsignedInt32();
    }
}
