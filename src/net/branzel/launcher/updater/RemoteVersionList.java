package net.branzel.launcher.updater;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import net.branzel.launcher.versions.CompleteVersion;
import net.minecraft.launcher.Http;
import net.minecraft.launcher.OperatingSystem;

public class RemoteVersionList extends VersionList
{
    private final Proxy proxy;

    public RemoteVersionList(Proxy proxy)
    {
        this.proxy = proxy;
    }

    @Override
    public boolean hasAllFiles(CompleteVersion version, OperatingSystem os)
    {
        return true;
    }

    @Override
    protected String getContent(String path) throws IOException
    {
        return Http.performGet(new URL("https://dl.dropboxusercontent.com/u/69130671/Minecraft/BML/Minecraft.Download/" + path), proxy);
    }

    public Proxy getProxy() {
        return proxy;
    }
}