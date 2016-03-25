/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.google.gct.idea.appengine.cloud;

import com.google.common.annotations.VisibleForTesting;
import com.google.gct.idea.appengine.util.CloudSdkUtil;
import com.google.gct.idea.elysium.ProjectSelector;
import com.google.gct.idea.util.GctBundle;
import com.google.gct.idea.util.SystemEnvironmentProvider;
import com.google.gct.login.CredentialedUser;

import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.remoteServer.RemoteServerConfigurable;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;

/**
 * GCP ManagedVM Cloud configuration UI.
 */
public class ManagedVmCloudConfigurable extends RemoteServerConfigurable implements Configurable {

  private final ManagedVmServerConfiguration configuration;
  private final SystemEnvironmentProvider environmentProvider;

  private String displayName = GctBundle.message("appengine.managedvm.name");
  private JPanel mainPanel;
  private TextFieldWithBrowseButton cloudSdkDirectoryField;
  private ProjectSelector projectSelector;
  private JLabel warningMessage;

  public ManagedVmCloudConfigurable(ManagedVmServerConfiguration configuration,
      @Nullable Project project) {
    this.configuration = configuration;
    environmentProvider = SystemEnvironmentProvider.getInstance();

    warningMessage.setVisible(false);

    final String cloudSdkDirectoryPath = CloudSdkUtil.findCloudSdkDirectoryPath(environmentProvider);

    if (cloudSdkDirectoryPath != null
        && configuration.getCloudSdkHomePath() == null) {
      configuration.setCloudSdkHomePath(cloudSdkDirectoryPath);
      cloudSdkDirectoryField.setText(cloudSdkDirectoryPath);
    }

    cloudSdkDirectoryField.addBrowseFolderListener(
        GctBundle.message("appengine.cloudsdk.location.browse.window.title"),
        null,
        project,
        FileChooserDescriptorFactory.createSingleFolderDescriptor()
    );

    cloudSdkDirectoryField.getTextField().getDocument()
        .addDocumentListener(getSdkDirectoryFieldListener());
  }

  private DocumentAdapter getSdkDirectoryFieldListener() {
    return new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        String path = cloudSdkDirectoryField.getText();
        boolean isValid = CloudSdkUtil.containsCloudSdkExecutable(path);
        if (isValid) {
          cloudSdkDirectoryField.getTextField().setForeground(JBColor.black);
          warningMessage.setVisible(false);
        } else {
          cloudSdkDirectoryField.getTextField().setForeground(JBColor.red);
          warningMessage.setVisible(true);
          warningMessage.setText(
              GctBundle.message("appengine.cloudsdk.location.missing.message"));
        }
      }
    };
  }

  @VisibleForTesting
  ProjectSelector getProjectSelector() {
    return projectSelector;
  }

  @VisibleForTesting
  JLabel getWarningMessage() {
    return warningMessage;
  }

  @VisibleForTesting
  TextFieldWithBrowseButton getCloudSdkDirectoryField() {
    return cloudSdkDirectoryField;
  }

  @Nls
  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return null;
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    return mainPanel;
  }

  @Override
  public boolean isModified() {
    boolean isSdkDirModified = !Comparing.strEqual(getCloudSdkDirectory(),
        configuration.getCloudSdkHomePath());
    boolean isProjectModified = !Comparing.strEqual(getCloudProjectName(),
        configuration.getCloudProjectName());
    boolean isUserModified = !Comparing.strEqual(getGoogleUserName(),
        configuration.getGoogleUserName());

    return isSdkDirModified || isProjectModified || isUserModified;
  }

  @Override
  public void apply() throws ConfigurationException {
    if (StringUtil.isEmpty(cloudSdkDirectoryField.getText())
        || !CloudSdkUtil.containsCloudSdkExecutable(cloudSdkDirectoryField.getText())) {
      throw new RuntimeConfigurationError(
          GctBundle.message("appengine.cloudsdk.location.missing.message"));
    }
    else if (StringUtil.isEmpty(projectSelector.getText())) {
      throw new RuntimeConfigurationError(
          GctBundle.message("appengine.cloudsdk.project.missing.message"));
    }
    else {
      configuration.setCloudProjectName(projectSelector.getText());
      CredentialedUser selectedUser = projectSelector.getSelectedUser();
      if (selectedUser != null) {
        configuration.setGoogleUserName(selectedUser.getEmail());
      }
      configuration.setCloudSdkHomePath(cloudSdkDirectoryField.getText());
    }
  }

  @Override
  public void reset() {
    cloudSdkDirectoryField.setText(configuration.getCloudSdkHomePath());
    projectSelector.setText(configuration.getCloudProjectName());
  }

  /**
   * We don't need to test the connection if we know the cloud SDK, user, and project ID are valid.
   */
  @Override
  public boolean canCheckConnection() {
    return false;
  }

  public String getCloudSdkDirectory() {
    return cloudSdkDirectoryField.getText();
  }

  public String getCloudProjectName() {
    return projectSelector.getText();
  }

  @Nullable
  public String getGoogleUserName() {
    CredentialedUser selectedUser = projectSelector.getSelectedUser();
    if (selectedUser == null) {
      return null;
    }
    return selectedUser.getEmail();
  }
}
