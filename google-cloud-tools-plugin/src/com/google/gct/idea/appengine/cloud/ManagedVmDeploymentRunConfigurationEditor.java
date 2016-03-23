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
import com.google.common.eventbus.Subscribe;
import com.google.gct.idea.appengine.cloud.ManagedVmCloudType.ManagedVmDeploymentConfigurator.UserSpecifiedPathDeploymentSource;
import com.google.gct.idea.appengine.cloud.ManagedVmDeploymentConfiguration.ConfigType;
import com.google.gct.idea.util.GctBundle;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
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
import javax.swing.border.TitledBorder;

/**
 * Editor for a ManagedVM Deployment runtime configuration.
 */
public class ManagedVmDeploymentRunConfigurationEditor extends
    SettingsEditor<ManagedVmDeploymentConfiguration> {

  private final Project project;

  private JComboBox configTypeComboBox;
  private JPanel mvmConfigFilesPanel;
  private JPanel editorPanel;
  private JPanel titledPanel;
  private TextFieldWithBrowseButton appYamlPathField;
  private TextFieldWithBrowseButton dockerFilePathField;
  private JButton generateAppYamlButton;
  private JButton generateDockerfileButton;
  private JPanel userSpecifiedArtifactPanel;
  private TextFieldWithBrowseButton userSpecifiedArtifactFileSelector;
  private DeploymentSource deploymentSource;
  private AppEngineHelper appEngineHelper;

  public ManagedVmDeploymentRunConfigurationEditor(
      final Project project,
      final DeploymentSource deploymentSource,
      final ManagedVmServerConfiguration configuration,
      final AppEngineHelper appEngineHelper) {
    this.project = project;
    this.deploymentSource = deploymentSource;
    this.appEngineHelper = appEngineHelper;

    updateCloudProjectName(appEngineHelper.getProjectId());
    configuration.getProjectNameListener().register(new ProjectNameListener());

    updateJarWarSelector();
    userSpecifiedArtifactFileSelector.setVisible(true);

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
    userSpecifiedArtifactFileSelector.addBrowseFolderListener(
        GctBundle.message("appengine.managedvm.config.user.specified.artifact.title"),
        null,
        project,
        FileChooserDescriptorFactory.createSingleFileDescriptor().withFileFilter(new Condition<VirtualFile>() {
          @Override
          public boolean value(VirtualFile file) {
            return Comparing.equal(file.getExtension(), "jar", SystemInfo.isFileSystemCaseSensitive)
                || Comparing.equal(file.getExtension(), "war", SystemInfo.isFileSystemCaseSensitive);
          }
        })
    );
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
                .defaultDockerfile(DeploymentArtifactType.typeForPath(deploymentSource.getFile()));
          }
        }, dockerFilePathField));
  }


  @Override
  protected void resetEditorFrom(ManagedVmDeploymentConfiguration configuration) {
    userSpecifiedArtifactFileSelector.setText(configuration.getUserSpecifiedArtifactPath());
    dockerFilePathField.setText(configuration.getDockerFilePath());
    appYamlPathField.setText(configuration.getAppYamlPath());
    configTypeComboBox.setSelectedItem(configuration.getConfigType());
  }

  @Override
  protected void applyEditorTo(ManagedVmDeploymentConfiguration configuration)
      throws ConfigurationException {
    configuration.setUserSpecifiedArtifact(isUserSpecifiedPathDeploymentSource());
    configuration.setUserSpecifiedArtifactPath(userSpecifiedArtifactFileSelector.getText());
    configuration.setDockerFilePath(dockerFilePathField.getText());
    configuration.setAppYamlPath(appYamlPathField.getText());
    configuration.setConfigType(getConfigType());

    updateCloudProjectName(appEngineHelper.getProjectId());
    setDeploymentSourceName(configuration.getUserSpecifiedArtifactPath());
    updateJarWarSelector();
    validateConfiguration();
  }

  private void updateCloudProjectName(String name) {
    TitledBorder border = (TitledBorder) titledPanel.getBorder();
    border.setTitle(GctBundle.message("appengine.managedvm.config.project.panel.title", name));
    titledPanel.repaint();
    titledPanel.revalidate();
  }

  private void updateJarWarSelector() {
      userSpecifiedArtifactPanel.setVisible(isUserSpecifiedPathDeploymentSource());
  }

  /**
   * The name of the currently selected deployment source is displayed in the Application Servers window.
   * We want this name to also include the path to the manually chosen archive when one is selected.
   */
  private void setDeploymentSourceName(String filePath) {
    if(isUserSpecifiedPathDeploymentSource() && !StringUtil.isEmpty(userSpecifiedArtifactFileSelector.getText())) {
      ((UserSpecifiedPathDeploymentSource) deploymentSource).setName(
          GctBundle.message(
              "appengine.managedvm.user.specified.deploymentsource.name.with.filename",
              new File(filePath).getName()));
    }
  }

  private void validateConfiguration() throws ConfigurationException {
    if (isUserSpecifiedPathDeploymentSource() && (StringUtil.isEmpty(userSpecifiedArtifactFileSelector.getText())
        || !isJarOrWar(userSpecifiedArtifactFileSelector.getText()))) {
      throw new ConfigurationException(
          GctBundle.message("appengine.managedvm.config.user.specified.artifact.error"));
    } else if (!isUserSpecifiedPathDeploymentSource() && !deploymentSource.isValid()) {
      throw new ConfigurationException(
          GctBundle.message("appengine.managedvm.config.deployment.source.error"));
    }
  }

  private boolean isJarOrWar(String path) {
    File file = new File(path);
    if (file.isDirectory()) {
      return false;
    }
    String name = file.getName();
    return StringUtil.endsWithIgnoreCase(name, ".jar") || StringUtil.endsWithIgnoreCase(name, ".war");
  }

  private boolean isUserSpecifiedPathDeploymentSource() {
    return deploymentSource instanceof UserSpecifiedPathDeploymentSource;
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
    return editorPanel;
  }

  /**
   * A somewhat generic way of generating a file for a {@link TextFieldWithBrowseButton}.
   */
  private static class GenerateConfigActionListener implements ActionListener {

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

  private class ProjectNameListener {
    @Subscribe
    public void handleProjectNameChange(String name) {
      updateCloudProjectName(name);
    }
  }
}
