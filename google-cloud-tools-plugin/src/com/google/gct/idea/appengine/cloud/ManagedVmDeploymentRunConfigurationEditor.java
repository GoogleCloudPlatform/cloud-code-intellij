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

import com.google.common.base.Supplier;
import com.google.gct.idea.appengine.cloud.ManagedVmDeploymentConfiguration.ConfigType;
import com.google.gct.idea.util.GctBundle;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Editor for a ManagedVM Deployment runtime configuration.
 */
public class ManagedVmDeploymentRunConfigurationEditor extends
    SettingsEditor<ManagedVmDeploymentConfiguration> {

  private final Project project;

  private JComboBox configTypeComboBox;
  private JPanel mvmConfigFilesPanel;
  private JPanel mainPanel;
  private TextFieldWithBrowseButton appYamlPathField;
  private TextFieldWithBrowseButton dockerFilePathField;
  private JButton generateAppYamlButton;
  private JButton generateDockerfileButton;
  private AppEngineHelper appEngineHelper;


  public ManagedVmDeploymentRunConfigurationEditor(final Project project,
      final DeploymentSource source, final AppEngineHelper appEngineHelper) {
    this.project = project;
    this.appEngineHelper = appEngineHelper;
    configTypeComboBox.setModel(new DefaultComboBoxModel(ConfigType.values()));
    configTypeComboBox.setSelectedItem(ConfigType.AUTO);
    mvmConfigFilesPanel.setVisible(false);
    configTypeComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (getConfigType() == ConfigType.CUSTOM) {
          mvmConfigFilesPanel.setVisible(true);
        } else {
          mvmConfigFilesPanel.setVisible(false);
        }
      }
    });
    dockerFilePathField.addBrowseFolderListener(
        GctBundle.message("appengine.dockerfile.location.browse.button"),
        null,
        project,
        FileChooserDescriptorFactory.createSingleFileDescriptor());
    appYamlPathField.addBrowseFolderListener(
        GctBundle.message("appengine.appyaml.location.browse.window.title"),
        null,
        project,
        FileChooserDescriptorFactory.createSingleFileDescriptor());
    generateAppYamlButton.addActionListener(
        new GenerateConfigActionListener(project, "app.yaml", new Supplier<File>() {
          @Override
          public File get() {
            return appEngineHelper.defaultAppYaml();
          }
        }, appYamlPathField));
    generateDockerfileButton.addActionListener(
        new GenerateConfigActionListener(project, "Dockerfile", new Supplier<File>() {
          @Override
          public File get() {
            return appEngineHelper
                .defaultDockerfile(DeploymentArtifactType.typeForPath(source.getFile()));
          }
        }, dockerFilePathField));
  }


  @Override
  protected void resetEditorFrom(ManagedVmDeploymentConfiguration configuration) {
    dockerFilePathField.setText(configuration.getDockerFilePath());
    appYamlPathField.setText(configuration.getAppYamlPath());
    configTypeComboBox.setSelectedItem(configuration.getConfigType());
  }

  @Override
  protected void applyEditorTo(ManagedVmDeploymentConfiguration configuration)
      throws ConfigurationException {
    configuration.setDockerFilePath(dockerFilePathField.getText());
    configuration.setAppYamlPath(appYamlPathField.getText());
    configuration.setConfigType(getConfigType());
  }

  @Nullable
  private ConfigType getConfigType() {
    int selectedIndex = configTypeComboBox.getSelectedIndex();
    if (selectedIndex == -1) {
      return null;
    }
    return (ConfigType) configTypeComboBox.getItemAt(selectedIndex);
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    return mainPanel;
  }

  /**
   * A somewhat generic way of generating a file for a {@link TextFieldWithBrowseButton}.
   */
  private class GenerateConfigActionListener implements ActionListener {

    private final Project project;
    private final String fileName;
    private final TextFieldWithBrowseButton filePicker;
    private final Supplier<File> sourceFileProvider;

    public GenerateConfigActionListener(
        Project project,
        String fileName,
        Supplier<File> sourceFileProvider,
        TextFieldWithBrowseButton filePicker) {
      this.project = project;
      this.fileName = fileName;
      this.sourceFileProvider = sourceFileProvider;
      this.filePicker = filePicker;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      SelectConfigDestinationFolderDialog destinationFolderDialog = new
          SelectConfigDestinationFolderDialog(project);
      if (destinationFolderDialog.showAndGet()) {
        File destinationFolderPath = destinationFolderDialog.getDestinationFolder();
        File destinationFilePath = new File(destinationFolderPath, fileName);
        try {
          FileUtil.copy(sourceFileProvider.get(), destinationFilePath);
          LocalFileSystem.getInstance().refreshAndFindFileByIoFile(destinationFilePath);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        filePicker.setText(destinationFilePath.getPath());
      }
    }
  }
}
