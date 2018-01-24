package org.robovm.sdk.dyld.cache.structs;

import org.robovm.sdk.dyld.bytereader.ByteReader;

public class DyldCacheHeader {

    private String magic;       //    char        magic[16];              // e.g. "dyld_v0    i386"
    private long mappingOffset; //    uint32_t    mappingOffset;          // file offset to first dyld_cache_mapping_info
    private long mappingCount;  //    uint32_t    mappingCount;           // number of dyld_cache_mapping_info entries
    private long imagesOffset;  //    uint32_t    imagesOffset;           // file offset to first dyld_cache_image_info
    private long imagesCount;   //    uint32_t    imagesCount;            // number of dyld_cache_image_info entries
    private long dyldBaseAddress;   //    uint64_t    dyldBaseAddress;        // base address of dyld when cache was built

    public DyldCacheHeader(ByteReader reader) {
        // read interested fields
        magic = reader.readString(16);
        mappingOffset = reader.readUnsignedInt32();
        mappingCount = reader.readUnsignedInt32();
        imagesOffset = reader.readUnsignedInt32();
        imagesCount = reader.readUnsignedInt32();
        dyldBaseAddress = reader.readLong();
    }

    //    uint64_t    codeSignatureOffset;    // file offset of code signature blob
//    uint64_t    codeSignatureSize;      // size of code signature blob (zero means to end of file)
//    uint64_t    slideInfoOffset;        // file offset of kernel slid info
//    uint64_t    slideInfoSize;          // size of kernel slid info
//    uint64_t    localSymbolsOffset;     // file offset of where local symbols are stored
//    uint64_t    localSymbolsSize;       // size of local symbols information
//    uint8_t     uuid[16];               // unique value for each shared cache file
//    uint64_t    cacheType;              // 0 for development, 1 for production
//    uint32_t    branchPoolsOffset;      // file offset to table of uint64_t pool addresses
//    uint32_t    branchPoolsCount;       // number of uint64_t entries
//    uint64_t    accelerateInfoAddr;     // (unslid) address of optimization info
//    uint64_t    accelerateInfoSize;     // size of optimization info
//    uint64_t    imagesTextOffset;       // file offset to first dyld_cache_image_text_info
//    uint64_t    imagesTextCount;        // number of dyld_cache_image_text_info entries
//    uint64_t    dylibsImageGroupAddr;   // (unslid) address of ImageGroup for dylibs in this cache
//    uint64_t    dylibsImageGroupSize;   // size of ImageGroup for dylibs in this cache
//    uint64_t    otherImageGroupAddr;    // (unslid) address of ImageGroup for other OS dylibs
//    uint64_t    otherImageGroupSize;    // size of oImageGroup for other OS dylibs
//    uint64_t    progClosuresAddr;       // (unslid) address of list of program launch closures
//    uint64_t    progClosuresSize;       // size of list of program launch closures
//    uint64_t    progClosuresTrieAddr;   // (unslid) address of trie of indexes into program launch closures
//    uint64_t    progClosuresTrieSize;   // size of trie of indexes into program launch closures
//    uint32_t    platform;               // platform number (macOS=1, etc)
//    uint32_t    formatVersion        : 8, // launch_cache::binary_format::kFormatVersion
//    dylibsExpectedOnDisk : 1, // dyld should expect the dylib exists on disk and to compare inode/mtime to see if cache is valid
//    simulator            : 1; // for simulator of specified platform
//    uint64_t    sharedRegionStart;      // base load address of cache if not slid
//    uint64_t    sharedRegionSize;       // overall size of region cache can be mapped into
//    uint64_t    maxSlide;               // runtime slide of cache can be between zero and this value


    public String getMagic() {
        return magic;
    }

    public long getMappingOffset() {
        return mappingOffset;
    }

    public long getMappingCount() {
        return mappingCount;
    }

    public long getImagesOffset() {
        return imagesOffset;
    }

    public long getImagesCount() {
        return imagesCount;
    }

    public long getDyldBaseAddress() {
        return dyldBaseAddress;
    }
}
