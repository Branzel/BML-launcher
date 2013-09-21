package net.branzel.launcher.ui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import net.branzel.launcher.Launcher;
import net.branzel.launcher.events.RefreshedVersionsListener;
import net.branzel.launcher.updater.VersionManager;
import net.branzel.launcher.updater.VersionSyncInfo;
import net.branzel.launcher.versions.ReleaseType;
import net.branzel.launcher.versions.Version;

public class VersionPanel extends JPanel implements RefreshedVersionsListener {
    private final Launcher launcher;
    private final JComboBox versionList = new JComboBox();
    private final List<ReleaseTypeCheckBox> customVersionTypes = new ArrayList();
  public static final Set<ReleaseType> DEFAULT_RELEASE_TYPES = new HashSet(Arrays.asList(new ReleaseType[] { ReleaseType.RELEASE }));
    
    public VersionPanel(Launcher launcher) {
        this.launcher = launcher;
         
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Version Selection"));

        createInterface();
        addEventHandlers();

        List versions = launcher.getVersionManager().getVersions();

        if (versions.isEmpty())
            launcher.getVersionManager().addRefreshedVersionsListener(this);
        else
            populateVersions(versions);
    }
    
    protected void createInterface() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.anchor = 17;

        constraints.gridy = 0;

        for (ReleaseType type : ReleaseType.values()) {
            if (type.getDescription() != null) {
                ReleaseTypeCheckBox checkbox = new ReleaseTypeCheckBox(type);
                // TO DOcheckbox.setSelected(editor.getProfile().getVersionFilter().getTypes().contains(type));
                customVersionTypes.add(checkbox);

                constraints.fill = 2;
                constraints.weightx = 1.0D;
                constraints.gridwidth = 0;
                add(checkbox, constraints);
                constraints.gridwidth = 1;
                constraints.weightx = 0.0D;
                constraints.fill = 0;

                constraints.gridy += 1;
            }
        }
        add(new JLabel("Use version:"), constraints);
        constraints.fill = 2;
        constraints.weightx = 1.0D;
        add(versionList, constraints);
        constraints.weightx = 0.0D;
        constraints.fill = 0;

        constraints.gridy += 1;

        versionList.setRenderer(new VersionListRenderer());
    }
    
    protected void addEventHandlers() {
        versionList.addItemListener(new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent e) {
                VersionPanel.this.updateVersionSelection();
            }
        });
        
        for (final ReleaseTypeCheckBox type : customVersionTypes)
            type.addItemListener(new ItemListener() {
                private boolean isUpdating = false;

                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (isUpdating) return;
                    if ((e.getStateChange() == 1) && (type.getType().getPopupWarning() != null)) {
                      int result = JOptionPane.showConfirmDialog(launcher.getFrame(), type.getType().getPopupWarning() + "\n\nAre you sure you want to continue?");

                      isUpdating = true;
                      if (result == 0) {
                        type.setSelected(true);
                        System.out.println("ProfileVersionPanel.this.updateCustomVersionFilter();");
                      } else {
                        type.setSelected(false);
                      }
                      isUpdating = false;
                    } else {
                        System.out.println("ProfileVersionPanel.this.updateCustomVersionFilter(); (else)");
                    }
                }
            });
    }

 /* private void updateCustomVersionFilter() {
    Profile profile = editor.getProfile();
    Set newTypes = new HashSet(Profile.DEFAULT_RELEASE_TYPES);

    for (ReleaseTypeCheckBox type : customVersionTypes) {
      if (type.isSelected())
        newTypes.add(type.getType());
      else {
        newTypes.remove(type.getType());
      }
    }

    if (newTypes.equals(Profile.DEFAULT_RELEASE_TYPES))
      profile.setAllowedReleaseTypes(null);
    else {
      profile.setAllowedReleaseTypes(newTypes);
    }

    populateVersions(editor.getLauncher().getVersionManager().getVersions(editor.getProfile().getVersionFilter()));
    editor.getLauncher().getVersionManager().removeRefreshedVersionsListener(this);
  }
*/
    private void updateVersionSelection() {
        Object selection = versionList.getSelectedItem();

        if ((selection instanceof VersionSyncInfo)) {
            Version version = ((VersionSyncInfo)selection).getLatestVersion();
            launcher.setLastVersionId(version.getId());
        } else {
            launcher.setLastVersionId(null);
        }
    }

  private void populateVersions(List<VersionSyncInfo> versions) {
    String previous = launcher.getLastVersionId();
    VersionSyncInfo selected = null;

    versionList.removeAllItems();
    versionList.addItem("Use Latest Version");

    for (VersionSyncInfo version : versions) {
      if (version.getLatestVersion().getId().equals(previous)) {
        selected = version;
      }

      versionList.addItem(version);
    }

    if ((selected == null) && (!versions.isEmpty()))
      versionList.setSelectedIndex(0);
    else
      versionList.setSelectedItem(selected);
  }

    @Override
  public void onVersionsRefreshed(VersionManager manager)
  {
    List versions = manager.getVersions();
    populateVersions(versions);
    launcher.getVersionManager().removeRefreshedVersionsListener(this);
  }

    @Override
  public boolean shouldReceiveEventsInUIThread()
  {
    return true;
  }
  private static class ReleaseTypeCheckBox extends JCheckBox {
    private final ReleaseType type;

    private ReleaseTypeCheckBox(ReleaseType type) {
      super();
      this.type = type;
    }

    public ReleaseType getType() {
      return type;
    }
  }

  private static class VersionListRenderer extends BasicComboBoxRenderer
  {
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
    {
      if ((value instanceof VersionSyncInfo)) {
        VersionSyncInfo syncInfo = (VersionSyncInfo)value;
        Version version = syncInfo.getLatestVersion();

        value = String.format("%s %s", new Object[] { version.getType().getName(), version.getId() });
      }

      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      return this;
    }
  }
}
