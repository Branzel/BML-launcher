package net.branzel.launcher.ui;

import java.awt.GridLayout;
import javax.swing.JPanel;
import net.branzel.launcher.Launcher;

/**
 *
 * @author Branzel
 */
public final class RightBarPanel extends JPanel {
    private final Launcher launcher;
    private final VersionPanel versionpanel;
    private final PlayButtonPanel playbuttonpanel;
    private final LogInPanel logInPanel;
    
    public RightBarPanel(Launcher launcher) {
        this.launcher = launcher;

        versionpanel = new VersionPanel(launcher);
        playbuttonpanel = new PlayButtonPanel(launcher);
        logInPanel = new LogInPanel(launcher);
        
        createInterface();
    }
    
    protected void createInterface() {
        setLayout(new GridLayout(2,1));

        add(logInPanel);
        add(versionpanel);
        add(playbuttonpanel);
    }
    
    public Launcher getLauncher() {
        return launcher;
    }

    public PlayButtonPanel getPlayButtonPanel() {
        return playbuttonpanel;
    }
    
    public LogInPanel getLogInPanel() {
        return logInPanel;
    }
}
