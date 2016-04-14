/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.cloud.tools.intellij.appengine.cloud;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration.ConfigType;
import com.google.cloud.tools.intellij.appengine.cloud.FileConfirmationDialog.DialogType;
import com.google.cloud.tools.intellij.appengine.cloud.SelectConfigDestinationFolderDialog.ConfigFileType;
import com.google.cloud.tools.intellij.ui.BrowserOpeningHyperLinkListener;
import com.google.cloud.tools.intellij.ui.PlaceholderTextField;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.base.Supplier;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.Balloon.Position;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.awt.RelativePoint;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;

/**
 * Editor for an App Engine Deployment runtime configuration.
 */
public class AppEngineDeploymentRunConfigurationEditor extends
    SettingsEditor<AppEngineDeploymentConfiguration> {

  private final Project project;

  private JComboBox configTypeComboBox;
  private JPanel appEngineConfigFilesPanel;
  private JPanel editorPanel;
  private JPanel titledPanel;
  private TextFieldWithBrowseButton appYamlPathField;
  private TextFieldWithBrowseButton dockerFilePathField;
  private JButton generateAppYamlButton;
  private JButton generateDockerfileButton;
  private JPanel userSpecifiedArtifactPanel;
  private TextFieldWithBrowseButton userSpecifiedArtifactFileSelector;
  private JTextPane appEngineCostWarningLabel;
  private PlaceholderTextField versionIdField;
  private JCheckBox versionOverrideCheckBox;
  private DeploymentSource deploymentSource;
  private AppEngineHelper appEngineHelper;

  private static final String COST_WARNING_OPEN_TAG = "<html><font face='sans' size='-1'><i>";
  private static final String COST_WARNING_CLOSE_TAG = "</i></font></html>";
  private static final String COST_WARNING_HREF_OPEN_TAG =
      "<a href='https://cloud.google.com/appengine/pricing'>";
  private static final String COST_WARNING_HREF_CLOSE_TAG = "</a>";
  public static final String DEFAULT_APP_YAML_DIR = "/src/main/appengine";
  public static final String DEFAULT_DOCKERFILE_DIR = "/src/main/docker";

  public AppEngineDeploymentRunConfigurationEditor(
      final Project project,
      final DeploymentSource deploymentSource,
      final AppEngineServerConfiguration configuration,
      final AppEngineHelper appEngineHelper) {
    this.project = project;
    this.deploymentSource = deploymentSource;
    this.appEngineHelper = appEngineHelper;

    updateCloudProjectName(appEngineHelper.getProjectId());
    configuration.setProjectNameListener(new ProjectNameListener());

    versionIdField.setPlaceholderText(GctBundle.message("appengine.flex.version.placeholder.text"));
    resetOverridableFields(versionOverrideCheckBox, versionIdField);
    updateJarWarSelector();
    userSpecifiedArtifactFileSelector.setVisible(true);

    appEngineCostWarningLabel.setText(
        GctBundle.message("appengine.flex.deployment.cost.warning",
            COST_WARNING_OPEN_TAG,
            COST_WARNING_HREF_OPEN_TAG,
            COST_WARNING_HREF_CLOSE_TAG,
            COST_WARNING_CLOSE_TAG));
    appEngineCostWarningLabel.addHyperlinkListener(new BrowserOpeningHyperLinkListener());
    appEngineCostWarningLabel.setBackground(editorPanel.getBackground());

    configTypeComboBox.setModel(new DefaultComboBoxModel(ConfigType.values()));
    configTypeComboBox.setSelectedItem(ConfigType.AUTO);
    appEngineConfigFilesPanel.setVisible(false);
    configTypeComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (getConfigType() == ConfigType.CUSTOM) {
          appEngineConfigFilesPanel.setVisible(true);

          // For user convenience, pre-fill the path fields for app.yaml and Dockerfile
          // if they already exist in their usual directories in the current project.
          if (project != null && project.getBasePath() != null) {
            if (StringUtil.isEmpty(appYamlPathField.getText())) {
              String defaultAppYamlPath =
                  project.getBasePath() + DEFAULT_APP_YAML_DIR + "/app.yaml";
              if (new File(defaultAppYamlPath).exists()) {
                appYamlPathField.setText(defaultAppYamlPath);
              }
            }
            if (StringUtil.isEmpty(dockerFilePathField.getText())) {
              String defaultDockerfilePath =
                  project.getBasePath() + DEFAULT_DOCKERFILE_DIR + "/Dockerfile";
              if (new File(defaultDockerfilePath).exists()) {
                dockerFilePathField.setText(defaultDockerfilePath);
              }
            }
          }
        } else {
          appEngineConfigFilesPanel.setVisible(false);
        }
      }
    });
    userSpecifiedArtifactFileSelector.addBrowseFolderListener(
        GctBundle.message("appengine.flex.config.user.specified.artifact.title"),
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
    userSpecifiedArtifactFileSelector.getTextField().getDocument()
        .addDocumentListener(getUserSpecifiedArtifactFileListener());
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
        new GenerateConfigActionListener(project, "app.yaml", ConfigFileType.APP_YAML,
            new Supplier<File>() {
              @Override
              public File get() {
                return appEngineHelper.defaultAppYaml();
              }
            }, appYamlPathField, userSpecifiedArtifactFileSelector));
    generateDockerfileButton.addActionListener(
        new GenerateConfigActionListener(project, "Dockerfile", ConfigFileType.DOCKERFILE,
            new Supplier<File>() {
              @Override
              public File get() {
                return appEngineHelper.defaultDockerfile(
                    DeploymentArtifactType.typeForPath(deploymentSource.getFile()));
              }
            }, dockerFilePathField, userSpecifiedArtifactFileSelector));
    versionOverrideCheckBox.addItemListener(
        new CustomFieldOverrideListener(versionOverrideCheckBox, versionIdField));
  }

  @Override
  protected void resetEditorFrom(AppEngineDeploymentConfiguration configuration) {
    userSpecifiedArtifactFileSelector.setText(configuration.getUserSpecifiedArtifactPath());
    dockerFilePathField.setText(configuration.getDockerFilePath());
    appYamlPathField.setText(configuration.getAppYamlPath());
    configTypeComboBox.setSelectedItem(configuration.getConfigType());

    versionOverrideCheckBox.setSelected(!StringUtil.isEmpty(configuration.getVersion()));
    versionIdField.setEditable(versionOverrideCheckBox.isSelected());
    if(versionOverrideCheckBox.isSelected()) {
      versionIdField.setText(configuration.getVersion());
    }
  }

  @Override
  protected void applyEditorTo(AppEngineDeploymentConfiguration configuration)
      throws ConfigurationException {
    configuration.setUserSpecifiedArtifact(isUserSpecifiedPathDeploymentSource());
    configuration.setUserSpecifiedArtifactPath(userSpecifiedArtifactFileSelector.getText());
    configuration.setDockerFilePath(dockerFilePathField.getText());
    configuration.setAppYamlPath(appYamlPathField.getText());
    configuration.setConfigType(getConfigType());
    configuration.setVersion(versionOverrideCheckBox.isSelected() ? versionIdField.getText() : null);

    updateCloudProjectName(appEngineHelper.getProjectId());
    setDeploymentSourceName(configuration.getUserSpecifiedArtifactPath());
    updateJarWarSelector();
    validateConfiguration();
  }

  private void updateCloudProjectName(String name) {
    TitledBorder border = (TitledBorder) titledPanel.getBorder();
    border.setTitle(GctBundle.message("appengine.config.project.panel.title", name));
    titledPanel.repaint();
    titledPanel.revalidate();
  }

  private void updateJarWarSelector() {
    userSpecifiedArtifactPanel.setVisible(isUserSpecifiedPathDeploymentSource());
  }

  private void resetOverridableFields(JCheckBox overrideCheckbox, JTextField field) {
    field.setEditable(overrideCheckbox.isSelected());
  }

  /**
   * The name of the currently selected deployment source is displayed in the Application Servers window.
   * We want this name to also include the path to the manually chosen archive when one is selected.
   */
  private void setDeploymentSourceName(String filePath) {
    if(isUserSpecifiedPathDeploymentSource() && !StringUtil.isEmpty(userSpecifiedArtifactFileSelector.getText())) {
      ((UserSpecifiedPathDeploymentSource) deploymentSource).setName(
          GctBundle.message(
              "appengine.flex.user.specified.deploymentsource.name.with.filename",
              new File(filePath).getName()));
    }
  }

  private void validateConfiguration() throws ConfigurationException {
    if (isUserSpecifiedPathDeploymentSource() && (StringUtil.isEmpty(userSpecifiedArtifactFileSelector.getText())
        || !isJarOrWar(userSpecifiedArtifactFileSelector.getText()))) {
      throw new ConfigurationException(
          GctBundle.message("appengine.flex.config.user.specified.artifact.error"));
    } else if (!isUserSpecifiedPathDeploymentSource() && !deploymentSource.isValid()) {
      throw new ConfigurationException(
          GctBundle.message("appengine.config.deployment.source.error"));
    } else if(versionOverrideCheckBox.isSelected() && StringUtils.isBlank(versionIdField.getText())) {
      throw new ConfigurationException(GctBundle.message("appengine.config.version.error"));
    } else if (getConfigType() == ConfigType.CUSTOM) {
      if (StringUtils.isBlank(appYamlPathField.getText())) {
        throw new ConfigurationException(
            GctBundle.message("appengine.flex.config.custom.app.yaml.error"));
      } else if (StringUtils.isBlank(dockerFilePathField.getText())) {
        throw new ConfigurationException(
            GctBundle.message("appengine.flex.config.custom.dockerfile.error"));
      }
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

  private DocumentAdapter getUserSpecifiedArtifactFileListener() {
    return new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        if(isUserSpecifiedPathDeploymentSource()) {
          ((UserSpecifiedPathDeploymentSource) deploymentSource).setFilePath(
              userSpecifiedArtifactFileSelector.getText());
        }
      }
    };
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
  private class GenerateConfigActionListener implements ActionListener {

    private final Project project;
    private final String fileName;
    private final ConfigFileType configFileType;
    private final TextFieldWithBrowseButton filePicker;
    private final Supplier<File> sourceFileProvider;
    private final JPanel fileSelector;

    public GenerateConfigActionListener(
        Project project,
        String fileName,
        ConfigFileType configFileType,
        Supplier<File> sourceFileProvider,
        TextFieldWithBrowseButton filePicker,
        JPanel fileSelector) {
      this.project = project;
      this.fileName = fileName;
      this.configFileType = configFileType;
      this.sourceFileProvider = sourceFileProvider;
      this.filePicker = filePicker;
      this.fileSelector = fileSelector;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      if (sourceFileProvider.get() == null) {
        if (!isUserSpecifiedPathDeploymentSource()) {
          throw new AssertionError("Error generating configuration file: "
              + "artifact deployment source is missing a source .jar or .war file.");
        }

        BalloonBuilder builder = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(
                GctBundle.getString("appengine.config.deployment.source.error"),
                MessageType.INFO, null)
            .setFadeoutTime(3000);
        Balloon balloon = builder.createBalloon();
        balloon.show(
            new RelativePoint(fileSelector,
                new Point(fileSelector.getWidth() / 2, fileSelector.getHeight() / 2)),
            Position.above);
        return;
      }

      SelectConfigDestinationFolderDialog destinationFolderDialog = new
          SelectConfigDestinationFolderDialog(project, configFileType);
      if (destinationFolderDialog.showAndGet()) {
        File destinationFolderPath = destinationFolderDialog.getDestinationFolder();
        File destinationFilePath = new File(destinationFolderPath, fileName);

        if (destinationFilePath.exists()) {
          if (!new FileConfirmationDialog(
              project, DialogType.CONFIRM_OVERWRITE, destinationFilePath).showAndGet()) {
            return;
          }
        } else if (destinationFolderPath.isFile()) {
          new FileConfirmationDialog(
              project, DialogType.NOT_DIRECTORY_ERROR, destinationFolderPath).show();
          return;
        } else if (!destinationFolderPath.exists()) {
          if (!new FileConfirmationDialog(
              project, DialogType.CONFIRM_CREATE_DIR, destinationFolderPath).showAndGet()) {
            return;
          }
        }

        try {
          FileUtil.copy(sourceFileProvider.get(), destinationFilePath);
          LocalFileSystem.getInstance().refreshAndFindFileByIoFile(destinationFilePath);
        } catch (IOException e) {
          String message = GctBundle.message(
              "appengine.flex.config.generation.io.error", destinationFilePath.getName());
          Messages.showErrorDialog(project, message + e.getLocalizedMessage(), "Error");
          return;
        }
        filePicker.setText(destinationFilePath.getPath());
      }
    }
  }

  private class CustomFieldOverrideListener implements ItemListener {
    private JCheckBox overrideCheckbox;
    private JTextField field;

    public CustomFieldOverrideListener(JCheckBox overrideCheckbox, JTextField field) {
      this.overrideCheckbox = overrideCheckbox;
      this.field = field;
    }

    @Override
    public void itemStateChanged(ItemEvent itemEvent) {
      resetOverridableFields(overrideCheckbox, field);
    }
  }

  private class ProjectNameListener implements PropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      updateCloudProjectName((String) evt.getNewValue());
    }
  }
}
