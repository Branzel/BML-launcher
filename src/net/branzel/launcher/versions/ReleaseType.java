package net.branzel.launcher.versions;

import java.util.HashMap;
import java.util.Map;

public enum ReleaseType {
    SNAPSHOT("snapshot", "Enable experimental development versions (\"snapshots\")"), 
    RELEASE("release", null);

    private static final String POPUP_DEV_VERSIONS = "Are you sure you want to enable development builds?\nThey are not guaranteed to be stable and may corrupt your world.\nYou are advised to run this in a separate directory or run regular backups.";
    private static final Map<String, ReleaseType> lookup;
    private final String name;
    private final String description;

    private ReleaseType(String name, String description)
    {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPopupWarning() {
        if (description == null) return null;
        if (this == SNAPSHOT) return POPUP_DEV_VERSIONS;
        return null;
    }

    public static ReleaseType getByName(String name) {
        return (ReleaseType)lookup.get(name);
    }

    static {
        lookup = new HashMap();

        for (ReleaseType type : values())
            lookup.put(type.getName(), type);
    }
}
