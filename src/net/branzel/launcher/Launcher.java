package net.branzel.launcher;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import net.branzel.launcher.ui.OptionPanel;
import net.branzel.launcher.ui.PlayButtonPanel;
import net.branzel.launcher.ui.RightBarPanel;
import net.branzel.launcher.ui.VersionPanel;
import net.branzel.launcher.ui.console.ConsoleFrame;
import net.branzel.launcher.updater.LocalVersionList;
import net.branzel.launcher.updater.RemoteVersionList;
import net.branzel.launcher.updater.VersionManager;
import net.minecraft.launcher.OperatingSystem;
import net.branzel.launcher.updater.download.DownloadJob;

/**
 *
 * @author Branzel
 */
public final class Launcher {
    private static Launcher instance;
    private final JFrame frame;
    private final VersionPanel versionpanel;
    private final GameLauncher gameLauncher;
    private final PlayButtonPanel playbuttonpanel;
    private final RightBarPanel rightbarpanel;
    private final ConsoleFrame consoleframe;
    private final OptionPanel optionpanel;
    private static final List<String> delayedSysout = new ArrayList();
    private final File workingDirectory;
    private final Proxy proxy;
    private final PasswordAuthentication proxyAuth;
    private final Integer bootstrapVersion;
    private final VersionManager versionManager;
    public String versionId = null;
    
    public Launcher(JFrame frame, File workingDirectory, Proxy proxy, PasswordAuthentication proxyAuth, Integer bootstrapVersion) {
        this.bootstrapVersion = bootstrapVersion;
        instance = this;
        setLookAndFeel();

        this.proxy = proxy;
        this.proxyAuth = proxyAuth;
        this.workingDirectory = workingDirectory;
        this.frame = frame;
        gameLauncher = new GameLauncher(this);
        versionManager = new VersionManager(new LocalVersionList(workingDirectory), new RemoteVersionList(proxy));
        rightbarpanel = new RightBarPanel(this);
        versionpanel = new VersionPanel(this);
        playbuttonpanel = new PlayButtonPanel(this);
        consoleframe = new ConsoleFrame(this);
        optionpanel = new OptionPanel(this);

        initializeFrame();

        for (String line : delayedSysout) {
            consoleframe.getConsole().print(line + "\n");
        }

        downloadResources();
        refreshVersions();

        println("Launcher 1.0.0 (through bootstrap " + bootstrapVersion + ") started on " + OperatingSystem.getCurrentPlatform().getName() + "...");
        println("Current time is " + DateFormat.getDateTimeInstance(2, 2, Locale.US).format(new Date()));

        if (!OperatingSystem.getCurrentPlatform().isSupported()) {
            println("This operating system is unknown or unsupported, we cannot guarantee that the game will launch.");
        }
        println("System.getProperty('os.name') == '" + System.getProperty("os.name") + "'");
        println("System.getProperty('os.version') == '" + System.getProperty("os.version") + "'");
        println("System.getProperty('os.arch') == '" + System.getProperty("os.arch") + "'");
        println("System.getProperty('java.version') == '" + System.getProperty("java.version") + "'");
        println("System.getProperty('java.vendor') == '" + System.getProperty("java.vendor") + "'");
        println("System.getProperty('sun.arch.data.model') == '" + System.getProperty("sun.arch.data.model") + "'");
    }
    
    public static Launcher getInstance() {
        return instance;
    }
    
    public void setLastVersionId(String id) {
        versionId = id;
    }
    
    public VersionManager getVersionManager() {
        return versionManager;
    }
    
    public Proxy getProxy() {
        return proxy;
    }
    
    public PasswordAuthentication getProxyAuth() {
        return proxyAuth;
    }
    
    public File getWorkingDirectory() {
        return workingDirectory;
    }
    
    public ConsoleFrame getLauncherTabPanel() {
        return consoleframe;
    }
    
    public String getLastVersionId() {
        return versionId;
    }
    
    private static void setLookAndFeel() {
        JFrame frame = new JFrame();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ignored) {
            try {
                getInstance().println("Your java failed to provide normal look and feel, trying the old fallback now");
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException t) {
                getInstance().println("Unexpected exception setting look and feel");
                getInstance().println(t.toString());
            }
        }
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("test"));
        frame.add(panel);
        try {
            frame.pack();
        } catch (Throwable t) {
            getInstance().println("Custom (broken) theme detected, falling back onto x-platform theme");
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                getInstance().println("Unexpected exception setting look and feel", ex);
            }
        }

        frame.dispose();
    }
    
    protected void initializeFrame() {
        frame.getContentPane().removeAll();
        frame.setTitle("Branzel's Minecraft Launcher 1.0.0");
        frame.setPreferredSize(new Dimension(900, 580));
        frame.setDefaultCloseOperation(2);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                frame.setVisible(false);
                frame.dispose();
                versionManager.getExecutorService().shutdown();
            }
        });
        try {
            InputStream in = net.minecraft.launcher.Launcher.class.getResourceAsStream("/favicon.png");
            if (in != null)
                frame.setIconImage(ImageIO.read(in));
        } catch (IOException localIOException) { }
        
        frame.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.anchor = 17;

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = 3;
        constraints.ipady  = 40;
        constraints.gridy = 0;
        
        constraints.gridwidth = 1;
        constraints.ipady  = 0;
        constraints.fill = 2;
        constraints.weightx = 1.0D;
        
        frame.add(optionpanel, constraints);
        frame.add(rightbarpanel, constraints);

        frame.pack();
        frame.setVisible(true);
    }

    public void println(String line) {
        System.out.println(line);

        if (consoleframe == null)
            delayedSysout.add(line);
        else
            consoleframe.getConsole().print(line + "\n");
    }

    public void println(String line, Throwable throwable) {
        println(line);
        println(throwable);
    }
    
    public void closeLauncher() {
        frame.dispatchEvent(new WindowEvent(frame, 201));
        consoleframe.dispatchEvent(new WindowEvent(frame, 201));
    }

    public void println(Throwable throwable) {
        StringWriter writer = null;
        PrintWriter printWriter = null;
        String result = throwable.toString();
        try {
            writer = new StringWriter();
            printWriter = new PrintWriter(writer);
            throwable.printStackTrace(printWriter);
            result = writer.toString();
        } finally {
            try {
                if (writer != null) writer.close();
                if (printWriter != null) printWriter.close(); 
            }
            catch (IOException localIOException1) { }
        }
        println(result);
    }
    
    private void downloadResources() {
        final DownloadJob job = new DownloadJob("Resources", true, gameLauncher);
        gameLauncher.addJob(job);
        versionManager.getExecutorService().submit(new Runnable()
        {
            @Override
            public void run() {
                  try {
                      versionManager.downloadResources(job);
                      job.startDownloading(versionManager.getExecutorService());
                  } catch (IOException e) {
                      Launcher.getInstance().println("Unexpected exception queueing resource downloads", e);
                  }
            }
        } );
    }
    
    public void refreshVersions() {
        versionManager.getExecutorService().submit(new Runnable()
        {
            @Override
            public void run() {
                try {
                    versionManager.refreshVersions();
                } catch (Throwable e) {
                    Launcher.getInstance().println("Unexpected exception refreshing version list", e);
                }

              /// TO DO ensureLoggedIn();
            } 
        } );
    }
    
    public JFrame getFrame() {
        return frame;
    }
    
    public int getBootstrapVersion() {
        return bootstrapVersion.intValue();
    }
    
    public GameLauncher getGameLauncher() {
        return gameLauncher;
    }
    
    public OptionPanel getOptionPanel() {
        return optionpanel;
    }
    
    public RightBarPanel getRightBarPanel() {
        return rightbarpanel;
    }
}
