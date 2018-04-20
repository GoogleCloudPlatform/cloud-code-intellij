/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.csr;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.project.ProjectSelector;
import com.intellij.dvcs.ui.DvcsBundle;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import java.awt.*;
import java.io.File;
import java.util.Optional;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** The dialog that prompts the user to download (git clone) from a GCP project. */
public class CloneCloudRepositoryDialog extends DialogWrapper {

  private static final String INVALID_FILENAME_CHARS = "[/\\\\?%*:|\"<>]";

  private JPanel rootPanel;
  private ProjectSelector projectSelector;
  private TextFieldWithBrowseButton parentDirectory;
  private JTextField directoryName;
  private JLabel parentDirectoryLabel;
  private RepositorySelector repositorySelector;

  @NotNull private String defaultDirectoryName = "";
  @NotNull private final Project project;

  public CloneCloudRepositoryDialog(@NotNull Project project) {
    super(project, true);
    this.project = project;
    parentDirectoryLabel.setText(DvcsBundle.message("clone.parent.dir"));
    init();
    initComponents();
    setTitle(CloudReposMessageBundle.message("clonefromgcp.title"));
    setOKButtonText(CloudReposMessageBundle.message("clonefromgcp.button"));

    projectSelector.loadActiveCloudProject();
  }

  @Nullable
  public String getSourceRepositoryUrl() {
    return getCurrentUrlText();
  }

  @Nullable
  public String getParentDirectory() {
    return parentDirectory.getText();
  }

  @Nullable
  public String getDirectoryName() {
    return directoryName.getText();
  }

  @Nullable
  public String getGcpUserName() {
    return Optional.ofNullable(projectSelector.getSelectedProject())
        .map(CloudProject::googleUsername)
        .orElse(null);
  }

  private void initComponents() {
    FileChooserDescriptor fcd = FileChooserDescriptorFactory.createSingleFolderDescriptor();
    fcd.setShowFileSystemRoots(true);
    fcd.setTitle(CloudReposMessageBundle.message("clonefromgcp.destination.directory.title"));
    fcd.setDescription(
        CloudReposMessageBundle.message("clonefromgcp.destination.directory.description"));
    fcd.setHideIgnored(false);
    parentDirectory.addActionListener(
        new ComponentWithBrowseButton.BrowseFolderActionListener<JTextField>(
            fcd.getTitle(),
            fcd.getDescription(),
            parentDirectory,
            project,
            fcd,
            TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT) {
          @Override
          protected VirtualFile getInitialFile() {
            String text = getComponentText();
            if (text.length() == 0) {
              VirtualFile file = project.getBaseDir();
              if (file != null) {
                return file;
              }
            }
            return super.getInitialFile();
          }
        });

    final DocumentListener updateOkButtonListener =
        new DocumentAdapter() {
          @Override
          protected void textChanged(DocumentEvent event) {
            updateButtons();
          }
        };
    parentDirectory.getChildComponent().getDocument().addDocumentListener(updateOkButtonListener);
    parentDirectory.setText(ProjectUtil.getBaseDir());
    directoryName.getDocument().addDocumentListener(updateOkButtonListener);

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

  /** Check fields and display error in the wrapper if there is a problem. */
  private void updateButtons() {
    CloudProject selectedProject = projectSelector.getSelectedProject();
    Optional<CredentialedUser> selectedUser =
        selectedProject == null
            ? Optional.empty()
            : Services.getLoginService().getLoggedInUser(selectedProject.googleUsername());

    if (selectedProject != null && !selectedUser.isPresent()) {
      setErrorText(CloudReposMessageBundle.message("cloud.repository.dialog.invalid.project"));
      setOKActionEnabled(false);
      return;
    }

    if (!StringUtil.isEmpty(repositorySelector.getText())
        && StringUtil.isEmpty(repositorySelector.getSelectedRepository())) {
      setErrorText(CloudReposMessageBundle.message("cloud.repository.dialog.invalid.repository"));
      setOKActionEnabled(false);
      return;
    }

    if (!selectedUser.isPresent()
        || StringUtil.isEmpty(repositorySelector.getSelectedRepository())) {
      setErrorText(null);
      setOKActionEnabled(false);
      return;
    }

    if (StringUtil.isEmpty(parentDirectory.getText())
        || StringUtil.isEmpty(directoryName.getText())) {
      setErrorText(null);
      setOKActionEnabled(false);
      return;
    }

    File file = new File(parentDirectory.getText(), directoryName.getText());
    if (file.exists()) {
      setErrorText(CloudReposMessageBundle.message("clonefromgcp.destination.exists.error"));
      setOKActionEnabled(false);
      return;
    }

    if (!file.getParentFile().exists()) {
      setErrorText(CloudReposMessageBundle.message("clonefromgcp.parent.missing.error"));
      setOKActionEnabled(false);
      return;
    }

    setErrorText(null);
    setOKActionEnabled(true);
  }

  @Nullable
  private String getCurrentUrlText() {
    CloudProject selectedProject = projectSelector.getSelectedProject();
    Optional<CredentialedUser> selectedUser =
        selectedProject == null
            ? Optional.empty()
            : Services.getLoginService().getLoggedInUser(selectedProject.googleUsername());

    if (selectedProject == null
        || !selectedUser.isPresent()
        || StringUtil.isEmpty(repositorySelector.getText())) {
      return null;
    }

    return GcpHttpAuthDataProvider.getGcpUrl(
        selectedProject.projectId(), repositorySelector.getText());
  }

  private void createUIComponents() {
    projectSelector = new ProjectSelector(project);
    projectSelector.setMinimumSize(new Dimension(400, 0));
    projectSelector.addProjectSelectionListener(this::updateRepositorySelector);
    repositorySelector =
        new RepositorySelector(projectSelector.getSelectedProject(), false /*canCreateRepository*/);

    repositorySelector
        .getDocument()
        .addDocumentListener(
            new DocumentAdapter() {
              @Override
              protected void textChanged(DocumentEvent event) {
                updateButtons();
              }
            });
  }

  private void updateRepositorySelector(@NotNull CloudProject selectedProject) {
    if (defaultDirectoryName.equals(directoryName.getText())
        || directoryName.getText().length() == 0) {
      // modify field if it was unmodified or blank
      String projectDescription = selectedProject.projectName();
      if (!Strings.isNullOrEmpty(projectDescription)) {
        defaultDirectoryName = projectDescription.replaceAll(INVALID_FILENAME_CHARS, "");
        defaultDirectoryName = defaultDirectoryName.replaceAll("\\s", "");
      } else {
        defaultDirectoryName = "";
      }

      directoryName.setText(defaultDirectoryName);
    }
    repositorySelector.setCloudProject(selectedProject);
    repositorySelector.setText("");
    repositorySelector.loadRepositories();
    updateButtons();
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return projectSelector;
  }

  @Override
  protected JComponent createCenterPanel() {
    return rootPanel;
  }
}
