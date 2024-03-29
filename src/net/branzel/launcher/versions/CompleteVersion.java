package net.branzel.launcher.versions;

import java.io.File;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.launcher.OperatingSystem;
import net.branzel.launcher.updater.download.Downloadable;
import net.minecraft.launcher.versions.Library;
import net.minecraft.launcher.versions.Rule;

public class CompleteVersion implements Version
{
    private String id;
    private Date time;
    private Date releaseTime;
    private ReleaseType type;
    private String minecraftArguments;
    private List<Library> libraries;
    private String mainClass;
    private int minimumLauncherVersion;
    private String incompatibilityReason;
    private List<Rule> rules;
    private volatile boolean synced = false;

    public CompleteVersion() { }

    public CompleteVersion(String id, Date releaseTime, Date updateTime, ReleaseType type, String mainClass, String minecraftArguments) {
        if ((id == null) || (id.length() == 0)) throw new IllegalArgumentException("ID cannot be null or empty");
        if (releaseTime == null) throw new IllegalArgumentException("Release time cannot be null");
        if (updateTime == null) throw new IllegalArgumentException("Update time cannot be null");
        if (type == null) throw new IllegalArgumentException("Release type cannot be null");
        if ((mainClass == null) || (mainClass.length() == 0)) throw new IllegalArgumentException("Main class cannot be null or empty");
        if (minecraftArguments == null) throw new IllegalArgumentException("Process arguments cannot be null or empty");

        this.id = id;
        this.releaseTime = releaseTime;
        time = updateTime;
        this.type = type;
        this.mainClass = mainClass;
        libraries = new ArrayList();
        this.minecraftArguments = minecraftArguments;
    }

    public CompleteVersion(CompleteVersion version) {
        this(version.getId(), version.getReleaseTime(), version.getUpdatedTime(), version.getType(), version.getMainClass(), version.getMinecraftArguments());
    }

    public CompleteVersion(Version version, String mainClass, String minecraftArguments) {
        this(version.getId(), version.getReleaseTime(), version.getUpdatedTime(), version.getType(), mainClass, minecraftArguments);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ReleaseType getType() {
        return type;
    }

    @Override
    public Date getUpdatedTime() {
        return time;
    }

    @Override
    public Date getReleaseTime() {
        return releaseTime;
    }

    public Collection<Library> getLibraries() {
        return libraries;
    }

    public String getMainClass() {
        return mainClass;
    }

    @Override
    public void setUpdatedTime(Date time) {
        if (time == null) throw new IllegalArgumentException("Time cannot be null");
        this.time = time;
    }

    @Override
    public void setReleaseTime(Date time) {
        if (time == null) throw new IllegalArgumentException("Time cannot be null");
        releaseTime = time;
    }

    @Override
    public void setType(ReleaseType type) {
        if (type == null) throw new IllegalArgumentException("Release type cannot be null");
        this.type = type;
    }

    public void setMainClass(String mainClass) {
        if ((mainClass == null) || (mainClass.length() == 0)) throw new IllegalArgumentException("Main class cannot be null or empty");
        this.mainClass = mainClass;
    }

    public Collection<Library> getRelevantLibraries() {
        List result = new ArrayList();

        for (Library library : libraries) {
            if (library.appliesToCurrentEnvironment()) {
                result.add(library);
            }
        }

        return result;
    }

    public Collection<File> getClassPath(OperatingSystem os, File base) {
        Collection<Library> librariescol = getRelevantLibraries();
        Collection result = new ArrayList();

        for (Library library : librariescol) {
            if (library.getNatives() == null) {
                result.add(new File(base, "libraries/" + library.getArtifactPath()));
            }
        }

        result.add(new File(base, "versions/" + getId() + "/" + getId() + ".jar"));

        return result;
    }

    public Collection<String> getExtractFiles(OperatingSystem os) {
        Collection<Library> librariescol = getRelevantLibraries();
        Collection result = new ArrayList();

        for (Library library : librariescol) {
            Map natives = library.getNatives();

            if ((natives != null) && (natives.containsKey(os))) {
                result.add("libraries/" + library.getArtifactPath((String)natives.get(os)));
            }
        }

        return result;
    }

    public Set<String> getRequiredFiles(OperatingSystem os) {
        Set neededFiles = new HashSet();

        for (Library library : getRelevantLibraries()) {
            if (library.getNatives() != null) {
                  String natives = (String)library.getNatives().get(os);
                  if (natives != null) neededFiles.add("libraries/" + library.getArtifactPath(natives)); 
            }
            else {
                  neededFiles.add("libraries/" + library.getArtifactPath());
            }
        }

        return neededFiles;
    }

    public Set<Downloadable> getRequiredDownloadables(OperatingSystem os, Proxy proxy, File targetDirectory, boolean ignoreLocalFiles) throws MalformedURLException {
        Set neededFiles = new HashSet();

        for (Library library : getRelevantLibraries()) {
            String file = null;

            if (library.getNatives() != null) {
                String natives = (String)library.getNatives().get(os);
                if (natives != null)
                    file = library.getArtifactPath(natives);
            }
            else {
                file = library.getArtifactPath();
            }

            if (file != null) {
                URL url = new URL(library.getDownloadUrl() + file);
                File local = new File(targetDirectory, "libraries/" + file);

                if ((!local.isFile()) || (!library.hasCustomUrl())) {
                    neededFiles.add(new Downloadable(proxy, url, local, ignoreLocalFiles));
                }
            }
        }

        return neededFiles;
    }

    @Override
    public String toString() {
        return "CompleteVersion{id='" + id + '\'' + ", time=" + time + ", type=" + type + ", libraries=" + libraries + ", mainClass='" + mainClass + '\'' + ", minimumLauncherVersion=" + minimumLauncherVersion + '}';
    }

    public String getMinecraftArguments() {
        return minecraftArguments;
    }

    public void setMinecraftArguments(String minecraftArguments) {
        if (minecraftArguments == null) throw new IllegalArgumentException("Process arguments cannot be null or empty");
        this.minecraftArguments = minecraftArguments;
    }

    public int getMinimumLauncherVersion() {
        return minimumLauncherVersion;
    }

    public void setMinimumLauncherVersion(int minimumLauncherVersion) {
        this.minimumLauncherVersion = minimumLauncherVersion;
    }

    public boolean appliesToCurrentEnvironment() {
        if (rules == null) return true;
        Rule.Action lastAction = Rule.Action.DISALLOW;

        for (Rule rule : rules) {
            Rule.Action action = rule.getAppliedAction();
            if (action != null) lastAction = action;
        }

        return lastAction == Rule.Action.ALLOW;
    }

    public String getIncompatibilityReason() {
        return incompatibilityReason;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }
}