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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.appengine.v1.model.Application;
import com.google.cloud.tools.intellij.appengine.application.AppEngineAdminService;
import com.google.cloud.tools.intellij.appengine.application.AppEngineApplicationCreateDialog;
import com.google.cloud.tools.intellij.appengine.application.GoogleApiException;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration.ConfigType;
import com.google.cloud.tools.intellij.appengine.cloud.FileConfirmationDialog.DialogType;
import com.google.cloud.tools.intellij.appengine.cloud.SelectConfigDestinationFolderDialog.ConfigFileType;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.resources.ProjectSelector;
import com.google.cloud.tools.intellij.resources.ProjectSelector.ProjectSelectionChangedEvent;
import com.google.cloud.tools.intellij.resources.ProjectSelector.ProjectSelectionListener;
import com.google.cloud.tools.intellij.ui.BrowserOpeningHyperLinkListener;
import com.google.cloud.tools.intellij.ui.PlaceholderTextField;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
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
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;

import git4idea.DialogManager;

/**
 * Editor for an App Engine Deployment runtime configuration.
 */
public class AppEngineDeploymentRunConfigurationEditor extends
    SettingsEditor<AppEngineDeploymentConfiguration> {
  private Project project;

  private JComboBox configTypeComboBox;
  private JPanel appEngineConfigFilesPanel;
  private JPanel editorPanel;
  private TextFieldWithBrowseButton appYamlPathField;
  private TextFieldWithBrowseButton dockerFilePathField;
  private JButton generateAppYamlButton;
  private JButton generateDockerfileButton;
  private JPanel userSpecifiedArtifactPanel;
  private TextFieldWithBrowseButton userSpecifiedArtifactFileSelector;
  private JTextPane appEngineCostWarningLabel;
  private PlaceholderTextField versionIdField;
  private JCheckBox versionOverrideCheckBox;
  private ProjectSelector projectSelector;
  private JLabel environmentLabel;
  private JPanel appEngineFlexConfigPanel;
  private JCheckBox promoteCheckbox;
  private JCheckBox stopPreviousVersionCheckbox;
  private JLabel stopPreviousVersionLabel;
  private JTextPane promoteInfoLabel;
  private JTextPane regionLabel;
  private CreateApplicationLinkListener createApplicationListener;

  private DeploymentSource deploymentSource;
  private AppEngineEnvironment environment;

  private static final String LABEL_OPEN_TAG = "<html><font face='sans' size='-1'>";
  private static final String LABEL_CLOSE_TAG = "</font></html>";
  private static final String LABEL_HREF_CLOSE_TAG = "</a>";

  private static final String PROMOTE_INFO_HREF_OPEN_TAG =
      "<a href='https://console.cloud.google.com/appengine/versions'>";
  private static final String COST_WARNING_HREF_OPEN_TAG =
      "<a href='https://cloud.google.com/appengine/pricing'>";
  private static final String CREATE_APPLICATION_HREF_OPEN_TAG = "<a href='#'>";

  public static final String DEFAULT_APP_YAML_DIR = "/src/main/appengine";
  public static final String DEFAULT_DOCKERFILE_DIR = "/src/main/docker";

  public static final boolean PROMOTE_DEFAULT = true;
  public static final boolean STOP_PREVIOUS_VERSION_DEFAULT = true;

  /**
   * Initializes the UI components.
   */
  public AppEngineDeploymentRunConfigurationEditor(
      final Project project,
      final AppEngineDeployable deploymentSource,
      final AppEngineHelper appEngineHelper) {
    this.project = project;
    this.deploymentSource = deploymentSource;

    versionIdField.setPlaceholderText(GctBundle.message("appengine.flex.version.placeholder.text"));
    promoteCheckbox.setSelected(PROMOTE_DEFAULT);
    stopPreviousVersionCheckbox.setSelected(STOP_PREVIOUS_VERSION_DEFAULT);

    resetOverridableFields(versionOverrideCheckBox, versionIdField);
    updateJarWarSelector();
    userSpecifiedArtifactFileSelector.setVisible(true);

    environment = deploymentSource.getEnvironment();

    promoteInfoLabel.setText(
        GctBundle.message("appengine.promote.info.label",
            LABEL_OPEN_TAG,
            PROMOTE_INFO_HREF_OPEN_TAG,
            LABEL_HREF_CLOSE_TAG,
            LABEL_CLOSE_TAG));
    promoteInfoLabel.addHyperlinkListener(new BrowserOpeningHyperLinkListener());

    if (environment.isFlexible()) {
      appEngineCostWarningLabel.setText(
          GctBundle.message("appengine.flex.deployment.cost.warning",
              LABEL_OPEN_TAG,
              COST_WARNING_HREF_OPEN_TAG,
              LABEL_HREF_CLOSE_TAG,
              LABEL_CLOSE_TAG));
      appEngineCostWarningLabel.addHyperlinkListener(new BrowserOpeningHyperLinkListener());
      appEngineCostWarningLabel.setBackground(editorPanel.getBackground());
      environmentLabel.setText(getEnvironmentDisplayableLabel());
    } else {
      appEngineCostWarningLabel.setVisible(false);
      environmentLabel.setText(environment.localizedLabel());
      stopPreviousVersionLabel.setVisible(false);
      stopPreviousVersionCheckbox.setVisible(false);
    }

    configTypeComboBox.setModel(new DefaultComboBoxModel(ConfigType.values()));
    configTypeComboBox.setSelectedItem(ConfigType.AUTO);
    appEngineConfigFilesPanel.setVisible(false);
    configTypeComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent event) {
        if (getConfigType() == ConfigType.CUSTOM) {
          appEngineConfigFilesPanel.setVisible(true);

          // For user convenience, pre-fill the path fields for app.yaml and Dockerfile
          // if they already exist in their usual directories in the current project.
          if (project != null && project.getBasePath() != null) {
            if (StringUtil.isEmpty(appYamlPathField.getText())) {
              Path defaultAppYamlPath = Paths.get(
                  project.getBasePath() + DEFAULT_APP_YAML_DIR + "/app.yaml");
              if (Files.exists(defaultAppYamlPath)) {
                appYamlPathField.setText(defaultAppYamlPath.toString());
              }
            }
            if (StringUtil.isEmpty(dockerFilePathField.getText())) {
              Path defaultDockerfilePath = Paths.get(
                  project.getBasePath() + DEFAULT_DOCKERFILE_DIR + "/Dockerfile");
              if (Files.exists(defaultDockerfilePath)) {
                dockerFilePathField.setText(defaultDockerfilePath.toString());
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
        FileChooserDescriptorFactory.createSingleFileDescriptor()
            .withFileFilter(new Condition<VirtualFile>() {
              @Override
              public boolean value(VirtualFile file) {
                return Comparing.equal(
                        file.getExtension(), "jar", SystemInfo.isFileSystemCaseSensitive)
                    || Comparing.equal(
                        file.getExtension(), "war", SystemInfo.isFileSystemCaseSensitive);
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
            new Supplier<Path>() {
              @Override
              public Path get() {
                return appEngineHelper.defaultAppYaml();
              }
            }, appYamlPathField, userSpecifiedArtifactFileSelector));
    generateDockerfileButton.addActionListener(
        new GenerateConfigActionListener(project, "Dockerfile", ConfigFileType.DOCKERFILE,
            new Supplier<Path>() {
              @Override
              public Path get() {
                return appEngineHelper.defaultDockerfile(
                    AppEngineFlexDeploymentArtifactType.typeForPath(
                        Paths.get(deploymentSource.getFilePath())));
              }
            }, dockerFilePathField, userSpecifiedArtifactFileSelector));
    versionOverrideCheckBox.addItemListener(
        new CustomFieldOverrideListener(versionOverrideCheckBox, versionIdField));
    promoteCheckbox.addItemListener(new PromoteListener());

    appEngineFlexConfigPanel.setVisible(
        environment == AppEngineEnvironment.APP_ENGINE_FLEX
            && !AppEngineProjectService.getInstance().isFlexCompat(project, deploymentSource));

    // TODO updateRegionField() with any persisted configs

    projectSelector.addProjectSelectionListener(new ProjectSelectionListener() {
      @Override
      public void selectionChanged(ProjectSelectionChangedEvent event) {
        updateRegionField(event.getSelectedProject().getProjectId(), event.getUser().getCredential());
      }
    });

    createApplicationListener = new CreateApplicationLinkListener();
    regionLabel.addHyperlinkListener(createApplicationListener);
  }

  private void updateRegionField(String projectId, Credential credential) {
    try {
      Application application =
          AppEngineAdminService.getInstance().getApplicationForProjectId(projectId, credential);

      if (application != null) {
        setRegionLabelText(application.getLocationId(), false);
      } else {
        createApplicationListener.setCredential(credential);
        createApplicationListener.setProjectId(projectId);
        setRegionLabelText(GctBundle.message("appengine.application.not.exist") + " "
            + GctBundle.message("appengine.application.create",
            CREATE_APPLICATION_HREF_OPEN_TAG, LABEL_HREF_CLOSE_TAG), true);

        // TODO should the region (or the presence of the application be part of the Configuration?
        //  TODO  - this might be necessary if we want to use this to mark the config as invalid
      }
    } catch (IOException | GoogleApiException e) {
      setRegionLabelText(GctBundle.message("appengine.application.region.fetch.error"), true);
    }
  }

  private void setRegionLabelText(String text, boolean isErrorState) {
    regionLabel.setText(LABEL_OPEN_TAG + text + LABEL_CLOSE_TAG);
    regionLabel.setForeground(isErrorState ? JBColor.red : JBColor.black);
  }

  @Override
  protected void resetEditorFrom(AppEngineDeploymentConfiguration configuration) {
    projectSelector.setText(configuration.getCloudProjectName());
    userSpecifiedArtifactFileSelector.setText(configuration.getUserSpecifiedArtifactPath());
    dockerFilePathField.setText(configuration.getDockerFilePath());
    appYamlPathField.setText(configuration.getAppYamlPath());
    configTypeComboBox.setSelectedItem(configuration.getConfigType());

    versionOverrideCheckBox.setSelected(!StringUtil.isEmpty(configuration.getVersion()));
    promoteCheckbox.setSelected(configuration.isPromote());
    stopPreviousVersionCheckbox.setSelected(configuration.isStopPreviousVersion());
    versionIdField.setEditable(versionOverrideCheckBox.isSelected());
    if (versionOverrideCheckBox.isSelected()) {
      versionIdField.setText(configuration.getVersion());
    }
  }

  @Override
  protected void applyEditorTo(AppEngineDeploymentConfiguration configuration)
      throws ConfigurationException {
    validateConfiguration();

    configuration.setCloudProjectName(projectSelector.getText());
    CredentialedUser selectedUser = projectSelector.getSelectedUser();
    if (selectedUser != null) {
      configuration.setGoogleUsername(selectedUser.getEmail());
    }
    configuration.setEnvironment(environment.name());
    configuration.setUserSpecifiedArtifact(isUserSpecifiedPathDeploymentSource());
    configuration.setUserSpecifiedArtifactPath(userSpecifiedArtifactFileSelector.getText());
    configuration.setDockerFilePath(dockerFilePathField.getText());
    configuration.setAppYamlPath(appYamlPathField.getText());
    configuration.setConfigType(getConfigType());
    configuration.setVersion(
        versionOverrideCheckBox.isSelected() ? versionIdField.getText() : null);
    configuration.setPromote(promoteCheckbox.isSelected());
    configuration.setStopPreviousVersion(stopPreviousVersionCheckbox.isSelected());

    setDeploymentProjectAndVersion();
    updateJarWarSelector();
  }

  @VisibleForTesting
  JComboBox getConfigTypeComboBox() {
    return configTypeComboBox;
  }

  @VisibleForTesting
  JLabel getEnvironmentLabel() {
    return environmentLabel;
  }

  @VisibleForTesting
  JPanel getAppEngineFlexConfigPanel() {
    return appEngineFlexConfigPanel;
  }

  @VisibleForTesting
  void setProjectSelector(ProjectSelector projectSelector) {
    this.projectSelector = projectSelector;
  }

  @VisibleForTesting
  JCheckBox getPromoteCheckbox() {
    return promoteCheckbox;
  }

  @VisibleForTesting
  JCheckBox getStopPreviousVersionCheckbox() {
    return stopPreviousVersionCheckbox;
  }

  /**
   * If a project's appengine-web.xml contains <env>flex</env> then we want to override
   * the default localized label of the environment
   */
  private String getEnvironmentDisplayableLabel() {
    if (AppEngineProjectService.getInstance().isFlexCompatEnvFlex(project, deploymentSource)) {
      return GctBundle.message("appengine.environment.name.mvm");
    }

    return environment.localizedLabel();
  }

  private void updateJarWarSelector() {
    userSpecifiedArtifactPanel.setVisible(isUserSpecifiedPathDeploymentSource());
  }

  private void resetOverridableFields(JCheckBox overrideCheckbox, JTextField field) {
    field.setEditable(overrideCheckbox.isSelected());
  }

  /**
   * Sets the project / version to allow the deployment line items to be decorated with additional
   * identifying data. See {@link AppEngineRuntimeInstance#getDeploymentName}.
   */
  private void setDeploymentProjectAndVersion() {
    AppEngineDeployable deployable = (AppEngineDeployable) deploymentSource;

    deployable.setProjectName(projectSelector.getText());
    deployable.setVersion(getDisplayableVersion());
  }

  private String getDisplayableVersion() {
    return versionOverrideCheckBox.isSelected()
        ? versionIdField.getText() : "auto";

  }

  private void validateConfiguration() throws ConfigurationException {
    if (isUserSpecifiedPathDeploymentSource()
        && (StringUtil.isEmpty(userSpecifiedArtifactFileSelector.getText())
            || !isJarOrWar(userSpecifiedArtifactFileSelector.getText()))) {
      throw new ConfigurationException(
          GctBundle.message("appengine.flex.config.user.specified.artifact.error"));
    } else if (!isUserSpecifiedPathDeploymentSource() && !deploymentSource.isValid()) {
      throw new ConfigurationException(
          GctBundle.message("appengine.config.deployment.source.error"));
    } else if (StringUtils.isBlank(projectSelector.getText())) {
      throw new ConfigurationException(
          GctBundle.message("appengine.flex.config.project.missing.message"));
    } else if (versionOverrideCheckBox.isSelected()
        && StringUtils.isBlank(versionIdField.getText())) {
      throw new ConfigurationException(GctBundle.message("appengine.config.version.error"));
    } else if (getConfigType() == ConfigType.CUSTOM) {
      if (StringUtils.isBlank(appYamlPathField.getText())) {
        throw new ConfigurationException(
            GctBundle.message("appengine.flex.config.custom.app.yaml.error"));
      } else if (StringUtils.isBlank(dockerFilePathField.getText())) {
        throw new ConfigurationException(
            GctBundle.message("appengine.flex.config.custom.dockerfile.error"));
      }
    } else {
      Set<CloudSdkValidationResult> validationResults =
          CloudSdkService.getInstance().validateCloudSdk();
      if (validationResults.contains(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND)) {
        throw new ConfigurationException(GctBundle.message(
            "appengine.cloudsdk.deploymentconfiguration.location.invalid.message"));
      }
      if (environment.isStandard()
          && validationResults.contains(CloudSdkValidationResult.NO_APP_ENGINE_COMPONENT)) {
        throw new ConfigurationException(
            CloudSdkValidationResult.NO_APP_ENGINE_COMPONENT.getMessage());
      }
    }
  }

  private boolean isJarOrWar(String path) {
    File file = new File(path);
    if (file.isDirectory()) {
      return false;
    }
    String name = file.getName();
    return StringUtil.endsWithIgnoreCase(name, ".jar")
        || StringUtil.endsWithIgnoreCase(name, ".war");
  }

  private boolean isUserSpecifiedPathDeploymentSource() {
    return deploymentSource instanceof UserSpecifiedPathDeploymentSource;
  }

  private DocumentAdapter getUserSpecifiedArtifactFileListener() {
    return new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent event) {
        if (isUserSpecifiedPathDeploymentSource()) {
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
    private final Supplier<Path> sourceFileProvider;
    private final JPanel fileSelector;

    public GenerateConfigActionListener(
        Project project,
        String fileName,
        ConfigFileType configFileType,
        Supplier<Path> sourceFileProvider,
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
        Path destinationFolderPath = destinationFolderDialog.getDestinationFolder();
        Path destinationFilePath = destinationFolderPath.resolve(fileName);

        if (Files.exists(destinationFilePath)) {
          if (!new FileConfirmationDialog(
              project, DialogType.CONFIRM_OVERWRITE, destinationFilePath).showAndGet()) {
            return;
          }
        } else if (Files.isRegularFile(destinationFolderPath)) {
          new FileConfirmationDialog(
              project, DialogType.NOT_DIRECTORY_ERROR, destinationFolderPath).show();
          return;
        } else if (!Files.exists(destinationFolderPath)) {
          if (!new FileConfirmationDialog(
              project, DialogType.CONFIRM_CREATE_DIR, destinationFolderPath).showAndGet()) {
            return;
          }
        }

        try {
          FileUtil.copy(sourceFileProvider.get().toFile(), destinationFilePath.toFile());
          LocalFileSystem.getInstance().refreshAndFindFileByIoFile(destinationFilePath.toFile());
        } catch (IOException ex) {
          String message = GctBundle.message(
              "appengine.flex.config.generation.io.error", destinationFilePath.getFileName());
          Messages.showErrorDialog(project, message + ex.getLocalizedMessage(), "Error");
          return;
        }
        filePicker.setText(destinationFilePath.toString());
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

  private class PromoteListener implements ItemListener {
    @Override
    public void itemStateChanged(ItemEvent event) {
      boolean isPromoteSelected = ((JCheckBox) event.getItem()).isSelected();

      stopPreviousVersionLabel.setEnabled(isPromoteSelected);
      stopPreviousVersionCheckbox.setEnabled(isPromoteSelected);

      if (!isPromoteSelected) {
        stopPreviousVersionCheckbox.setSelected(false);
      }
    }
  }

  private class CreateApplicationLinkListener implements HyperlinkListener {

    private Credential credential;
    private String projectId;

    public void setCredential(Credential credential) {
      this.credential = credential;
    }

    public void setProjectId(String projectId) {
      this.projectId = projectId;
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
      if (e.getEventType() == EventType.ACTIVATED) {
        // construct and show the application creation dialog
        AppEngineApplicationCreateDialog applicationDialog
            = new AppEngineApplicationCreateDialog(
            AppEngineDeploymentRunConfigurationEditor.this.getComponent(), projectId, credential);
        DialogManager.show(applicationDialog);

        // if an application was created, update the region field display
        if (applicationDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
          updateRegionField(projectId, credential);
        }
      }
    }
  }
}
