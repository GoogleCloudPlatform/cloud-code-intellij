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

import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.project.ProjectSelector;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import git4idea.repo.GitRepository;
import java.awt.*;
import java.util.Optional;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shows the upload to Google Cloud Source Repositories dialog for initializing a project on a GCP
 * repository.
 */
public class SetupCloudRepositoryDialog extends DialogWrapper {

  private JPanel rootPanel;
  private ProjectSelector projectSelector;
  private RepositorySelector repositorySelector;
  private RepositoryRemotePanel remoteNameSelector;
  private String projectId;
  private String repositoryId;
  private String remoteName;
  private CredentialedUser credentialedUser;
  @NotNull private final Project ideProject;
  private GitRepository gitRepository;

  public SetupCloudRepositoryDialog(
      @NotNull Project ideProject,
      @Nullable GitRepository gitRepository,
      @NotNull String title,
      @NotNull String okText) {
    super(ideProject, true);
    this.ideProject = ideProject;

    this.gitRepository = gitRepository;

    init();
    setTitle(title);
    setOKButtonText(okText);
    setOKActionEnabled(false);

    projectSelector.loadActiveCloudProject();
  }

  /** Return the project ID selected by the user. */
  @NotNull
  public String getProjectId() {
    return projectId;
  }

  @NotNull
  String getRepositoryId() {
    return repositoryId;
  }

  @NotNull
  public String getRemoteName() {
    return remoteName;
  }

  /** Return the credentialed user that owns the ID returned from {@link #getProjectId()}. */
  @Nullable
  public CredentialedUser getCredentialedUser() {
    return credentialedUser;
  }

  @Override
  protected String getDimensionServiceKey() {
    return "SetupCloudRepositoryDialog";
  }

  private void createUIComponents() {
    projectSelector = new ProjectSelector(ideProject);
    projectSelector.setMinimumSize(new Dimension(400, 0));

    repositorySelector =
        new RepositorySelector(projectSelector.getSelectedProject(), true /*canCreateRepository*/);

    remoteNameSelector = new RepositoryRemotePanel(gitRepository);

    projectSelector.addProjectSelectionListener(this::updateRepositorySelector);

    repositorySelector
        .getDocument()
        .addDocumentListener(
            new DocumentAdapter() {
              @Override
              protected void textChanged(DocumentEvent event) {
                remoteNameSelector.update(repositorySelector.getSelectedRepository());
                updateButtons();
              }
            });

    remoteNameSelector
        .getRemoteNameField()
        .getDocument()
        .addDocumentListener(
            new DocumentAdapter() {
              @Override
              protected void textChanged(DocumentEvent event) {
                updateButtons();
              }
            });
  }

  private void updateRepositorySelector(CloudProject cloudProject) {
    repositorySelector.setCloudProject(cloudProject);
    repositorySelector.setText("");
    repositorySelector.loadRepositories();
    updateButtons();
  }

  private void updateButtons() {
    CloudProject selectedProject = projectSelector.getSelectedProject();
    Optional<CredentialedUser> user =
        Services.getLoginService().getLoggedInUser(selectedProject.googleUsername());

    if (!StringUtil.isEmpty(selectedProject.projectId()) && !user.isPresent()) {
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

    if (!user.isPresent() || StringUtil.isEmpty(repositorySelector.getSelectedRepository())) {
      setErrorText(null);
      setOKActionEnabled(false);
      return;
    }

    if (StringUtil.isEmpty(remoteNameSelector.getText())) {
      setErrorText(CloudReposMessageBundle.message("uploadtogcp.dialog.missing.remote"));
      setOKActionEnabled(false);
      return;
    }

    setErrorText(null);
    setOKActionEnabled(true);
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

  @Override
  protected void doOKAction() {
    CloudProject selectedProject = projectSelector.getSelectedProject();
    projectId = selectedProject.projectId();
    repositoryId = repositorySelector.getText();
    remoteName = remoteNameSelector.getText();
    credentialedUser =
        Services.getLoginService().getLoggedInUser(selectedProject.googleUsername()).orElse(null);
    super.doOKAction();
  }
}
