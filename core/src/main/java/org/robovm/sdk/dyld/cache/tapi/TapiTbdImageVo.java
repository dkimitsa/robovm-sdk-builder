package org.robovm.sdk.dyld.cache.tapi;

import java.util.LinkedHashSet;
import java.util.Set;

public class TapiTbdImageVo {
    private final String arch;
    private final String installName;
    private String uuid;
    private String platform = "ios";
    private String currentVersion;
    private String compatVersion;
    private String objcConstraint = "none";
    private final Set<String> exportedSymbols = new LinkedHashSet<>();
    private final Set<String> exportedObjcClasses = new LinkedHashSet<>();
    private final Set<String> exportedObjcIvars = new LinkedHashSet<>();
    private final Set<String> reexports = new LinkedHashSet<>();
    private final Set<String> exportedWeakDefined = new LinkedHashSet<>();

    // for batch processing
    private Set exportedItems[] = {exportedSymbols, exportedObjcClasses, exportedObjcIvars, reexports, exportedWeakDefined};
    private final static String exportedItemNames[] = {"symbols", "objc-classes", "objc-ivars", "re-exports", "weak-def-symbols"};

    public TapiTbdImageVo(String arch, String installName) {
        this.arch = arch;
        this.installName = installName;
    }

    public String getArch() {
        return arch;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getInstallName() {
        return installName;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getCompatVersion() {
        return compatVersion;
    }

    public void setCompatVersion(String compatVersion) {
        this.compatVersion = compatVersion;
    }

    public String getObjcConstraint() {
        return objcConstraint;
    }

    public void setObjcConstraint(String objcConstraint) {
        this.objcConstraint = objcConstraint;
    }

    public Set<String> getExportedSymbols() {
        return exportedSymbols;
    }

    public void addExportedSymbol(String e) {
        this.exportedSymbols.add(e);
    }

    public Set<String> getExportedObjcClasses() {
        return exportedObjcClasses;
    }

    public void addExportedObjcClass(String e) {
        this.exportedObjcClasses.add(e);
    }

    public Set<String> getExportedObjcIvars() {
        return exportedObjcIvars;
    }

    public void addExportedObjcIvar(String e ) {
        this.exportedObjcIvars.add(e);
    }

    public Set<String> getReexports() {
        return reexports;
    }

    public void addReexport(String e) {
        this.reexports.add(e);
    }

    public Set<String> getExportedWeakDefined() {
        return exportedWeakDefined;
    }

    public void addExportedWeakDefined(String e) {
        this.exportedWeakDefined.add(e);
    }

    /**
     * returns export list by it index, for batch processing
     */
    public Set<String> getExportItems(int idx) {
        //noinspection unchecked
        return exportedItems[idx];
    }

    public static int getExportItemCount() {
        return 5;
    }

    public static String getExportItemName(int idx) {
        return exportedItemNames[idx];
    }
}
