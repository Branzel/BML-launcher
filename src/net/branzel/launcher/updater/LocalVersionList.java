package net.branzel.launcher.updater;

import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Set;
import net.branzel.launcher.Launcher;
import net.branzel.launcher.versions.CompleteVersion;
import net.branzel.launcher.versions.ReleaseType;
import net.branzel.launcher.versions.Version;
import net.minecraft.launcher.OperatingSystem;

public class LocalVersionList extends FileBasedVersionList
{
    private final File baseDirectory;
    private final File baseVersionsDir;

    public LocalVersionList(File baseDirectory)
    {
        if ((baseDirectory == null) || (!baseDirectory.isDirectory())) throw new IllegalArgumentException("Base directory is not a folder!");

        this.baseDirectory = baseDirectory;
        baseVersionsDir = new File(this.baseDirectory, "versions");
        if (!baseVersionsDir.isDirectory()) baseVersionsDir.mkdirs(); 
    }

    @Override
    protected InputStream getFileInputStream(String path)
      throws FileNotFoundException
    {
        return new FileInputStream(new File(baseDirectory, path));
    }

    @Override
    public void refreshVersions() throws IOException
    {
        clearCache();

        File[] files = baseVersionsDir.listFiles();
        if (files == null) return;

        for (File directory : files) {
            String id = directory.getName();
            File jsonFile = new File(directory, id + ".json");

            if ((!directory.isDirectory()) || (!jsonFile.exists())) continue;
            try {
                String path = "versions/" + id + "/" + id + ".json";
                CompleteVersion version = (CompleteVersion)gson.fromJson(getContent(path), CompleteVersion.class);

                if (version.getId().equals(id))
                    addVersion(version);
                else if (Launcher.getInstance() != null)
                    Launcher.getInstance().println("Ignoring: " + path + "; it contains id: '" + version.getId() + "' expected '" + id + "'");
            } catch (RuntimeException ex) {
                if (Launcher.getInstance() != null)
                    Launcher.getInstance().println("Couldn't load local version " + jsonFile.getAbsolutePath(), ex);
                else {
                    throw new JsonSyntaxException("Loading file: " + jsonFile.toString(), ex);
                }
            }
        }

        for (Version version : getVersions()) {
            ReleaseType type = version.getType();

            if ((getLatestVersion(type) == null) || (getLatestVersion(type).getUpdatedTime().before(version.getUpdatedTime())))
                setLatestVersion(version);
        }
    }

    public void saveVersionList() throws IOException
    {
        String text = serializeVersionList();
        try (PrintWriter writer = new PrintWriter(new File(baseVersionsDir, "versions.json"))) {
            writer.print(text);
        }
    }

    public void saveVersion(CompleteVersion version) throws IOException {
        String text = serializeVersion(version);
        File target = new File(baseVersionsDir, version.getId() + "/" + version.getId() + ".json");
        if (target.getParentFile() != null) target.getParentFile().mkdirs();
        try (PrintWriter writer = new PrintWriter(target)) {
            writer.print(text);
        }
    }

    public File getBaseDirectory() {
        return baseDirectory;
    }

    @Override
    public boolean hasAllFiles(CompleteVersion version, OperatingSystem os)
    {
        Set<String> files = version.getRequiredFiles(os);

        for (String file : files) {
          if (!new File(baseDirectory, file).isFile()) {
            return false;
          }
        }

        return true;
    }
}
