package net.branzel.launcher.updater;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.branzel.launcher.Launcher;
import net.branzel.launcher.events.RefreshedVersionsListener;
import net.branzel.launcher.versions.CompleteVersion;
import net.branzel.launcher.versions.ReleaseType;
import net.branzel.launcher.versions.Version;
import net.minecraft.launcher.OperatingSystem;
import net.branzel.launcher.updater.download.DownloadJob;
import net.branzel.launcher.updater.download.Downloadable;
import net.minecraft.launcher.updater.VersionFilter;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class VersionManager {
    private final VersionList localVersionList;
    private final VersionList remoteVersionList;
    private final ThreadPoolExecutor executorService = new ExceptionalThreadPoolExecutor(8);
    private final List<RefreshedVersionsListener> refreshedVersionsListeners = Collections.synchronizedList(new ArrayList());
    private final Object refreshLock = new Object();
    private boolean isRefreshing;
    
    public VersionManager(VersionList localVersionList, VersionList remoteVersionList) {
        this.localVersionList = localVersionList;
        this.remoteVersionList = remoteVersionList;
    }
    
    public void refreshVersions() throws IOException {
        synchronized (refreshLock) {
            isRefreshing = true;
        }
        try
        {
            Launcher.getInstance().println("Refreshing local version list...");
            localVersionList.refreshVersions();
            Launcher.getInstance().println("Refreshing remote version list...");
            remoteVersionList.refreshVersions();
        } catch (IOException ex) {
            synchronized (refreshLock) {
                isRefreshing = false;
            }
            throw ex;
        }

        Launcher.getInstance().println("Refresh complete.");

        synchronized (refreshLock) {
            isRefreshing = false;
        }

        final List<RefreshedVersionsListener> listeners = new ArrayList(refreshedVersionsListeners);
        for (Iterator iterator = listeners.iterator(); iterator.hasNext(); ) {
            RefreshedVersionsListener listener = (RefreshedVersionsListener)iterator.next();

            if (!listener.shouldReceiveEventsInUIThread()) {
                listener.onVersionsRefreshed(this);
                iterator.remove();
            }
        }

        if (!listeners.isEmpty())
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (RefreshedVersionsListener listener : listeners)
                        listener.onVersionsRefreshed(VersionManager.this);
                }
            });
    }

    public List<VersionSyncInfo> getVersions()
    {
        return getVersions(null);
    }

    public List<VersionSyncInfo> getVersions(VersionFilter filter) {
        synchronized (refreshLock) {
            if (isRefreshing) return new ArrayList();
        }

        List result = new ArrayList();
        Object lookup = new HashMap();
        Map counts = new EnumMap(ReleaseType.class);

        for (ReleaseType type : ReleaseType.values()) {
            counts.put(type, Integer.valueOf(0));
        }

        for (Version version : localVersionList.getVersions()) {
            if ((version.getType() == null) || (version.getUpdatedTime() == null) || (
                (filter != null) && ((!filter.getTypes().contains(version.getType())) || (((Integer)counts.get(version.getType())).intValue() >= filter.getMaxCount()))))
                continue;
            VersionSyncInfo syncInfo = getVersionSyncInfo(version, remoteVersionList.getVersion(version.getId()));
            ((Map)lookup).put(version.getId(), syncInfo);
            result.add(syncInfo);
        }

        for (Version version : remoteVersionList.getVersions()) {
            if ((version.getType() == null) || (version.getUpdatedTime() == null) || (((Map)lookup).containsKey(version.getId())) || ((filter != null) && ((!filter.getTypes().contains(version.getType())) || (((Integer)counts.get(version.getType())).intValue() >= filter.getMaxCount()))))
                continue;
            VersionSyncInfo syncInfo = getVersionSyncInfo(localVersionList.getVersion(version.getId()), version);
            ((Map)lookup).put(version.getId(), syncInfo);
            result.add(syncInfo);

            if (filter != null) counts.put(version.getType(), Integer.valueOf(((Integer)counts.get(version.getType())).intValue() + 1));
        }

        if (result.isEmpty()) {
            for (Version version : localVersionList.getVersions()) {
                if ((version.getType() != null) && (version.getUpdatedTime() != null)) {
                    VersionSyncInfo syncInfo = getVersionSyncInfo(version, remoteVersionList.getVersion(version.getId()));
                    ((Map)lookup).put(version.getId(), syncInfo);
                    result.add(syncInfo);
                }
            }
        }

        Collections.sort(result, new Comparator() {
            @Override
            public int compare(Object a, Object b) {
                Version aVer = ((VersionSyncInfo)a).getLatestVersion();
                Version bVer = ((VersionSyncInfo)b).getLatestVersion();

                if ((aVer.getReleaseTime() != null) && (bVer.getReleaseTime() != null)) {
                      return bVer.getReleaseTime().compareTo(aVer.getReleaseTime());
                }
                return bVer.getUpdatedTime().compareTo(aVer.getUpdatedTime());
            }
        });
        return (List<VersionSyncInfo>)result;
    }

    public VersionSyncInfo getVersionSyncInfo(Version version) {
        return getVersionSyncInfo(version.getId());
    }

    public VersionSyncInfo getVersionSyncInfo(String name) {
        return getVersionSyncInfo(localVersionList.getVersion(name), remoteVersionList.getVersion(name));
    }

    public VersionSyncInfo getVersionSyncInfo(Version localVersion, Version remoteVersion) {
        boolean installed = localVersion != null;
        boolean upToDate = installed;

        if ((installed) && (remoteVersion != null)) {
            if (localVersion != null)
                upToDate = !remoteVersion.getUpdatedTime().after(localVersion.getUpdatedTime());
        }
        if ((localVersion instanceof CompleteVersion)) {
            upToDate &= localVersionList.hasAllFiles((CompleteVersion)localVersion, OperatingSystem.getCurrentPlatform());
        }

        return new VersionSyncInfo(localVersion, remoteVersion, installed, upToDate);
    }

    public List<VersionSyncInfo> getInstalledVersions() {
        List result = new ArrayList();

        for (Version version : localVersionList.getVersions()) {
            if ((version.getType() == null) || (version.getUpdatedTime() == null))
                continue;
            VersionSyncInfo syncInfo = getVersionSyncInfo(version, remoteVersionList.getVersion(version.getId()));
            result.add(syncInfo);
        }

        return result;
    }

    public VersionList getRemoteVersionList() {
        return remoteVersionList;
    }

    public VersionList getLocalVersionList() {
        return localVersionList;
    }

    public CompleteVersion getLatestCompleteVersion(VersionSyncInfo syncInfo) throws IOException {
        if (syncInfo.getLatestSource() == VersionSyncInfo.VersionSource.REMOTE) {
            CompleteVersion result = null;
            IOException exception = null;
            try
            {
                result = remoteVersionList.getCompleteVersion(syncInfo.getLatestVersion());
            } catch (IOException e) {
                exception = e;
                try {
                    result = localVersionList.getCompleteVersion(syncInfo.getLatestVersion());
                } catch (IOException localIOException1) { }
            }
            if (result != null) {
                return result;
            }
            throw exception;
        }

        return localVersionList.getCompleteVersion(syncInfo.getLatestVersion());
    }

    public DownloadJob downloadVersion(VersionSyncInfo syncInfo, DownloadJob job) throws IOException
    {
        if (!(localVersionList instanceof LocalVersionList)) throw new IllegalArgumentException("Cannot download if local repo isn't a LocalVersionList");
        if (!(remoteVersionList instanceof RemoteVersionList)) throw new IllegalArgumentException("Cannot download if local repo isn't a RemoteVersionList");
        CompleteVersion version = getLatestCompleteVersion(syncInfo);
        File baseDirectory = ((LocalVersionList)localVersionList).getBaseDirectory();
        Proxy proxy = ((RemoteVersionList)remoteVersionList).getProxy();

        job.addDownloadables(version.getRequiredDownloadables(OperatingSystem.getCurrentPlatform(), proxy, baseDirectory, false));

        String jarFile = "versions/" + version.getId() + "/" + version.getId() + ".jar";
        job.addDownloadables(new Downloadable[] { new Downloadable(proxy, new URL("https://dl.dropboxusercontent.com/u/69130671/Minecraft/BML/Minecraft.Download/" + jarFile), new File(baseDirectory, jarFile), false) });

        return job;
    }

    public DownloadJob downloadResources(DownloadJob job) throws IOException {
        File baseDirectory = ((LocalVersionList)localVersionList).getBaseDirectory();

        job.addDownloadables(getResourceFiles(((RemoteVersionList)remoteVersionList).getProxy(), baseDirectory));

        return job;
    }

    private Set<Downloadable> getResourceFiles(Proxy proxy, File baseDirectory) {
        Set result = new HashSet();
        try
        {
            URL resourceUrl = new URL("https://s3.amazonaws.com/Minecraft.Resources/");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(resourceUrl.openConnection(proxy).getInputStream());
            NodeList nodeLst = doc.getElementsByTagName("Contents");

            long start = System.nanoTime();
            for (int i = 0; i < nodeLst.getLength(); i++) {
                Node node = nodeLst.item(i);

                if (node.getNodeType() == 1) {
                    Element element = (Element)node;
                    String key = element.getElementsByTagName("Key").item(0).getChildNodes().item(0).getNodeValue();
                    String etag = element.getElementsByTagName("ETag") != null ? element.getElementsByTagName("ETag").item(0).getChildNodes().item(0).getNodeValue() : "-";
                    long size = Long.parseLong(element.getElementsByTagName("Size").item(0).getChildNodes().item(0).getNodeValue());

                    if (size > 0L) {
                        File file = new File(baseDirectory, "assets/" + key);
                        if (etag.length() > 1) {
                          etag = Downloadable.getEtag(etag);
                          if ((file.isFile()) && (file.length() == size)) {
                            String localMd5 = Downloadable.getMD5(file);
                            if (localMd5.equals(etag)) continue;
                          }
                        }
                        Downloadable downloadable = new Downloadable(proxy, new URL("https://s3.amazonaws.com/Minecraft.Resources/" + key), file, false);
                        downloadable.setExpectedSize(size);
                        result.add(downloadable);
                    }
                }
            }
            long end = System.nanoTime();
            long delta = end - start;
            Launcher.getInstance().println("Delta time to compare resources: " + delta / 1000000L + " ms ");
        } catch (ParserConfigurationException | IOException | SAXException | DOMException | NumberFormatException ex) {
            Launcher.getInstance().println("Couldn't download resources", ex);
        }

        return result;
    }

    public ThreadPoolExecutor getExecutorService() {
        return executorService;
    }

    public void addRefreshedVersionsListener(RefreshedVersionsListener listener) {
        refreshedVersionsListeners.add(listener);
    }

    public void removeRefreshedVersionsListener(RefreshedVersionsListener listener) {
        refreshedVersionsListeners.remove(listener);
    }
}
