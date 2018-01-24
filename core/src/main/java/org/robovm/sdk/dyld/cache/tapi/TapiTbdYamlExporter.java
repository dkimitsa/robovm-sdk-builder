package org.robovm.sdk.dyld.cache.tapi;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Export tbd images into yaml file
 *
 * @author Demyan Kimitsa
 */
public class TapiTbdYamlExporter {
    public interface ProgressListener {
        void onImageExported(String image, int imageIdx, int imageCnt);
    }

    public static void exportAll(File rootDir, Map<String, TapiTbdImageVo> images, List<String> archs, ProgressListener listener) throws IOException {

        // configure yaml -- with stream start "---", stream end "..." and tag root hashmap as "!tapi-tbd-v2"
        DumperOptions options = new DumperOptions();
        options.setExplicitStart(true);
        options.setExplicitEnd(true);
        Representer representer = new Representer();
        representer.addClassTag(YamlHashTable.class, new Tag("!tapi-tbd-v2"));
        Yaml yaml = new Yaml(representer, options);

        // move through all images, process and export as yaml
        int idx = -1; // -1 as it being incremented in begining of the loop not end
        for (String imageName : images.keySet()) {
            idx += 1;
            if (!TapiUtils.isPublicLocation(imageName))
                continue;

            if (listener != null)
                listener.onImageExported(imageName, idx, images.size());

            // prepare to write to file
            String tblName;
            if (imageName.endsWith(".dylib"))
                tblName = imageName.replace(".dylib", ".tbd");
            else
                tblName = imageName + ".tbd";

            File tblFile = new File(rootDir, tblName);
            if (!tblFile.getParentFile().exists() && !tblFile.getParentFile().mkdirs())
                throw new IOException("Ubable to create directories to " + tblFile);
            try (FileWriter tblWriter = new FileWriter(tblFile)) {
                // combine all platforms into one hash
                Map<String, ?> yamlMap = buildImageYamlMap(imageName, images, archs);
                yaml.dump(yamlMap, tblWriter);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> buildImageYamlMap(String imageName, Map<String, TapiTbdImageVo> images, List<String> archs) {
        TapiTbdImageVo image = images.get(imageName);

        // build list of uuids
        List<String> uuids = new ArrayList<>();
        for (String arch : archs) {
            uuids.add(arch + ": " + UUID.randomUUID());
        }

        // all data will go here
        Map<String, Object> yaml = new YamlHashTable<>();
        yaml.put("archs", archs);
        yaml.put("uuids", uuids);
        yaml.put("platform", "ios");
        yaml.put("install-name", image.getInstallName());
        yaml.put("current-version", image.getCurrentVersion());
        if (image.getCompatVersion() != null)
            yaml.put("compatibility-version", image.getCompatVersion());
        yaml.put("objc-constraint", image.getObjcConstraint());

        // process all exported items into exports array
        List<Map<String, ?>> exportsList = new ArrayList<>();
        yaml.put("exports", exportsList);
        for (int exportedItemType = 0; exportedItemType < TapiTbdImageVo.getExportItemCount(); exportedItemType++) {
            Set<String> exportItems = image.getExportItems(exportedItemType);
            if (exportItems.isEmpty())
                continue;

            // dump all export item types into map
            Map<String, Object> exportMap = new LinkedHashMap<>();
            exportMap.put("archs", new ArrayList(archs));
            List<String> exportItemsList = new ArrayList<>(exportItems);
            Collections.sort(exportItemsList);
            exportMap.put(TapiTbdImageVo.getExportItemName(exportedItemType), exportItemsList);
            exportsList.add(exportMap);
        }
        return yaml;
    }

    /**
     * custom hashmap subclass to be able to attach to it 'tapi-tbd-v2' tag
     */
    private static class YamlHashTable<K, V> extends LinkedHashMap<K, V> {
    }
}
