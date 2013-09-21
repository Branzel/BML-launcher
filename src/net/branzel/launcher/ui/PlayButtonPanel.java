/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.branzel.launcher.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import net.branzel.launcher.Launcher;
import net.branzel.launcher.updater.VersionManager;

/**
 *
 * @author Branzel
 */
public class PlayButtonPanel extends JPanel {
    private final Launcher launcher;
    private final JProgressBar progressBar;
    private final JButton playButton = new JButton("Play");
    
    public PlayButtonPanel(Launcher launcher) {
        this.launcher = launcher;
        progressBar = new JProgressBar();

        //launcher.getProfileManager().addRefreshedProfilesListener(this);
        checkState();
        createInterface();

        playButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e) {
                getLauncher().getVersionManager().getExecutorService().submit(new Runnable()
                {
                    @Override
                    public void run() {
                      getLauncher().getGameLauncher().playGame();
                    } } );
              } } );
    }
    
    protected void createInterface() {
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = 1;
        constraints.weightx = 1.0D;
        constraints.weighty = 1.0D;

        constraints.gridy = 0;
        constraints.gridx = 0;
        add(progressBar, constraints);
        constraints.gridy = 1;
        add(playButton, constraints);

        playButton.setFont(playButton.getFont().deriveFont(1, playButton.getFont().getSize() + 2));
    }
    
    public void checkState() {
    /*Profile profile = launcher.getProfileManager().getProfiles().isEmpty() ? null : launcher.getProfileManager().getSelectedProfile();
    AuthenticationService auth = profile == null ? null : launcher.getProfileManager().getAuthDatabase().getByUUID(profile.getPlayerUUID());

    if ((auth == null) || (!auth.isLoggedIn()) || (launcher.getVersionManager().getVersions(profile.getVersionFilter()).isEmpty())) {
      playButton.setEnabled(false);
      playButton.setText("Play");
    } else if (auth.getSelectedProfile() == null) {
      playButton.setEnabled(true);
      playButton.setText("Play Demo");
    } else if (auth.canPlayOnline()) {
      playButton.setEnabled(true);
      playButton.setText("Play");
    } else {
      playButton.setEnabled(true);
      playButton.setText("Play Offline");
    }*/
      playButton.setEnabled(true);
      playButton.setText("Play");

    if (launcher.getGameLauncher().isWorking())
      playButton.setEnabled(false);
  }
    public void onVersionsRefreshed(VersionManager manager) {
        checkState();
    }
    
    public Launcher getLauncher() {
        return launcher;
    }
    
    public JButton getPlayButton() {
        return playButton;
    }
    
    public JProgressBar getProgressBar() {
        return progressBar;
    }
}
