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
 * mach-o dyld_info command definition
 */
public class DyldInfoCommand {

    public final long rebase_off;
    public final long rebase_size;
    public final long bind_off;
    public final long bind_size;
    public final long weak_bind_off;
    public final long weak_bind_size;
    public final long lazy_bind_off;
    public final long lazy_bind_size;
    public final long export_off;
    public final long export_size;

    public DyldInfoCommand(ByteReader reader) {
        /*
         * Dyld rebases an image whenever dyld loads it at an address different
         * from its preferred address.  The rebase information is a stream
         * of byte sized opcodes whose symbolic names start with REBASE_OPCODE_.
         * Conceptually the rebase information is a table of tuples:
         *    <seg-index, seg-offset, type>
         * The opcodes are a compressed way to encode the table by only
         * encoding when a column changes.  In addition simple patterns
         * like "every n'th offset for m times" can be encoded in a few
         * bytes.
         */
        rebase_off = reader.readUnsignedInt32();    /* file offset to rebase info  */
        rebase_size = reader.readUnsignedInt32();   /* size of rebase info   */

        /*
         * Dyld binds an image during the loading process, if the image
         * requires any pointers to be initialized to symbols in other images.
         * The bind information is a stream of byte sized
         * opcodes whose symbolic names start with BIND_OPCODE_.
         * Conceptually the bind information is a table of tuples:
         *    <seg-index, seg-offset, type, symbol-library-ordinal, symbol-name, addend>
         * The opcodes are a compressed way to encode the table by only
         * encoding when a column changes.  In addition simple patterns
         * like for runs of pointers initialzed to the same value can be
         * encoded in a few bytes.
         */
        bind_off = reader.readUnsignedInt32();    /* file offset to binding info   */
        bind_size = reader.readUnsignedInt32();   /* size of binding info  */

        /*
         * Some C++ programs require dyld to unique symbols so that all
         * images in the process use the same copy of some code/data.
         * This step is done after binding. The content of the weak_bind
         * info is an opcode stream like the bind_info.  But it is sorted
         * alphabetically by symbol name.  This enable dyld to walk
         * all images with weak binding information in order and look
         * for collisions.  If there are no collisions, dyld does
         * no updating.  That means that some fixups are also encoded
         * in the bind_info.  For instance, all calls to "operator new"
         * are first bound to libstdc++.dylib using the information
         * in bind_info.  Then if some image overrides operator new
         * that is detected when the weak_bind information is processed
         * and the call to operator new is then rebound.
         */
        weak_bind_off = reader.readUnsignedInt32();   /* file offset to weak binding info   */
        weak_bind_size = reader.readUnsignedInt32();  /* size of weak binding info  */

        /*
         * Some uses of external symbols do not need to be bound immediately.
         * Instead they can be lazily bound on first use.  The lazy_bind
         * are contains a stream of BIND opcodes to bind all lazy symbols.
         * Normal use is that dyld ignores the lazy_bind section when
         * loading an image.  Instead the static linker arranged for the
         * lazy pointer to initially point to a helper function which
         * pushes the offset into the lazy_bind area for the symbol
         * needing to be bound, then jumps to dyld which simply adds
         * the offset to lazy_bind_off to get the information on what
         * to bind.
         */
        lazy_bind_off = reader.readUnsignedInt32();    /* file offset to lazy binding info */
        lazy_bind_size = reader.readUnsignedInt32();  /* size of lazy binding infs */

        /*
         * The symbols exported by a dylib are encoded in a trie.  This
         * is a compact representation that factors out common prefixes.
         * It also reduces LINKEDIT pages in RAM because it encodes all
         * information (name, address, flags) in one small, contiguous range.
         * The export area is a stream of nodes.  The first node sequentially
         * is the start node for the trie.
         *
         * Nodes for a symbol start with a uleb128 that is the length of
         * the exported symbol information for the string so far.
         * If there is no exported symbol, the node starts with a zero byte.
         * If there is exported info, it follows the length.
         *
         * First is a uleb128 containing flags. Normally, it is followed by
         * a uleb128 encoded offset which is location of the content named
         * by the symbol from the mach_header for the image.  If the flags
         * is EXPORT_SYMBOL_FLAGS_REEXPORT, then following the flags is
         * a uleb128 encoded library ordinal, then a zero terminated
         * UTF8 string.  If the string is zero length, then the symbol
         * is re-export from the specified dylib with the same name.
         * If the flags is EXPORT_SYMBOL_FLAGS_STUB_AND_RESOLVER, then following
         * the flags is two uleb128s: the stub offset and the resolver offset.
         * The stub is used by non-lazy pointers.  The resolver is used
         * by lazy pointers and must be called to get the actual address to use.
         *
         * After the optional exported symbol information is a byte of
         * how many edges (0-255) that this node has leaving it,
         * followed by each edge.
         * Each edge is a zero terminated UTF8 of the addition chars
         * in the symbol, followed by a uleb128 offset for the node that
         * edge points to.
         *
         */
        export_off = reader.readUnsignedInt32();    /* file offset to lazy binding info */
        export_size = reader.readUnsignedInt32();   /* size of lazy binding infs */
    }
}
