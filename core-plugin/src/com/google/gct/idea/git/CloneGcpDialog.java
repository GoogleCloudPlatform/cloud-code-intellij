/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.gct.idea.git;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.source.Source;
import com.google.api.services.source.model.ListReposResponse;
import com.google.api.services.source.model.Repo;
import com.google.gct.idea.elysium.ProjectSelector;
import com.google.gct.idea.util.GctBundle;
import com.google.gct.login.CredentialedUser;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.ComponentWithBrowseButton.BrowseFolderActionListener;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.util.ResourceBundle;

/**
 * The dialog that prompts the user to download (git clone) from a GCP project
 */
public class CloneGcpDialog extends DialogWrapper {

  private static final Logger LOG = Logger.getInstance(CloneGcpDialog.class);
  public static final String INVALID_FILENAME_CHARS = "[/\\\\?%*:|\"<>]";

  // Form controls
  private JPanel myRootPanel;
  private ProjectSelector myRepositoryURL;
  private TextFieldWithBrowseButton myParentDirectory;
  private JTextField myDirectoryName;

  @NotNull
  private String myDefaultDirectoryName = "";
  @NotNull
  private final Project myProject;

  public CloneGcpDialog(@NotNull Project project) {
    super(project, true);
    myProject = project;
    $$$setupUI$$$();
    init();
    initComponents();
    setTitle(GctBundle.message("clonefromgcp.title"));
    setOKButtonText(GctBundle.message("clonefromgcp.button"));
  }

  @Nullable
  public String getSourceRepositoryURL() {
    return getCurrentUrlText();
  }

  @Nullable
  public String getParentDirectory() {
    return myParentDirectory.getText();
  }

  @Nullable
  public String getDirectoryName() {
    return myDirectoryName.getText();
  }

  @Nullable
  public String getGCPUserName() {
    CredentialedUser selectedUser = myRepositoryURL.getSelectedUser();
    return selectedUser != null ? selectedUser.getEmail() : null;
  }

  private void initComponents() {
    FileChooserDescriptor fcd = FileChooserDescriptorFactory.createSingleFolderDescriptor();
    fcd.setShowFileSystemRoots(true);
    fcd.setTitle(GctBundle.message("clonefromgcp.destination.directory.title"));
    fcd.setDescription(GctBundle.message("clonefromgcp.destination.directory.description"));
    fcd.setHideIgnored(false);
    myParentDirectory.addActionListener(
        new BrowseFolderActionListener<JTextField>(fcd.getTitle(), fcd.getDescription(), myParentDirectory,
            myProject, fcd, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT) {
          @Override
          protected VirtualFile getInitialFile() {
            String text = getComponentText();
            if (text.length() == 0) {
              VirtualFile file = myProject.getBaseDir();
              if (file != null) {
                return file;
              }
            }
            return super.getInitialFile();
          }
        }
    );

    final DocumentListener updateOkButtonListener = new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        updateButtons();
      }
    };
    myParentDirectory.getChildComponent().getDocument().addDocumentListener(updateOkButtonListener);
    myParentDirectory.setText(ProjectUtil.getBaseDir());
    myDirectoryName.getDocument().addDocumentListener(updateOkButtonListener);

    setOKActionEnabled(false);
  }

  @Override
  protected String getDimensionServiceKey() {
    return "GCPCloneDialog";
  }

  @Override
  protected String getHelpId() {
    return "reference.VersionControl.Git.CloneRepository";
  }

  /**
   * Check fields and display error in the wrapper if there is a problem
   */
  private void updateButtons() {
    if (myParentDirectory.getText().length() == 0 || myDirectoryName.getText().length() == 0) {
      setErrorText(null);
      setOKActionEnabled(false);
      return;
    }
    File file = new File(myParentDirectory.getText(), myDirectoryName.getText());
    if (file.exists()) {
      setErrorText(GctBundle.message("clonefromgcp.destination.exists.error", file));
      setOKActionEnabled(false);
      return;
    } else if (!file.getParentFile().exists()) {
      setErrorText(GctBundle.message("clonefromgcp.parent.missing.error", file.getParent()));
      setOKActionEnabled(false);
      return;
    }
    setErrorText(null);
    setOKActionEnabled(true);
  }

  @Nullable
  private String getCurrentUrlText() {
    CredentialedUser selectedUser = myRepositoryURL.getSelectedUser();

    if (selectedUser == null || Strings.isNullOrEmpty(myRepositoryURL.getText())) {
      return null;
    }

    return GcpHttpAuthDataProvider.getGcpUrl(myRepositoryURL.getText());
  }

  private void createUIComponents() {
    myRepositoryURL = new ProjectSelector();
    myRepositoryURL.setMinimumSize(new Dimension(300, 0));
    myRepositoryURL.getDocument().addDocumentListener(new DocumentAdapter() {
      @SuppressWarnings("ConstantConditions")
      // This suppresses an invalid nullref warning for projectDescription.replaceAll.
      @Override
      protected void textChanged(DocumentEvent e) {
        if (myDefaultDirectoryName.equals(myDirectoryName.getText()) || myDirectoryName.getText().length() == 0) {
          // modify field if it was unmodified or blank
          String projectDescription = myRepositoryURL.getProjectDescription();
          if (!Strings.isNullOrEmpty(projectDescription)) {
            myDefaultDirectoryName = projectDescription.replaceAll(INVALID_FILENAME_CHARS, "");
            myDefaultDirectoryName = myDefaultDirectoryName.replaceAll("\\s", "");
          } else {
            myDefaultDirectoryName = "";
          }

          myDirectoryName.setText(myDefaultDirectoryName);
        }
        updateButtons();
      }
    });
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myRepositoryURL;
  }

  @Override
  protected JComponent createCenterPanel() {
    return myRootPanel;
  }

  /**
   * Method generated by IntelliJ IDEA GUI Designer >>> IMPORTANT!! <<< DO NOT edit this method OR call it in your code!
   *
   * @noinspection ALL
   */
  private void $$$setupUI$$$() {
    createUIComponents();
    myRootPanel = new JPanel();
    myRootPanel.setLayout(new GridLayoutManager(4, 4, new Insets(0, 0, 0, 0), -1, -1));
    final JLabel label1 = new JLabel();
    this.$$$loadLabelText$$$(label1,
        ResourceBundle.getBundle("messages/CloudToolsBundle").getString("clonefromgcp.projectid"));
    myRootPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final Spacer spacer1 = new Spacer();
    myRootPanel.add(spacer1,
        new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
            GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    final Spacer spacer2 = new Spacer();
    myRootPanel.add(spacer2,
        new GridConstraints(3, 1, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    final JLabel label2 = new JLabel();
    this.$$$loadLabelText$$$(label2,
        ResourceBundle.getBundle("com/intellij/dvcs/ui/DvcsBundle").getString("clone.parent.dir"));
    myRootPanel.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    myParentDirectory = new TextFieldWithBrowseButton();
    myRootPanel.add(myParentDirectory,
        new GridConstraints(1, 1, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label3 = new JLabel();
    this.$$$loadLabelText$$$(label3,
        ResourceBundle.getBundle("com/intellij/dvcs/ui/DvcsBundle").getString("clone.dir.name"));
    myRootPanel.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    myDirectoryName = new JTextField();
    myRootPanel.add(myDirectoryName,
        new GridConstraints(2, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null,
            0, false));
    myRootPanel.add(myRepositoryURL,
        new GridConstraints(0, 1, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(200, -1), null,
            null, 0, false));
    label3.setLabelFor(myDirectoryName);
  }

  /**
   * @noinspection ALL
   */
  private void $$$loadLabelText$$$(JLabel component, String text) {
    StringBuffer result = new StringBuffer();
    boolean haveMnemonic = false;
    char mnemonic = '\0';
    int mnemonicIndex = -1;
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '&') {
        i++;
        if (i == text.length()) {
          break;
        }
        if (!haveMnemonic && text.charAt(i) != '&') {
          haveMnemonic = true;
          mnemonic = text.charAt(i);
          mnemonicIndex = result.length();
        }
      }
      result.append(text.charAt(i));
    }
    component.setText(result.toString());
    if (haveMnemonic) {
      component.setDisplayedMnemonic(mnemonic);
      component.setDisplayedMnemonicIndex(mnemonicIndex);
    }
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return myRootPanel;
  }
}
