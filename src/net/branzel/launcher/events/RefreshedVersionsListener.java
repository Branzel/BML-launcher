package net.branzel.launcher.events;

import net.branzel.launcher.updater.VersionManager;

public abstract interface RefreshedVersionsListener {
    public abstract void onVersionsRefreshed(VersionManager paramVersionManager);

    public abstract boolean shouldReceiveEventsInUIThread();
}
