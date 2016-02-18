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

import com.google.gct.idea.appengine.util.CloudSdkUtil;
import com.google.gct.idea.elysium.ProjectSelector;
import com.google.gct.idea.util.GctBundle;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.remoteServer.RemoteServerConfigurable;
import com.intellij.ui.DocumentAdapter;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;

/**
 * GCP ManagedVM Cloud configuration UI.
 */
public class ManagedVmCloudConfigurable extends RemoteServerConfigurable implements Configurable {

  private final ManagedVmServerConfiguration configuration;
  @Nullable
  private final Project project;

  private String displayName = GctBundle.message("appengine.managedvm.name");
  private JPanel myMainPanel;
  private TextFieldWithBrowseButton cloudSdkLocationField;
  private ProjectSelector projectSelector;

  public ManagedVmCloudConfigurable(ManagedVmServerConfiguration configuration,
      @Nullable Project project) {
    this.configuration = configuration;
    this.project = project;

    String cloudSdkPath = CloudSdkUtil.findCloudSdkPath();
    if (cloudSdkPath != null && configuration.getCloudSdkPath() == null) {
      configuration.setCloudSdkPath(cloudSdkPath);
      cloudSdkLocationField.setText(cloudSdkPath);
    }
    cloudSdkLocationField.addBrowseFolderListener(
        GctBundle.message("appengine.cloudsdk.location.browse.window.title"),
        null,
        project,
        FileChooserDescriptorFactory.createSingleFileDescriptor()
    );
    projectSelector.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        if (projectSelector != null) {
          displayName = projectSelector.getText() + " Deployment";
        }

      }
    });

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
    return myMainPanel;
  }

  @Override
  public boolean isModified() {
    return !Comparing.strEqual(getCloudSdkLocation(), configuration.getCloudSdkPath()) ||
        !Comparing.strEqual(getCloudProjectName(), configuration.getCloudProjectName());
  }

  @Override
  public void apply() throws ConfigurationException {
    configuration.setCloudProjectName(projectSelector.getText());
    configuration.setCloudSdkPath(cloudSdkLocationField.getText());
  }

  @Override
  public void reset() {
    cloudSdkLocationField.setText(configuration.getCloudSdkPath());
    projectSelector.setText(configuration.getCloudProjectName());
  }

  /**
   * We don't need to test the connection if we know the cloud SDK, user, and project ID are valid.
   */
  @Override
  public boolean canCheckConnection() {
    return false;
  }

  public String getCloudSdkLocation() {
    return cloudSdkLocationField.getText();
  }

  public String getCloudProjectName() {
    return projectSelector.getText();
  }
}
