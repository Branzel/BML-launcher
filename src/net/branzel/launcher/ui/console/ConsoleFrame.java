package net.branzel.launcher.ui.console;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import net.branzel.launcher.Launcher;

/**
 *
 * @author Branzel
 */
public class ConsoleFrame extends JFrame {
    private final JTabbedPane tabpane;
    private final Launcher launcher;
    private final ConsoleTab console;
    private CrashReportTab crashReportTab;
    
    public ConsoleFrame(Launcher launcher) {
        tabpane = new JTabbedPane(1);

        this.launcher = launcher;
        console = new ConsoleTab(launcher);

        createInterface();
    }
    
    protected void createInterface() {
        this.getContentPane().removeAll();
        this.setTitle("Console");
        this.setPreferredSize(new Dimension(700, 350));
        this.setDefaultCloseOperation(2);
        
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });
        
        tabpane.addTab("Development Console", console);
        this.add(tabpane);
        
        this.pack();
        this.setVisible(true);
    }
    
    public Launcher getLauncher() {
        return launcher;
    }

    public ConsoleTab getConsole() {
        return console;
    }

    public void showConsole() {
        tabpane.setSelectedComponent(console);
    }
    
    public void setCrashReport(CrashReportTab newTab) {
        if (crashReportTab != null) removeTab(crashReportTab);
        crashReportTab = newTab;
        tabpane.addTab("Crash Report", crashReportTab);
        tabpane.setSelectedComponent(newTab);
    }

    protected void removeTab(Component tab) {
        for (int i = 0; i < tabpane.getTabCount(); i++)
            if (tabpane.getTabComponentAt(i) == tab) {
                  tabpane.removeTabAt(i);
                  break;
            }
    }
}
