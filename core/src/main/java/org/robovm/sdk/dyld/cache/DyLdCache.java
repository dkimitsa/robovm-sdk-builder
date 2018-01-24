package org.robovm.sdk.dyld.cache;

import org.robovm.sdk.dyld.bytereader.ByteReader;
import org.robovm.sdk.dyld.bytereader.impl.FileByteReader;
import org.robovm.sdk.dyld.bytereader.impl.MappedByteReader;
import org.robovm.sdk.dyld.cache.structs.DyLdCacheImageInfo;
import org.robovm.sdk.dyld.cache.structs.DyLdCacheMappingInfo;
import org.robovm.sdk.dyld.cache.structs.DyldCacheHeader;
import org.robovm.sdk.dyld.cache.tapi.TapiTbdImageVo;
import org.robovm.sdk.dyld.cache.tapi.TapiUtils;
import org.robovm.sdk.dyld.macho.MachOConsts;
import org.robovm.sdk.dyld.macho.MachOException;
import org.robovm.sdk.dyld.macho.cmds.DyldInfoCommand;
import org.robovm.sdk.dyld.macho.cmds.SymtabCommand;
import org.robovm.sdk.dyld.macho.structs.MachHeader;
import org.robovm.sdk.dyld.macho.structs.NList;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.*;

public class DyLdCache {

    private final static String OBJC_IVAR_PREFIX = "_OBJC_IVAR_$";
    private final static String OBJC_METACLASS_PREFIX = "_OBJC_METACLASS_$";
    private final static String OBJC_CLASS_PREFIX = "_OBJC_CLASS_$";

    private final ByteReader cacheFileReader;
    private final MappedByteReader cacheVmReader;
    private final DyldCacheHeader cacheHeader;
    private final String arch;


    public interface ProgressListener {
        void onImageBeingRead(String image, int imageIdx, int imageCnt);
    }

    public DyLdCache(File cache) throws MachOException {
        try {
            cacheFileReader = new FileByteReader(new RandomAccessFile(cache, "r"));
            cacheFileReader.order(ByteOrder.LITTLE_ENDIAN);
        } catch (IOException e) {
            throw new MachOException("Failed to open mach-o file", e);
        }

        // read header
        cacheHeader = new DyldCacheHeader(cacheFileReader);
        if (!cacheHeader.getMagic().startsWith("dyld_v1"))
            throw new MachOException("Broken cache header magic: " + cacheHeader.getMagic());
        arch = cacheHeader.getMagic().substring(cacheHeader.getMagic().lastIndexOf(' ') + 1);

        // get VM mapped reader
        cacheVmReader = createVmReader();
    }

    public Map<String, TapiTbdImageVo>  readImages(ProgressListener listener) throws MachOException {
        ByteReader.ArrayReader<DyLdCacheImageInfo> imagesReader = new ByteReader.ArrayReader<>(cacheFileReader, cacheHeader.getImagesOffset(),
                (int)cacheHeader.getImagesCount(), DyLdCacheImageInfo.OBJECT_READER(),
                true);

        // move through images now
        Map<String, TapiTbdImageVo> images = new LinkedHashMap<>();
        int idx = -1; // -1 as it being incremented in begining of the loop not end
        for (DyLdCacheImageInfo image : imagesReader) {
            idx += 1;
            TapiTbdImageVo vo = processImage(image);
            if (vo == null)
                continue;

            // add it to map
            images.put(vo.getInstallName(), vo);

            // update progress
            if (listener != null)
                listener.onImageBeingRead(vo.getInstallName(), idx , (int) cacheHeader.getImagesCount());
        }

        // all images has been read, resolve now dylib reexports
        for (TapiTbdImageVo image: images.values()) {
            resolveReexports(image, images, new HashSet<>());
        }

        // now there is a list of images
        // these are stored as with version identifiers, move through all of these and make an allias for each entry
        // e.g. libz.1.dylib <- libz.dylib
        Map<String, TapiTbdImageVo>  imagesWithoutVersions = new HashMap<>();
        for (Map.Entry<String, TapiTbdImageVo> e : images.entrySet()) {
            String imageName = e.getKey();
            if (!imageName.endsWith(".dylib") || !TapiUtils.isPublicLocation(imageName))
                continue;

            imageName = new File(imageName).getName();
            String[] chunks = imageName.split("\\.");
            if (chunks.length < 3) {
                // only name and ext
                continue;
            }

            // check chunks between idx 0 and last, these shall be numeric
            // e.g. libz.1.2.8.tbd > ["libz", "1", "2", "8", "tbd"], checking "1", "2", "8"
            // also cover symbol case, e.g. libSystem.B.dylib
            boolean isAllNumeric = true;
            boolean isAllAlpha = true;
            for (idx = 1; idx < chunks.length - 1; idx ++) {
                isAllNumeric &= TapiUtils.isStringCharsInRange(chunks[idx], '0', '9');
                isAllAlpha &= TapiUtils.isStringCharsInRange(chunks[idx], 'A', 'C');
            }

            if (!isAllNumeric && !isAllAlpha)
                continue;

            // name is subject for alias
            imageName = new File(e.getKey()).getParent() + "/" + chunks[0] + ".dylib";
            imagesWithoutVersions.put(imageName, e.getValue());
        }
        images.putAll(imagesWithoutVersions);

        // there is a list of libraries that are not present physicaly as file but their symbols are exported by libSystem
        // such as pthreads, make alias for these cases as well
        String hiddenLibs[] = new String[]{"/usr/lib/libc.dylib", "/usr/lib/libdbm.dylib",
                "/usr/lib/libdl.dylib", "/usr/lib/libinfo.dylib", "/usr/lib/libm.dylib", "/usr/lib/libpoll.dylib",
                "/usr/lib/libproc.dylib", "/usr/lib/libpthread.dylib", "/usr/lib/librpcsvc.dylib"};
        for (String hidden : hiddenLibs) {
            if (!images.containsKey(hidden)) {
                images.put(hidden, images.get("/usr/lib/libSystem.B.dylib"));
            }
        }

        return images;
    }

    /**
     * routine resolves reexport. all mentioned private dylibs shall be removed and their export symbols shall be added
     */
    private void resolveReexports(TapiTbdImageVo image, Map<String, TapiTbdImageVo> images, Set<TapiTbdImageVo> resolved) {
        if (resolved.contains(image))
            return;
        resolved.add(image);
        if (image.getReexports().isEmpty())
            return;

        List<String> toRemove = new ArrayList<>();
        List<String> toAdd = new ArrayList<>();
        for (String lib : image.getReexports()) {
            TapiTbdImageVo reexported = images.get(lib);
            if (reexported == null)
                throw new RuntimeException("Reexported " + lib + " not found in cache");
            if (TapiUtils.isPublicLocation(lib))
                continue;

            // resolveReexports for this library as well
            resolveReexports(reexported, images, resolved);

            // private lib, copy symbols
            image.getExportedSymbols().addAll(reexported.getExportedSymbols());
            image.getExportedObjcClasses().addAll(reexported.getExportedObjcClasses());
            image.getExportedObjcIvars().addAll(reexported.getExportedObjcIvars());
            image.getExportedWeakDefined().addAll(reexported.getExportedWeakDefined());

            toRemove.add(lib);
            toAdd.addAll(reexported.getReexports());
        }

        if (!toRemove.isEmpty())
            image.getReexports().removeAll(toRemove);
        if (!toAdd.isEmpty())
            image.getReexports().addAll(toAdd);
    }

    private TapiTbdImageVo processImage(DyLdCacheImageInfo image) throws MachOException {
        String imageName = cacheFileReader.readStringZ((int) image.getPathFileOffset());

//        if (!imageName.equals("/System/Library/Frameworks/Foundation.framework/Foundation")
////                && !imageName.equals("/System/Library/Frameworks/WebKit.framework/WebKit")
//                ){
//            return null;
//        }

        // filter out bundles
        if (imageName.contains(".bundle/")) {
            return null;
        }

        // create value object to store parsed data
        TapiTbdImageVo tbd = new TapiTbdImageVo(arch, imageName);

        // reading macho header, pick magic, mach header is located at memory offset
        cacheVmReader.setPosition(image.getAddress());
        long magic = cacheVmReader.readUnsignedInt32();
        if (magic != MachOConsts.MAGIC && magic != MachOConsts.MAGIC_64)
            throw new MachOException("unexpected Mach header MAGIC 0x" + Long.toHexString(magic));

        // parse header
        MachHeader header = new MachHeader(cacheVmReader, magic == MachOConsts.MAGIC_64);

        // read all commands
        for (int idx = 0; idx < header.ncmds(); idx++) {
            long pos = cacheVmReader.position();
            int cmd = (int) cacheVmReader.readUnsignedInt32();
            int cmdsize = (int) cacheVmReader.readUnsignedInt32();
            if (cmd == MachOConsts.commands.LC_SYMTAB) {
                SymtabCommand symtabCommand = new SymtabCommand(cacheVmReader);
                ByteReader.ObjectReader<NList> nlistObjReader = NList.OBJECT_READER(header.is64b());

                // mach-o header is in VM address space, but strings and symbol objects are in file space, crazy
                ByteReader stringReader = cacheFileReader.sliceAt((int) symtabCommand.stroff, (int) symtabCommand.strsize);
                ByteReader nlistReader = cacheFileReader.sliceAt((int) symtabCommand.symoff, (int) (symtabCommand.nsyms * nlistObjReader.objectSize()));
                ByteReader.ArrayReader<NList> nlistArrayReader = new ByteReader.ArrayReader<>(nlistReader, nlistObjReader);
                for (NList nlist : nlistArrayReader) {
                    if (nlist.isTypeUndfined() || nlist.isTypePreboundUndefined())
                        continue;
                    if (nlist.isTypeStab() && !nlist.isTypeStabGlobalSymb())
                        continue;

                    // get sym name
                    String sym = stringReader.readStringZ((int) nlist.n_strx());
                    addSymbolToTbd(sym, tbd, nlist);
                }
            } else if (cmd == MachOConsts.commands.LC_UUID) {
                // read 16 byte uuid
                byte[] bytes = new byte[16];
                cacheVmReader.get(bytes);
                UUID u = UUID.nameUUIDFromBytes(bytes);
                tbd.setUuid(u.toString());
            } else if (cmd == MachOConsts.commands.LC_REEXPORT_DYLIB ||
                    cmd == MachOConsts.commands.LC_ID_DYLIB) {
                long strOffset = cacheVmReader.readUnsignedInt32();
                long ts = cacheVmReader.readUnsignedInt32(); // timestamp
                long currentV = cacheVmReader.readUnsignedInt32();
                long compatV = cacheVmReader.readUnsignedInt32();
                String name = cacheVmReader.readStringZ(pos + strOffset);
                if (cmd == MachOConsts.commands.LC_ID_DYLIB) {
                    tbd.setCurrentVersion(encodedVersionToStr(currentV));
                    if (compatV != 0x10000)
                        tbd.setCompatVersion(encodedVersionToStr(compatV));
                } else {
                    tbd.addReexport(name);
                }
            } else if (cmd == MachOConsts.commands.LC_DYLD_INFO_ONLY) {
                DyldInfoCommand dyldInfo = new DyldInfoCommand(cacheVmReader);
                if (dyldInfo.export_size != 0) {
                    ByteReader trieReader = cacheFileReader.sliceAt((int) dyldInfo.export_off, dyldInfo.export_size);
                    parseIndirectSymbFromTrie(trieReader, "", tbd);
                }
            }

            cacheVmReader.setPosition(pos + cmdsize);
        }

        return tbd;
    }

    private void addSymbolToTbd(String sym, TapiTbdImageVo tbd, NList nlist) {
        if (sym != null && sym.length() > 0 && (sym.charAt(0) == '_' || Character.isAlphabetic(sym.charAt(0)))) {
            // all exported syms shall start with underscope
            if (sym.startsWith(OBJC_IVAR_PREFIX)) {
                tbd.addExportedObjcIvar(sym.substring(OBJC_IVAR_PREFIX.length()));
            } else if (sym.startsWith(OBJC_CLASS_PREFIX)) {
                tbd.addExportedObjcClass(sym.substring(OBJC_CLASS_PREFIX.length()));
            } else if (nlist != null && nlist.isWeakDefined()) {
                tbd.addExportedWeakDefined(sym);
            } else {
                tbd.addExportedSymbol(sym);
            }
        }
    }

    /**
     * converts macho version uint32_t to string presentation
     * @param v encoded as uint32_t
     * @return string presentation
     */
    private String encodedVersionToStr(long v) {
        // version is presented as 0xAAAABBCC -> aaaaa.bbb.ccc (in decimal)
        String version = "";
        int chunk = (int) (v & 0xFF);
        if (chunk != 0)
            version = "." + chunk;
        chunk = (int) ((v >> 8) & 0xFF);
        if (chunk != 0 || version.length() > 0)
            version = "." + chunk + version;
        chunk = (int) (v >> 16);
        version = chunk + version;
        return version;
    }

    private void parseIndirectSymbFromTrie(ByteReader reader, String name, TapiTbdImageVo tbd) {
        long savedPos;

         // Nodes for a symbol start with a uleb128 that is the length of
         // the exported symbol information for the string so far.
        long terminalSize = parseUleb128(reader);
        savedPos = reader.position();

        if ( terminalSize != 0 ) {
            // just get flag to check type, if it is reexported than this symbol is external and indirect, add to list
            long flags = parseUleb128(reader);
            if ((flags & MachOConsts.trie.EXPORT_SYMBOL_FLAGS_KIND_MASK) != MachOConsts.trie.EXPORT_SYMBOL_FLAGS_KIND_REGULAR ||
                    (flags & MachOConsts.trie.EXPORT_SYMBOL_FLAGS_REEXPORT) != 0) {
                addSymbolToTbd(name, tbd, null);
            }
        }

        reader.setPosition(savedPos + terminalSize);

        // After the optional exported symbol information is a byte of
        // how many edges (0-255) that this node has leaving it,
        // followed by each edge.
		byte childrenCount = reader.readByte();
		for (int i = 0; i < childrenCount; i++) {
            // Each edge is a zero terminated UTF8 of the addition chars
            // in the symbol, followed by a uleb128 offset for the node that
            // edge points to.
            String s = reader.readStringZ();
            long offset = parseUleb128(reader);
            savedPos = reader.position();
            reader.setPosition(offset);
            parseIndirectSymbFromTrie(reader, name + s, tbd);
            reader.setPosition(savedPos);
        }
    }

    private long parseUleb128(ByteReader reader) {
        long result = 0;
        int bit = 0;
        byte p;
        do {
            p = reader.readByte();
            long slice = p & 0x7f;

            result |= (slice << bit);
            bit += 7;
        } while ((p & 0x80) != 0);
        return result;
    }

    private MappedByteReader createVmReader() {
        ByteReader.ArrayReader<DyLdCacheMappingInfo> vmMappingReader = new ByteReader.ArrayReader<>(cacheFileReader,
                cacheHeader.getMappingOffset(), (int)cacheHeader.getMappingCount(),
                DyLdCacheMappingInfo.OBJECT_READER(), true);
        MappedByteReader.MappingEntry vmMappingEntries[] = new MappedByteReader.MappingEntry[(int) cacheHeader.getMappingCount()];
        int idx = 0;
        for (DyLdCacheMappingInfo mapInfo : vmMappingReader) {
            vmMappingEntries[idx++] = new MappedByteReader.MappingEntry(mapInfo.getAddress(),
                    mapInfo.getAddress() + mapInfo.getSize() - 1, mapInfo.getFileOffset());
        }
        return new MappedByteReader(cacheFileReader, vmMappingEntries);
    }

}
