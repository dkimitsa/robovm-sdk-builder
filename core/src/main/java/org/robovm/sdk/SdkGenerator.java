package org.robovm.sdk;

import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListParser;
import org.robovm.sdk.dyld.cache.DyLdCache;
import org.robovm.sdk.dyld.cache.tapi.TapiTbdImageVo;
import org.robovm.sdk.dyld.cache.tapi.TapiTbdYamlExporter;
import org.robovm.sdk.dyld.macho.MachOException;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class SdkGenerator {

    public interface ProgressListener {
        void progressOut(String msg, float progress);
    }

    public static void generateSdk(File cacheFile64b, File systemVersionPlist, File outputDir, ProgressListener listener ) throws MachOException, Exception {
        // de-cache everything        listener.progressOut("Generating meta-files...", 1f);
        DyLdCache cache = new DyLdCache(cacheFile64b);
        Map<String, TapiTbdImageVo> images = cache.readImages((image, imageIdx, imageCnt) -> {
            listener.progressOut("Reading: (" + imageIdx + "/" + imageCnt + ") " + image, 0.5f * imageIdx / imageCnt);
        });

        // folder to put everything there
        File xcodeOutputDir = new File(outputDir,"Xcode.app");
        if (xcodeOutputDir.exists())
            deleteDirectory(xcodeOutputDir);
        forceMkdir(xcodeOutputDir);

        // dump to files
        File tblExportDir = new File(xcodeOutputDir, "Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS.sdk");
        forceMkdir(tblExportDir);
        TapiTbdYamlExporter.exportAll(tblExportDir, images, Arrays.asList("arm64", "armv7", "armv7s"), (image, imageIdx, imageCnt) -> {
            listener.progressOut("Exporting: (" + imageIdx + "/" + imageCnt + ") " + image, 0.5f + 0.5f * imageIdx / imageCnt);
        });

        // generate meta files
        listener.progressOut("Generating meta-files...", 1f);
        generateMetaFiles(systemVersionPlist, xcodeOutputDir);


        // pack to zip
        listener.progressOut("Packing to zip...", 1f);
        ZipUtil.pack(xcodeOutputDir, new File(outputDir, "xcode.zip"), true);

        listener.progressOut("done!", 1f);
    }

    private static void deleteDirectory(File dir) throws IOException {
        if (!dir.exists())
            return;
        if (!dir.isDirectory())
            throw new IOException("Not a directory " + dir);
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory())
                    deleteDirectory(f);
                else
                    if (!f.delete())
                        throw new IOException("Failed to delete file " + f);
            }
        }

        if (!dir.delete())
            throw new IOException("Failed to delete dir " + dir);
    }

    private static void forceMkdir(File dir) throws IOException {
        if (!(dir.exists() && dir.isDirectory()) &&  !dir.mkdirs())
            throw new IOException("Failed to create " + dir);
    }


    private static void generateMetaFiles(File systemVersionPlist, File xcodeOutputDir) throws Exception {
        NSDictionary dict = (NSDictionary) PropertyListParser.parse(systemVersionPlist);
        String ProductBuildVersion = dict.get("ProductBuildVersion").toString(); // e.g. 15C107
        String ProductVersion = dict.get("ProductVersion").toString(); // e.g. 11.0

        // create folder for SDK root
        File sdkRoot = new File(xcodeOutputDir, "Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS.sdk");
        forceMkdir(sdkRoot);

        // files that going to be written
        File sdkSettingsFile = new File(sdkRoot, "SDKSettings.plist");
        File sdkSysVersionFile = new File(sdkRoot, "System/Library/CoreServices/SystemVersion.plist");
        File platformInfoFile = new File(sdkRoot, "../../../Info.plist");
        File xcodeVersionPListFile = new File(xcodeOutputDir, "version.plist");
        File xcodeInfoPListFile = new File(xcodeOutputDir, "Info.plist");


        // SDKSettings.plist
        dict = new NSDictionary();
        dict.put("DisplayName", "iOS " + ProductVersion); // e.g. "iOS 11.0"
        dict.put("MinimalDisplayName", ProductVersion); // e.g. "11.0"
        dict.put("CanonicalName", "iphoneos" + ProductVersion); // e.g.: iphoneos11.0
        dict.put("Version", ProductVersion); // e.g. "11.0"
        NSDictionary subDict = new NSDictionary();
        subDict.put("SUPPORTED_DEVICE_FAMILIES", "1,2");
        dict.put("DefaultProperties", subDict); // e,g, {SUPPORTED_DEVICE_FAMILIES : "1,2"}
        forceMkdir(sdkSettingsFile.getParentFile());
        PropertyListParser.saveAsXML(dict, sdkSettingsFile);

        // System/Library/CoreServices/SystemVersion.plist
        dict = new NSDictionary();
        dict.put("ProductBuildVersion", ProductBuildVersion); // e.g. "15A372"
        forceMkdir(sdkSysVersionFile.getParentFile());
        PropertyListParser.saveAsXML(dict, sdkSysVersionFile);

        // ../../../Info.plist
        dict = new NSDictionary();
        subDict = new NSDictionary();
        subDict.put("DTPlatformVersion", ProductVersion);
        subDict.put("DTPlatformName", "iphoneos");
        dict.put("AdditionalInfo", subDict); // e,g, {SUPPORTED_DEVICE_FAMILIES : "1,2"}
        PropertyListParser.saveAsXML(dict, platformInfoFile);

        // xcode meta files -- as in xcode 9.2
        dict = new NSDictionary();
        dict.put("DTXcode", "0920");
        PropertyListParser.saveAsXML(dict, xcodeInfoPListFile);
        dict = new NSDictionary();
        dict.put("ProductBuildVersion", "9C40b");
        PropertyListParser.saveAsXML(dict, xcodeVersionPListFile);
    }



    public static void main(String[] argv) throws Exception {
        // Allows to generate SDK files if dyld_shared_cache_arm64 and SystemVersion.plist has been downloaded from iOS devices

        try {
            if (argv.length != 3) {
                System.out.println("Usage: <path to 64bit dyld cach> <path to SystemVersion.plist> <output dir>");
                System.exit(-1);
            }

            generateSdk(new File(argv[0]), new File(argv[1]), new File(argv[2]), (msg, progress) -> System.out.println(msg));
        } catch (MachOException e) {
            e.printStackTrace();
        }
    }
}
