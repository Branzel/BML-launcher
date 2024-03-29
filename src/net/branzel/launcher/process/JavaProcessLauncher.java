package net.branzel.launcher.process;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.launcher.OperatingSystem;

public final class JavaProcessLauncher
{
    private final String jvmPath;
    private final List<String> commands;
    private File directory;

    public JavaProcessLauncher(String jvmPath, String[] commands) {
        if (jvmPath == null) jvmPath = OperatingSystem.getCurrentPlatform().getJavaDir();
        this.jvmPath = jvmPath;
        this.commands = new ArrayList(commands.length);
        addCommands(commands);
    }

    public JavaProcess start() throws IOException {
        List full = getFullCommands();
        return new JavaProcess(full, new ProcessBuilder(full).directory(directory).redirectErrorStream(true).start());
    }

    public List<String> getFullCommands() {
        List result = new ArrayList(commands);
        result.add(0, getJavaPath());
        return result;
    }

    public List<String> getCommands() {
        return commands;
    }

    public void addCommands(String[] commands) {
        this.commands.addAll(Arrays.asList(commands));
    }

    public void addSplitCommands(String commands) {
        addCommands(commands.split(" "));
    }

    public JavaProcessLauncher directory(File directory) {
        this.directory = directory;

        return this;
    }

    public File getDirectory() {
        return directory;
    }

    protected String getJavaPath() {
        return jvmPath;
    }

    @Override
    public String toString() {
        return "JavaProcessLauncher[commands=" + commands + ", java=" + jvmPath + "]";
    }
}
