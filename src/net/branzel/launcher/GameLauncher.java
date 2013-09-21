package net.branzel.launcher;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.branzel.launcher.process.JavaProcess;
import net.branzel.launcher.process.JavaProcessLauncher;
import net.branzel.launcher.process.JavaProcessRunnable;
import net.branzel.launcher.ui.console.CrashReportTab;
import net.branzel.launcher.updater.LocalVersionList;
import net.branzel.launcher.updater.VersionList;
import net.branzel.launcher.updater.VersionSyncInfo;
import net.branzel.launcher.updater.download.DownloadJob;
import net.branzel.launcher.updater.download.DownloadListener;
import net.branzel.launcher.updater.download.Downloadable;
import net.branzel.launcher.versions.CompleteVersion;
import net.minecraft.launcher.OperatingSystem;
import net.minecraft.launcher.versions.ExtractRules;
import net.minecraft.launcher.versions.Library;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.lang3.text.StrSubstitutor;

/**
 *
 * @author Branzel
 */
public class GameLauncher implements JavaProcessRunnable, DownloadListener {
    private final Object lock = new Object();
    private final Launcher launcher;
    private final List<DownloadJob> jobs = new ArrayList();
    private CompleteVersion version;
    private boolean isWorking;
    private File nativeDir;
    
    private void setWorking(boolean working) {
        synchronized (lock) {
            if (nativeDir != null) {
                Launcher.getInstance().println("Deleting " + nativeDir);
                if ((!nativeDir.isDirectory()) || (FileUtils.deleteQuietly(nativeDir))) {
                    nativeDir = null;
                } else {
                    Launcher.getInstance().println("Couldn't delete " + nativeDir + " - scheduling for deletion upon exit");
                    try {
                        FileUtils.forceDeleteOnExit(nativeDir);
                    } catch (Throwable localThrowable) { }
                }
            }
            isWorking = working;
            SwingUtilities.invokeLater(new Runnable()
            {
              @Override
              public void run() {
                    launcher.getRightBarPanel().getPlayButtonPanel().checkState();
              } } );
        }
    }
    
    public boolean isWorking() {
        return isWorking;
    }
    
    public GameLauncher(Launcher launcher) {
        this.launcher = launcher;
    }

    public void playGame() {
        synchronized (lock) {
            if (isWorking) {
                launcher.println("Tried to play game but game is already starting!");
                return;
            }

            setWorking(true);
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                launcher.getLauncherTabPanel().showConsole();
            }
        });
        launcher.println("Getting syncinfo for selected version");

        String lastVersionId = launcher.getLastVersionId();
        VersionSyncInfo syncInfo = null;

        if (lastVersionId != null) {
            syncInfo = launcher.getVersionManager().getVersionSyncInfo(lastVersionId);
        }

        if ((syncInfo == null) || (syncInfo.getLatestVersion() == null)) {
            syncInfo = (VersionSyncInfo)launcher.getVersionManager().getVersions().get(0);
        }

        if (syncInfo == null) {
            Launcher.getInstance().println("Tried to launch a version without a version being selected...");
            setWorking(false);
            return;
        }

        synchronized (lock) {
            launcher.println("Queueing library & version downloads");
            try {
                version = launcher.getVersionManager().getLatestCompleteVersion(syncInfo);
            } catch (IOException e) {
                Launcher.getInstance().println("Couldn't get complete version info for " + syncInfo.getLatestVersion(), e);
                setWorking(false);
                return;
            }

            if ((syncInfo.getRemoteVersion() != null) && (syncInfo.getLatestSource() != VersionSyncInfo.VersionSource.REMOTE) && (!version.isSynced())) {
                try {
                    CompleteVersion remoteVersion = launcher.getVersionManager().getRemoteVersionList().getCompleteVersion(syncInfo.getRemoteVersion());
                    launcher.getVersionManager().getLocalVersionList().removeVersion(version);
                    launcher.getVersionManager().getLocalVersionList().addVersion(remoteVersion);
                    ((LocalVersionList)launcher.getVersionManager().getLocalVersionList()).saveVersion(remoteVersion);
                    version = remoteVersion;
                } catch (IOException e) {
                    Launcher.getInstance().println("Couldn't sync local and remote versions", e);
                }
                version.setSynced(true);
            }

            if (!version.appliesToCurrentEnvironment()) {
                String reason = version.getIncompatibilityReason();
                if (reason == null) reason = "This version is incompatible with your computer. Please try another one by going into Edit Profile and selecting one through the dropdown. Sorry!";
                Launcher.getInstance().println("Version " + version.getId() + " is incompatible with current environment: " + reason);
                JOptionPane.showMessageDialog(launcher.getFrame(), reason, "Cannot play game", 0);
                setWorking(false);
                return;
            }

            if (version.getMinimumLauncherVersion() > 7) {
                Launcher.getInstance().println("An update to your launcher is available and is required to play " + version.getId() + ". Please restart your launcher.");
                setWorking(false);
                return;
            }

            if (!syncInfo.isInstalled()) {
                try {
                    VersionList localVersionList = launcher.getVersionManager().getLocalVersionList();
                    if ((localVersionList instanceof LocalVersionList)) {
                        ((LocalVersionList)localVersionList).saveVersion(version);
                        Launcher.getInstance().println("Installed " + syncInfo.getLatestVersion());
                  }
                } catch (IOException e) {
                    Launcher.getInstance().println("Couldn't save version info to install " + syncInfo.getLatestVersion(), e);
                    setWorking(false);
                    return;
                }
            }
            try {
                DownloadJob job = new DownloadJob("Version & Libraries", false, this);
                addJob(job);
                launcher.getVersionManager().downloadVersion(syncInfo, job);
                job.startDownloading(launcher.getVersionManager().getExecutorService());
            } catch (IOException e) {
                Launcher.getInstance().println("Couldn't get version info for " + syncInfo.getLatestVersion(), e);
                setWorking(false);
            }
        }
    }
    
    public void addJob(DownloadJob job) {
        synchronized (lock) {
            jobs.add(job);
        }
    }

    @Override
    public void onJavaProcessEnded(JavaProcess process) {
        int exitCode = process.getExitCode();

        if (exitCode == 0) {
            Launcher.getInstance().println("Game ended with no troubles detected (exit code " + exitCode + ")");

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    launcher.println("Exiting launcher as the game has ended");
                    launcher.closeLauncher();
                } } );
        } else {
            Launcher.getInstance().println("Game ended with bad state (exit code " + exitCode + ")");
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run() {
                    launcher.println("Showing console due to a game crash");
                    launcher.getLauncherTabPanel().setVisible(true);
                }
            });
            String errorText = null;
            String[] sysOut = (String[])process.getSysOutLines().getItems();

            for (int i = sysOut.length - 1; i >= 0; i--) {
                String line = sysOut[i];
                String crashIdentifier = "#@!@#";
                int pos = line.lastIndexOf(crashIdentifier);

                if ((pos >= 0) && (pos < line.length() - crashIdentifier.length() - 1)) {
                    errorText = line.substring(pos + crashIdentifier.length()).trim();
                    break;
                }
            }

            if (errorText != null) {
                File file = new File(errorText);

                if (file.isFile()) {
                    Launcher.getInstance().println("Crash report detected, opening: " + errorText);
                    InputStream inputStream = null;
                    try {
                        inputStream = new FileInputStream(file);
                        StringBuilder result;
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                            result = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (result.length() > 0) result.append("\n");
                                result.append(line);
                            }
                        }

                        launcher.getLauncherTabPanel().setCrashReport(new CrashReportTab(launcher, version, file, result.toString()));
                    } catch (IOException e) {
                        Launcher.getInstance().println("Couldn't open crash report", e);
                    } finally {
                        Downloadable.closeSilently(inputStream);
                    }
                } else {
                    Launcher.getInstance().println("Crash report detected, but unknown format: " + errorText);
                }
            }
        }

        setWorking(false);
    }

    @Override
    public void onDownloadJobFinished(DownloadJob job) {
        updateProgressBar();
        synchronized (lock) {
            if (job.getFailures() > 0) {
                launcher.println("Job '" + job.getName() + "' finished with " + job.getFailures() + " failure(s)!");
                setWorking(false);
            } else {
                launcher.println("Job '" + job.getName() + "' finished successfully");

                if ((isWorking()) && (!hasRemainingJobs()))
                    try {
                        launchGame();
                    } catch (Throwable ex) {
                        Launcher.getInstance().println("Fatal error launching game. Report this to http://mojang.atlassian.net please!", ex);
                    }
            }
        }
    }
    
    public boolean hasRemainingJobs() {
        synchronized (lock) {
            for (DownloadJob job : jobs) {
                if (!job.isComplete()) return true;
            }
        }

        return false;
    }
    
    private void cleanOldNatives() {
        File root = new File(launcher.getWorkingDirectory(), "versions/");
        launcher.println("Looking for old natives to clean up...");
        IOFileFilter ageFilter = new AgeFileFilter(System.currentTimeMillis() - 3600L);

        for (File versions : root.listFiles())
            for (File folder : versions.listFiles((FilenameFilter)FileFilterUtils.and(new IOFileFilter[] { new PrefixFileFilter(versions.getName() + "-natives-"), ageFilter }))) {
                Launcher.getInstance().println("Deleting " + folder);

                FileUtils.deleteQuietly(folder);
            }
    }
    
    private void unpackNatives(CompleteVersion version, File targetDir) throws IOException {
        OperatingSystem os = OperatingSystem.getCurrentPlatform();
        Collection libraries = version.getRelevantLibraries();

        for (Object librarya : libraries) {
            Library library = (Library) librarya;
            Map nativesPerOs = library.getNatives();

            if ((nativesPerOs != null) && (nativesPerOs.get(os) != null)) {
                File file = new File(launcher.getWorkingDirectory(), "libraries/" + library.getArtifactPath((String)nativesPerOs.get(os)));
                ZipFile zip = new ZipFile(file);
                ExtractRules extractRules = library.getExtractRules();
                try {
                    Enumeration entries = zip.entries();

                    while (entries.hasMoreElements()) {
                        ZipEntry entry = (ZipEntry)entries.nextElement();

                        if ((extractRules != null) && (!extractRules.shouldExtract(entry.getName())))
                        {
                            continue;
                        }
                        File targetFile = new File(targetDir, entry.getName());
                        if (targetFile.getParentFile() != null) targetFile.getParentFile().mkdirs();

                        if (!entry.isDirectory()) {
                            BufferedInputStream inputStream = new BufferedInputStream(zip.getInputStream(entry));

                            byte[] buffer = new byte[2048];
                            FileOutputStream outputStream = new FileOutputStream(targetFile);
                            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
                            try {
                                int length;
                                while ((length = inputStream.read(buffer, 0, buffer.length)) != -1)
                                    bufferedOutputStream.write(buffer, 0, length);
                            } finally {
                                Downloadable.closeSilently(bufferedOutputStream);
                                Downloadable.closeSilently(outputStream);
                                Downloadable.closeSilently(inputStream);
                            }
                        }
                    }
                } finally {
                    zip.close();
                }
            }
        }
    }
    
    private String constructClassPath(CompleteVersion version) {
        StringBuilder result = new StringBuilder();
        Collection classPath = version.getClassPath(OperatingSystem.getCurrentPlatform(), launcher.getWorkingDirectory());
        String separator = System.getProperty("path.separator");

        for (Object filea : classPath) {
            File file = (File) filea;
          if (!file.isFile()) throw new RuntimeException("Classpath file not found: " + file);
          if (result.length() > 0) result.append(separator);
            result.append(file.getAbsolutePath());
        }

        return result.toString();
    }
    
    protected void launchGame() {
        launcher.println("Launching game");

        if (version == null) {
            Launcher.getInstance().println("Aborting launch; version is null?");
            return;
        }

        cleanOldNatives();

        nativeDir = new File(launcher.getWorkingDirectory(), "versions/" + version.getId() + "/" + version.getId() + "-natives-" + System.nanoTime());
        if (!nativeDir.isDirectory()) nativeDir.mkdirs();
        launcher.println("Unpacking natives to " + nativeDir);
        try {
            unpackNatives(version, nativeDir);
        } catch (IOException e) {
            Launcher.getInstance().println("Couldn't unpack natives!", e);
            return;
        }

        File gameDirectory = launcher.getWorkingDirectory();
        Launcher.getInstance().println("Launching in " + gameDirectory);

        if (!gameDirectory.exists()) {
            if (!gameDirectory.mkdirs()) {
                Launcher.getInstance().println("Aborting launch; couldn't create game directory");
                return;
            }
        } else if (!gameDirectory.isDirectory()) {
            Launcher.getInstance().println("Aborting launch; game directory is not actually a directory");
            return;
        }

        JavaProcessLauncher processLauncher = new JavaProcessLauncher(OperatingSystem.getCurrentPlatform().getJavaDir(), new String[0]);
        launcher.println("Warning: currently using " + OperatingSystem.getCurrentPlatform().getJavaDir() + " have to make a option for it.");
        processLauncher.directory(gameDirectory);

        File assetsDirectory = new File(launcher.getWorkingDirectory(), "assets");

        OperatingSystem os = OperatingSystem.getCurrentPlatform();
        if (os.equals(OperatingSystem.OSX))
            processLauncher.addCommands(new String[] { "-Xdock:icon=" + new File(assetsDirectory, "icons/minecraft.icns").getAbsolutePath(), "-Xdock:name=Minecraft" });
        else if (os.equals(OperatingSystem.WINDOWS)) {
            processLauncher.addCommands(new String[] { "-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump" });
        }

        String profileArgs = "-Dfml.ignoreInvalidMinecraftCertificates=true -Dfml.ignorePatchDiscrepancies=true -Xmx" + launcher.getOptionPanel().GetRAMValue() + "m";
        launcher.println("Currently using standard arguments, have to make a option.");

        processLauncher.addSplitCommands(profileArgs);

        processLauncher.addCommands(new String[] { "-Djava.library.path=" + nativeDir.getAbsolutePath() });
        processLauncher.addCommands(new String[] { "-cp", constructClassPath(version) });
        processLauncher.addCommands(new String[] { version.getMainClass() });

    /*AuthenticationService auth = launcher.getProfileManager().getAuthDatabase().getByUUID(selectedProfile.getPlayerUUID());
     * */
   /* String[] args = getMinecraftArguments(version, selectedProfile, gameDirectory, assetsDirectory, auth);
    * */
        String[] args = getMinecraftArguments(version, gameDirectory, assetsDirectory);
        if (args == null) return;
        processLauncher.addCommands(args);
        
        Proxy proxy = launcher.getProxy();
        PasswordAuthentication proxyAuth = launcher.getProxyAuth();
            if (!proxy.equals(Proxy.NO_PROXY)) {
                InetSocketAddress address = (InetSocketAddress)proxy.address();
                processLauncher.addCommands(new String[] { "--proxyHost", address.getHostName() });
                processLauncher.addCommands(new String[] { "--proxyPort", Integer.toString(address.getPort()) });
            if (proxyAuth != null) {
                processLauncher.addCommands(new String[] { "--proxyUser", proxyAuth.getUserName() });
                processLauncher.addCommands(new String[] { "--proxyPass", new String(proxyAuth.getPassword()) });
            }
        }
        /*
    if ((auth == null) || (auth.getSelectedProfile() == null)) {
      processLauncher.addCommands(new String[] { "--demo" });
    }

    if (selectedProfile.getResolution() != null) {
      processLauncher.addCommands(new String[] { "--width", String.valueOf(selectedProfile.getResolution().getWidth()) });
      processLauncher.addCommands(new String[] { "--height", String.valueOf(selectedProfile.getResolution().getHeight()) });
    }*/
        try {
            List parts = processLauncher.getFullCommands();
            StringBuilder full = new StringBuilder();
            boolean first = true;

            for (Object parta : parts) {
                String part = (String) parta;
              if (!first) full.append(" ");
              full.append(part);
              first = false;
            }

            Launcher.getInstance().println("Running " + full.toString());
            JavaProcess process = processLauncher.start();
            process.safeSetExitRunnable(this);

            if (launcher.getOptionPanel().GetShowConsoleState() == false)
                launcher.getLauncherTabPanel().setVisible(false);
            launcher.getFrame().setVisible(false);
        } catch (IOException e) {
            Launcher.getInstance().println("Couldn't launch game", e);
            setWorking(false);
        }
    }

    private String[] getMinecraftArguments(CompleteVersion version, File gameDirectory, File assetsDirectory) { //, AuthenticationService authentication) {
        if (version.getMinecraftArguments() == null) {
            Launcher.getInstance().println("Can't run version, missing minecraftArguments");
            setWorking(false);
            return null;
        }

        Map map = new HashMap();
        StrSubstitutor substitutor = new StrSubstitutor(map);
        String[] split = version.getMinecraftArguments().split(" ");

        map.put("auth_username", launcher.getRightBarPanel().getLogInPanel().getUsername()); //authentication.getUsername());
        map.put("auth_session", String.format("token:%s:%s", new Object[] { UUID.randomUUID(), UUID.randomUUID() }));//(authentication.getSessionToken() == null) && (authentication.canPlayOnline()) ? "-" : authentication.getSessionToken());

        //if (authentication.getSelectedProfile() != null) {
            map.put("auth_player_name", launcher.getRightBarPanel().getLogInPanel().getUsername());//, authentication.getSelectedProfile().getName());
            map.put("auth_uuid", "uuid");//authentication.getSelectedProfile().getId());
        /*} else {
            map.put("auth_player_name", "Player");
            map.put("auth_uuid", new UUID(0L, 0L).toString());
        }*/

        map.put("profile_name", "none");//selectedProfile.getName());
        map.put("version_name", version.getId());

        map.put("game_directory", gameDirectory.getAbsolutePath());
        map.put("game_assets", assetsDirectory.getAbsolutePath());

        for (int i = 0; i < split.length; i++) {
          split[i] = substitutor.replace(split[i]);
        }

        return split;
    }
    
    @Override
    public void onDownloadJobProgressChanged(DownloadJob job) {
      updateProgressBar();
    }

    protected void updateProgressBar() {
      final float progress = getProgress();
      final boolean hasTasks = hasRemainingJobs();

      SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run() {
            launcher.getRightBarPanel().getPlayButtonPanel().getProgressBar().setVisible(hasTasks);
            launcher.getRightBarPanel().getPlayButtonPanel().getProgressBar().setValue((int)(progress * 100.0F));
        } } );
    }
    
    protected float getProgress() {
        synchronized (lock) {
            float max = 0.0F;
            float result = 0.0F;

            for (DownloadJob job : jobs) {
                float progress = job.getProgress();

                if (progress >= 0.0F) {
                    result += progress;
                    max += 1.0F;
                }
            }

            return result / max;
        }
    }
}
