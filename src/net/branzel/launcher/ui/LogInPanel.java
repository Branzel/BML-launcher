package net.branzel.launcher.ui;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import net.branzel.launcher.Launcher;
import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.OperatingSystem;
import net.minecraft.launcher.authentication.AuthenticationDatabase;
import net.minecraft.launcher.authentication.AuthenticationService;
import net.minecraft.launcher.authentication.yggdrasil.YggdrasilAuthenticationService;

/**
 *
 * @author Branzel
 */
public class LogInPanel extends JPanel implements ActionListener {
    private final Launcher launcher;
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final AuthenticationService authentication = new YggdrasilAuthenticationService();
    
    public LogInPanel(Launcher launcher) {
        this.launcher = launcher;

        usernameField.addActionListener(this);
        passwordField.addActionListener(this);

        createInterface();
    }
    
    protected void createInterface() {
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = 2;
        constraints.gridx = 0;
        constraints.gridy = -1;
        constraints.weightx = 1.0D;

        add(Box.createGlue());

        JLabel usernameLabel = new JLabel("Email Address or Username:");
        Font labelFont = usernameLabel.getFont().deriveFont(1);
        Font smalltextFont = usernameLabel.getFont().deriveFont(labelFont.getSize() - 2.0F);

        usernameLabel.setFont(labelFont);
        add(usernameLabel, constraints);
        add(usernameField, constraints);

        JLabel forgotUsernameLabel = new JLabel("(Which do I use?)");
        forgotUsernameLabel.setFont(smalltextFont);
        forgotUsernameLabel.setHorizontalAlignment(4);
        forgotUsernameLabel.addMouseListener(new MouseAdapter()
        {
          public void mouseClicked(MouseEvent e) {
            OperatingSystem.openLink(LauncherConstants.URL_FORGOT_USERNAME);
          }
        });
        add(forgotUsernameLabel, constraints);

        add(Box.createVerticalStrut(10), constraints);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(labelFont);
        add(passwordLabel, constraints);
        add(passwordField, constraints);

        JLabel forgotPasswordLabel = new JLabel("(Forgot Password?)");
        forgotPasswordLabel.setFont(smalltextFont);
        forgotPasswordLabel.setHorizontalAlignment(4);
        forgotPasswordLabel.addMouseListener(new MouseAdapter()
        {
          public void mouseClicked(MouseEvent e) {
            OperatingSystem.openLink(LauncherConstants.URL_FORGOT_PASSWORD_MINECRAFT);
          }
        });
        add(forgotPasswordLabel, constraints);

        add(Box.createVerticalStrut(10), constraints);
    }
    
     public void actionPerformed(ActionEvent e) {
        if ((e.getSource() == usernameField) || (e.getSource() == passwordField))
            tryLogIn();
     }
     
     public void tryLogIn() {
        setCanLogIn(false);
        authentication.logOut();
        authentication.setUsername(usernameField.getText());
        authentication.setPassword(String.valueOf(passwordField.getPassword()));
        final int passwordLength = passwordField.getPassword().length;

        passwordField.setText("");

   /*     launcher.getVersionManager().getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
            try {
                authentication.logIn();
                //AuthenticationDatabase authDatabase = launcher.getProfileManager().getAuthDatabase();

                if (authentication.getSelectedProfile() == null) {
                if (ArrayUtils.isNotEmpty(authentication.getAvailableProfiles())) {
                  for (GameProfile profile : authentication.getAvailableProfiles()) {
                    userDropdown.addItem(profile.getName());
                  }

                SwingUtilities.invokeLater(new Runnable()
                {
                  public void run() {
                    usernameField.setEditable(false);
                    passwordField.setEditable(false);
                    userDropdownPanel.setVisible(true);
                    popup.repack();
                    popup.setCanLogIn(true);
                    passwordField.setText(StringUtils.repeat('*', passwordLength));
                  } } );
              } else {
                String uuid = "demo-" + authentication.getUsername();
                authDatabase.register(uuid, authentication);
                popup.setLoggedIn(uuid);
              }
            } else {
              authDatabase.register(authentication.getSelectedProfile().getId(), authentication);
              popup.setLoggedIn(authentication.getSelectedProfile().getId());
            }
          } catch (UserMigratedException ex) {
            popup.getLauncher().println(ex);
            popup.getErrorForm().displayError(new String[] { "Sorry, but we can't log you in with your username.", "You have migrated your account, please use your email address." });
            popup.setCanLogIn(true);
          } catch (InvalidCredentialsException ex) {
            popup.getLauncher().println(ex);
            popup.getErrorForm().displayError(new String[] { "Sorry, but your username or password is incorrect!", "Please try again. If you need help, try the 'Forgot Password' link." });
            popup.setCanLogIn(true);
          } catch (AuthenticationException ex) {
            popup.getLauncher().println(ex);
            popup.getErrorForm().displayError(new String[] { "Sorry, but we couldn't connect to our servers.", "Please make sure that you are online and that Minecraft is not blocked." });
            popup.setCanLogIn(true);
          }
        }
      });*/
     }
     
    public void setCanLogIn(final boolean enabled) {
        if (SwingUtilities.isEventDispatchThread()) {
            launcher.getRightBarPanel().getPlayButtonPanel().getPlayButton().setEnabled(enabled);
        } else {
          SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setCanLogIn(enabled);
                } } );
        }
    }
    
    public String getUsername() {
        return usernameField.getText();
    }
}
