/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.cloud.flexible;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineApplicationInfoPanel;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeployable;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfigurationPanel;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineEnvironment;
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacet;
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacetType;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService.FlexibleRuntime;
import com.google.cloud.tools.intellij.appengine.project.MalformedYamlFileException;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.resources.ProjectSelector;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;

import com.intellij.facet.FacetManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.ListCellRendererWrapper;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;

/**
 * Flexible deployment run configuration user interface.
 */
public final class AppEngineFlexibleDeploymentEditor extends
    SettingsEditor<AppEngineDeploymentConfiguration> {
  private static final String DEFAULT_SERVICE = "default";
  private static final String DOCKERFILE_NAME = "Dockerfile";
  private DeploymentSource deploymentSource;
  private final AppEngineProjectService appEngineProjectService =
      AppEngineProjectService.getInstance();

  private AppEngineDeploymentConfigurationPanel commonConfig;
  private JPanel mainPanel;
  private TextFieldWithBrowseButton appYamlTextField;
  private TextFieldWithBrowseButton dockerDirectoryTextField;
  private TextFieldWithBrowseButton archiveSelector;
  private JPanel archiveSelectorPanel;
  private JLabel dockerDirectoryLabel;
  private JComboBox<Module> modulesWithFlexFacetComboBox;
  private JCheckBox appYamlOverrideCheckBox;
  private JCheckBox dockerDirectoryOverrideCheckBox;
  private String dockerfileOverride = "";
  private JButton moduleSettingsButton;
  private JCheckBox hiddenValidationTrigger;
  private JLabel noSupportedModulesWarning;

  public AppEngineFlexibleDeploymentEditor(Project project, AppEngineDeployable deploymentSource) {
    this.deploymentSource = deploymentSource;

    commonConfig
        .getEnvironmentLabel()
        .setText(AppEngineEnvironment.APP_ENGINE_FLEX.localizedLabel());

    commonConfig.getDeployAllConfigsCheckbox().setSelected(false);
    commonConfig.getDeployAllConfigsCheckbox().setVisible(false);

    appYamlTextField.addBrowseFolderListener(
        GctBundle.message("appengine.flex.config.browse.app.yaml"),
        null /* description */,
        project,
        FileChooserDescriptorFactory.createSingleFileDescriptor().withFileFilter(
            virtualFile -> Comparing.equal(virtualFile.getExtension(), "yaml")
                || Comparing.equal(virtualFile.getExtension(), "yml"))
    );

    dockerDirectoryTextField.addBrowseFolderListener(
        GctBundle.message("appengine.flex.config.browse.docker.directory"),
        null /* description */,
        project,
        FileChooserDescriptorFactory.createSingleFolderDescriptor()
    );

    archiveSelector.addBrowseFolderListener(
        GctBundle.message("appengine.flex.config.user.specified.artifact.title"),
        null /* description */,
        project,
        FileChooserDescriptorFactory.createSingleFileDescriptor().withFileFilter(
            virtualFile ->
                Comparing.equal(
                    virtualFile.getExtension(), "jar", SystemInfo.isFileSystemCaseSensitive)
                    || Comparing.equal(
                    virtualFile.getExtension(), "war", SystemInfo.isFileSystemCaseSensitive)
        )
    );

    archiveSelector.getTextField().getDocument().addDocumentListener(
        new DocumentAdapter() {
          @Override
          protected void textChanged(DocumentEvent event) {
            if (deploymentSource instanceof UserSpecifiedPathDeploymentSource) {
              ((UserSpecifiedPathDeploymentSource) deploymentSource).setFilePath(
                  archiveSelector.getText());
            }
          }
        }
    );

    appYamlOverrideCheckBox.addActionListener(event -> {
      boolean isAppYamlOverrideSelected = ((JCheckBox) event.getSource()).isSelected();
      appYamlTextField.setVisible(isAppYamlOverrideSelected);

      setModuleControlsEnabled(
          !(isAppYamlOverrideSelected && dockerDirectoryOverrideCheckBox.isSelected()));
      updateServiceName();
      toggleDockerfileSection();
    });

    dockerDirectoryOverrideCheckBox.addActionListener(
        event -> {
          boolean isDockerfileOverrideSelected = ((JCheckBox) event.getSource()).isSelected();
          dockerDirectoryTextField.setEnabled(isDockerfileOverrideSelected);
          if (isDockerfileOverrideSelected) {
            if (dockerfileOverride.isEmpty()) {
              dockerfileOverride = dockerDirectoryTextField.getText();
            }
            dockerDirectoryTextField.setText(dockerfileOverride);
          } else {
            dockerfileOverride = dockerDirectoryTextField.getText();
            dockerDirectoryTextField.setText(getDockerDirectoryPath());
          }

          setModuleControlsEnabled(
              !(isDockerfileOverrideSelected && appYamlOverrideCheckBox.isSelected()));
        }
    );

    appYamlTextField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent event) {
        updateServiceName();
        toggleDockerfileSection();
      }
    });

    resetModuleConfigSelection(project);
    modulesWithFlexFacetComboBox.addItemListener(event -> toggleDockerfileSection());
    modulesWithFlexFacetComboBox.setRenderer(new ListCellRendererWrapper<Module>() {
      @Override
      public void customize(JList list, Module value, int index, boolean selected,
          boolean hasFocus) {
        if (value != null) {
          setText(value.getName());
        }
      }
    });

    appYamlTextField.setText(getAppYamlPath());

    moduleSettingsButton.addActionListener(event -> {
      AppEngineFlexibleFacet flexFacet =
          FacetManager.getInstance(((Module) modulesWithFlexFacetComboBox.getSelectedItem()))
              .getFacetByType(AppEngineFlexibleFacetType.ID);
      ModulesConfigurator.showFacetSettingsDialog(flexFacet, null /* tabNameToSelect */);
      // When we get out of the dialog window, we want to re-eval the configuration.
      // validateConfiguration() can't be used here because the ConfigurationException isn't caught
      // anywhere, and fireEditorStateChanged() doesn't trigger any listeners called from here.
      // Emulating a user action triggers apply(), so that's what we're doing here.
      hiddenValidationTrigger.doClick();
      toggleDockerfileSection();
      resetModuleConfigSelection(project);
    });

    updateSelectors();
    toggleDockerfileSection();
  }

  private void resetModuleConfigSelection(Project project) {
    modulesWithFlexFacetComboBox.setModel(new DefaultComboBoxModel<>(
        Arrays.stream(ModuleManager.getInstance(project).getModules())
            .filter(module ->
                FacetManager.getInstance(module)
                    .getFacetByType(AppEngineFlexibleFacetType.ID) != null)
            .toArray(Module[]::new)
    ));

    // For the case Flex isn't enabled for any modules, the user can still deploy filesystem
    // jars/wars.
    if (modulesWithFlexFacetComboBox.getItemCount() == 0) {
      modulesWithFlexFacetComboBox.setVisible(false);
      moduleSettingsButton.setVisible(false);
      appYamlOverrideCheckBox.setVisible(false);
      dockerDirectoryOverrideCheckBox.setVisible(false);
      noSupportedModulesWarning.setVisible(true);
      appYamlTextField.setVisible(true);
      dockerDirectoryTextField.setVisible(true);
      // These checks are important so getAppYamlPath() and getDockerDirectory() work correctly.
      appYamlOverrideCheckBox.setSelected(true);
      dockerDirectoryOverrideCheckBox.setSelected(true);
    }
  }

  @Override
  protected void resetEditorFrom(@NotNull AppEngineDeploymentConfiguration configuration) {
    commonConfig.resetEditorFrom(configuration);

    appYamlTextField.setText(configuration.getAppYamlPath());
    dockerDirectoryTextField.setText(configuration.getDockerDirectoryPath());
    appYamlTextField.setVisible(configuration.isOverrideAppYaml()
        || modulesWithFlexFacetComboBox.getItemCount() == 0);
    archiveSelector.setText(configuration.getUserSpecifiedArtifactPath());
    appYamlOverrideCheckBox.setSelected(configuration.isOverrideAppYaml()
        || modulesWithFlexFacetComboBox.getItemCount() == 0);
    dockerDirectoryOverrideCheckBox.setSelected(configuration.isOverrideDockerDirectory()
        || modulesWithFlexFacetComboBox.getItemCount() == 0);

    setModuleControlsEnabled(
        !(configuration.isOverrideAppYaml() && configuration.isOverrideDockerDirectory()));

    toggleDockerfileSection();
    updateServiceName();
  }

  @Override
  protected void applyEditorTo(@NotNull AppEngineDeploymentConfiguration configuration)
      throws ConfigurationException {
    validateConfiguration();

    commonConfig.applyEditorTo(configuration);

    configuration.setAppYamlPath(getAppYamlPath());
    configuration.setDockerDirectoryPath(getDockerDirectoryPath());
    CredentialedUser user = commonConfig.getProjectSelector().getSelectedUser();
    if (user != null) {
      configuration.setGoogleUsername(user.getEmail());
    }
    String environment = "";
    if (deploymentSource instanceof AppEngineDeployable) {
      environment = ((AppEngineDeployable) deploymentSource).getEnvironment().name();
    }
    configuration.setEnvironment(environment);
    configuration.setUserSpecifiedArtifact(
        deploymentSource instanceof UserSpecifiedPathDeploymentSource);
    configuration.setUserSpecifiedArtifactPath(archiveSelector.getText());
    configuration.setOverrideAppYaml(appYamlOverrideCheckBox.isSelected());
    configuration.setOverrideDockerDirectory(dockerDirectoryOverrideCheckBox.isSelected());
    updateSelectors();

    commonConfig.setDeploymentProjectAndVersion(deploymentSource);
  }

  private void validateConfiguration() throws ConfigurationException {
    if (deploymentSource instanceof UserSpecifiedPathDeploymentSource
        && (StringUtil.isEmpty(archiveSelector.getText())
        || !isJarOrWar(archiveSelector.getText()))) {
      throw new ConfigurationException(
          GctBundle.message("appengine.flex.config.user.specified.artifact.error"));
    }
    if (!(deploymentSource instanceof UserSpecifiedPathDeploymentSource)
        && !deploymentSource.isValid()) {
      throw new ConfigurationException(
          GctBundle.message("appengine.config.deployment.source.error"));
    }
    if (StringUtils.isBlank(commonConfig.getProjectSelector().getText())) {
      throw new ConfigurationException(
          GctBundle.message("appengine.flex.config.project.missing.message"));
    }
    Set<CloudSdkValidationResult> validationResults =
        CloudSdkService.getInstance().validateCloudSdk();
    if (validationResults.contains(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND)) {
      throw new ConfigurationException(GctBundle.message(
          "appengine.cloudsdk.deploymentconfiguration.location.invalid.message"));
    }
    if (StringUtils.isBlank(getAppYamlPath())) {
      throw new ConfigurationException(
          GctBundle.message("appengine.flex.config.browse.app.yaml"));
    }
    if (!isValidConfigurationFile(getAppYamlPath())) {
      throw new ConfigurationException(
          GctBundle.getString("appengine.deployment.error.staging.yaml") + " "
              + GctBundle.getString("appengine.deployment.error.staging.file.gotosettings"));
    }
    try {
      if (isCustomRuntime()) {
        String dockerDirectoryPath = getDockerDirectoryPath();
        if (StringUtils.isBlank(dockerDirectoryPath)) {
          throw new ConfigurationException(
              GctBundle.message("appengine.flex.config.browse.docker.directory"));
        }
        if (!isValidConfigurationFile(Paths.get(dockerDirectoryPath, DOCKERFILE_NAME).toString())) {
          throw new ConfigurationException(
              GctBundle.getString("appengine.deployment.error.staging.dockerfile") + " "
                  + GctBundle.getString("appengine.deployment.error.staging.directory.gotosettings"));
        }
      }
    } catch (MalformedYamlFileException myf) {
      throw new ConfigurationException(
          GctBundle.message("appengine.appyaml.malformed"));
    }
    if (!commonConfig.getApplicationInfoPanel().isApplicationValid()) {
      throw new ConfigurationException(
          GctBundle.message("appengine.application.required.deployment"));
    }
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    return mainPanel;
  }

  private void updateServiceName() {
    try {
      Optional<String> service =
          appEngineProjectService.getServiceNameFromAppYaml(getAppYamlPath());
      commonConfig.getServiceLabel().setText(service.orElse(DEFAULT_SERVICE));
    } catch (MalformedYamlFileException myf) {
      commonConfig.getServiceLabel().setText("");
    }
  }

  private void updateSelectors() {
    archiveSelectorPanel.setVisible(deploymentSource instanceof UserSpecifiedPathDeploymentSource);
  }

  private boolean isJarOrWar(String stringPath) {
    try {
      Path path = Paths.get(stringPath);
      return !Files.isDirectory(path) && (StringUtil.endsWithIgnoreCase(stringPath, ".jar")
          || StringUtil.endsWithIgnoreCase(stringPath, ".war"));
    } catch (InvalidPathException ipe) {
      return false;
    }
  }

  private boolean isCustomRuntime() throws MalformedYamlFileException {
    return appEngineProjectService.getFlexibleRuntimeFromAppYaml(getAppYamlPath())
        .filter(runtime -> runtime == FlexibleRuntime.CUSTOM)
        .isPresent();
  }

  /**
   * Enables the Dockerfile section of the UI if the app.yaml file contains "runtime: custom".
   * Disables it otherwise.
   */
  private void toggleDockerfileSection() {
    boolean visible = false;
    try {
      visible = isCustomRuntime();
    } catch (MalformedYamlFileException myf) {
      // Do nothing, don't blow up, let visible stay false.
    }
    dockerDirectoryOverrideCheckBox.setVisible(
        visible && modulesWithFlexFacetComboBox.getItemCount() != 0);
    dockerDirectoryTextField.setVisible(visible);
    dockerDirectoryTextField.setEnabled(dockerDirectoryOverrideCheckBox.isSelected());
    dockerDirectoryLabel.setVisible(visible);
    if (visible) {
      dockerDirectoryTextField.setText(getDockerDirectoryPath());
    }
  }

  /**
   * Checks if a configuration file is valid by checking if it exists and is a regular file.
   */
  private boolean isValidConfigurationFile(String path) {
    try {
      if (!Files.exists(Paths.get(path)) || !Files.isRegularFile(Paths.get(path))) {
        return false;
      }
    } catch (SecurityException ex) {
      return false;
    }

    return true;
  }

  /**
   * Returns the final app.yaml file path from the combobox or text field, depending on if it's
   * overridden.
   */
  private String getAppYamlPath() {
    if (appYamlOverrideCheckBox.isSelected()) {
      return appYamlTextField.getText();
    }

    if (modulesWithFlexFacetComboBox.getSelectedItem() == null) {
      return "";
    }

    return Optional.ofNullable(
        FacetManager.getInstance(((Module) modulesWithFlexFacetComboBox.getSelectedItem()))
        .getFacetByType(AppEngineFlexibleFacetType.ID))
        .map(flexFacet -> flexFacet.getConfiguration().getAppYamlPath())
        .orElse("");
  }

  private String getDockerDirectoryPath() {
    if (dockerDirectoryOverrideCheckBox.isSelected()) {
      return dockerDirectoryTextField.getText();
    }

    if (modulesWithFlexFacetComboBox.getSelectedItem() == null) {
      return "";
    }

    return Optional.ofNullable(
        FacetManager.getInstance(((Module) modulesWithFlexFacetComboBox.getSelectedItem()))
            .getFacetByType(AppEngineFlexibleFacetType.ID))
        .map(flexFacet -> flexFacet.getConfiguration().getDockerDirectory())
        .orElse("");
  }

  /**
   * Enables or disables the modules combo box and module settings button. Ideally, they get
   * disabled if both app.yaml and Dockerfile override check boxes are checked and enabled
   * otherwise.
   */
  private void setModuleControlsEnabled(boolean enabled) {
    modulesWithFlexFacetComboBox.setEnabled(enabled);
    moduleSettingsButton.setEnabled(enabled);
  }

  @VisibleForTesting
  JCheckBox getStopPreviousVersionCheckBox() {
    return commonConfig.getStopPreviousVersionCheckbox();
  }

  @VisibleForTesting
  TextFieldWithBrowseButton getAppYamlTextField() {
    return appYamlTextField;
  }

  @VisibleForTesting
  TextFieldWithBrowseButton getDockerDirectoryTextField() {
    return dockerDirectoryTextField;
  }

  @VisibleForTesting
  JLabel getDockerDirectoryLabel() {
    return dockerDirectoryLabel;
  }

  @VisibleForTesting
  JCheckBox getDockerDirectoryOverrideCheckBox() {
    return dockerDirectoryOverrideCheckBox;
  }

  @VisibleForTesting
  JCheckBox getAppYamlOverrideCheckBox() {
    return appYamlOverrideCheckBox;
  }

  @VisibleForTesting
  JLabel getServiceLabel() {
    return commonConfig.getServiceLabel();
  }

  @VisibleForTesting
  JComboBox getModulesWithFlexFacetComboBox() {
    return modulesWithFlexFacetComboBox;
  }

  @VisibleForTesting
  TextFieldWithBrowseButton getArchiveSelector() {
    return archiveSelector;
  }

  @VisibleForTesting
  JCheckBox getDeployAllConfigsCheckbox() {
    return commonConfig.getDeployAllConfigsCheckbox();
  }

  @VisibleForTesting
  ProjectSelector getProjectSelector() {
    return commonConfig.getProjectSelector();
  }

  @VisibleForTesting
  AppEngineDeploymentConfigurationPanel getCommonConfig() {
    return commonConfig;
  }

  @VisibleForTesting
  void setAppInfoPanel(AppEngineApplicationInfoPanel appInfoPanel) {
    commonConfig.setApplicationInfoPanel(appInfoPanel);
  }

  @VisibleForTesting
  void setDeploymentSource(DeploymentSource deploymentSource) {
    this.deploymentSource = deploymentSource;
  }
}
