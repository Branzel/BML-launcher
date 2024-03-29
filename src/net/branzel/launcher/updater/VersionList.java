package net.branzel.launcher.updater;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.branzel.launcher.versions.CompleteVersion;
import net.branzel.launcher.versions.PartialVersion;
import net.branzel.launcher.versions.ReleaseType;
import net.branzel.launcher.versions.Version;
import net.minecraft.launcher.OperatingSystem;
import net.minecraft.launcher.updater.DateTypeAdapter;
import net.minecraft.launcher.updater.LowerCaseEnumTypeAdapterFactory;

public abstract class VersionList {
    protected final Gson gson;
    private final Map<String, Version> versionsByName = new HashMap();
    private final List<Version> versions = new ArrayList();
    private final Map<ReleaseType, Version> latestVersions = new EnumMap(ReleaseType.class);
    
    public VersionList() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory());
        builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
        builder.enableComplexMapKeySerialization();
        builder.setPrettyPrinting();

        gson = builder.create();
    }
    
    public Collection<Version> getVersions() {
        return versions;
    }

    public Version getLatestVersion(ReleaseType type) {
        if (type == null) throw new IllegalArgumentException("Type cannot be null");
        return (Version)latestVersions.get(type);
    }

    public Version getVersion(String name) {
        if ((name == null) || (name.length() == 0)) throw new IllegalArgumentException("Name cannot be null or empty");
        return (Version)versionsByName.get(name);
    }

    public CompleteVersion getCompleteVersion(String name) throws IOException {
        if ((name == null) || (name.length() == 0)) throw new IllegalArgumentException("Name cannot be null or empty");
        Version version = getVersion(name);
        if (version == null) throw new IllegalArgumentException("Unknown version - cannot get complete version of null");
        return getCompleteVersion(version);
    }

    public CompleteVersion getCompleteVersion(Version version) throws IOException {
        if ((version instanceof CompleteVersion)) return (CompleteVersion)version;
        if (version == null) throw new IllegalArgumentException("Version cannot be null");

        CompleteVersion complete = (CompleteVersion)gson.fromJson(getContent("versions/" + version.getId() + "/" + version.getId() + ".json"), CompleteVersion.class);
        ReleaseType type = version.getType();

        Collections.replaceAll(versions, version, complete);
        versionsByName.put(version.getId(), complete);

        if (latestVersions.get(type) == version) {
            latestVersions.put(type, complete);
        }

        return complete;
    }

    protected void clearCache() {
        versionsByName.clear();
        versions.clear();
        latestVersions.clear();
    }

    public void refreshVersions() throws IOException {
        clearCache();

        RawVersionList versionList = (RawVersionList)gson.fromJson(getContent("versions/versions.json"), RawVersionList.class);
        System.out.println("Adding versions");

        for (Version version : versionList.getVersions()) {
            versions.add(version);
            versionsByName.put(version.getId(), version);
            System.out.println("Adding version: " + version);
        }

        for (ReleaseType type : ReleaseType.values())
            latestVersions.put(type, versionsByName.get(versionList.getLatestVersions().get(type)));
    }

    public CompleteVersion addVersion(CompleteVersion version) {
        if (version.getId() == null) throw new IllegalArgumentException("Cannot add blank version");
        if (getVersion(version.getId()) != null) throw new IllegalArgumentException("Version '" + version.getId() + "' is already tracked");

        versions.add(version);
        versionsByName.put(version.getId(), version);

        return version;
    }

    public void removeVersion(String name) {
        if ((name == null) || (name.length() == 0)) throw new IllegalArgumentException("Name cannot be null or empty");
        Version version = getVersion(name);
        if (version == null) throw new IllegalArgumentException("Unknown version - cannot remove null");
        removeVersion(version);
    }

    public void removeVersion(Version version) {
        if (version == null) throw new IllegalArgumentException("Cannot remove null version");
        versions.remove(version);
        versionsByName.remove(version.getId());

        for (ReleaseType type : ReleaseType.values())
            if (getLatestVersion(type) == version)
                latestVersions.remove(type);
    }

    public void setLatestVersion(Version version) {
        if (version == null) throw new IllegalArgumentException("Cannot set latest version to null");
        latestVersions.put(version.getType(), version);
    }

    public void setLatestVersion(String name) {
        if ((name == null) || (name.length() == 0)) throw new IllegalArgumentException("Name cannot be null or empty");
        Version version = getVersion(name);
        if (version == null) throw new IllegalArgumentException("Unknown version - cannot set latest version to null");
        setLatestVersion(version);
    }

    public String serializeVersionList() {
        RawVersionList list = new RawVersionList();

        for (ReleaseType type : ReleaseType.values()) {
            Version latest = getLatestVersion(type);
            if (latest != null) {
                list.getLatestVersions().put(type, latest.getId());
            }
        }

        for (Version version : getVersions()) {
            PartialVersion partial;

            if ((version instanceof PartialVersion))
                partial = (PartialVersion)version;
            else {
                partial = new PartialVersion(version);
            }

            list.getVersions().add(partial);
        }

        return gson.toJson(list);
    }

    public String serializeVersion(CompleteVersion version) {
        if (version == null) throw new IllegalArgumentException("Cannot serialize null!");
        return gson.toJson(version);
    }
    
    public abstract boolean hasAllFiles(CompleteVersion paramCompleteVersion, OperatingSystem paramOperatingSystem);

    protected abstract String getContent(String paramString) throws IOException;

    private static class RawVersionList { private List<PartialVersion> versions = new ArrayList();
        private Map<ReleaseType, String> latest = new EnumMap(ReleaseType.class);

        public List<PartialVersion> getVersions() {
            return versions;
        }

        public Map<ReleaseType, String> getLatestVersions() {
            return latest;
        }
    }
}
