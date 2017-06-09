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
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleRuntimePanel;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService.FlexibleRuntime;
import com.google.cloud.tools.intellij.appengine.project.MalformedYamlFileException;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.resources.ProjectSelector;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
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
import com.intellij.ui.HyperlinkLabel;
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

  private Project project;
  private AppEngineDeploymentConfigurationPanel commonConfig;
  private JPanel mainPanel;
  private TextFieldWithBrowseButton archiveSelector;
  private JPanel archiveSelectorPanel;
  private JComboBox<AppEngineFlexibleFacet> appYamlCombobox;
  private JButton editAppYamlButton;
  private JCheckBox hiddenValidationTrigger;
  private HyperlinkLabel dockerfileDirectoryPathLink;
  private JPanel noAppYamlsWarningPanel;
  private AppEngineFlexibleRuntimePanel runtimePanel;
  private JPanel dockerDirectoryPanel;

  public AppEngineFlexibleDeploymentEditor(Project project, AppEngineDeployable deploymentSource) {
    this.deploymentSource = deploymentSource;
    this.project = project;

    commonConfig
        .getEnvironmentLabel()
        .setText(AppEngineEnvironment.APP_ENGINE_FLEX.localizedLabel());

    commonConfig.getDeployAllConfigsCheckbox().setSelected(false);
    commonConfig.getDeployAllConfigsCheckbox().setVisible(false);

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

    reloadAppYamls(project);
    appYamlCombobox.addItemListener(event -> {
      toggleDockerfileSection();
      resetRuntimeDisplay();
    });
    appYamlCombobox.setRenderer(
        new ListCellRendererWrapper<AppEngineFlexibleFacet>() {
          @Override
          public void customize(
              JList list,
              AppEngineFlexibleFacet facet,
              int index,
              boolean selected,
              boolean hasFocus) {
            if (facet != null) {
              setText(tryTruncateConfigPathForDisplay(facet.getConfiguration().getAppYamlPath()));
            }
          }
        });

    editAppYamlButton.addActionListener(event -> openModuleSettings());
    dockerfileDirectoryPathLink.addHyperlinkListener(event -> openModuleSettings());

    updateSelectors();
    toggleDockerfileSection();
  }

  private void openModuleSettings() {
    AppEngineFlexibleFacet flexFacet = ((AppEngineFlexibleFacet) appYamlCombobox.getSelectedItem());

    if (ModulesConfigurator.showFacetSettingsDialog(flexFacet, null /* tabNameToSelect */)) {
      // The user may have updated the configuration, so we need to refresh it here too.
      reloadAppYamls(project);
      appYamlCombobox.setSelectedItem(flexFacet);
      toggleDockerfileSection();

      // When we get out of the dialog window, we want to re-eval the configuration.
      // validateConfiguration() can't be used here because the ConfigurationException
      // isn't caught anywhere, and fireEditorStateChanged() doesn't trigger any listeners
      // called from here. Emulating a user action triggers apply(), so that's what we're
      // doing here.
      hiddenValidationTrigger.doClick();
    }
  }

  private void resetRuntimeDisplay() {
    if (isValidConfigurationFile(getAppYamlPath())) {
      try {
        Optional<FlexibleRuntime> flexibleRuntime =
            appEngineProjectService.getFlexibleRuntimeFromAppYaml(getAppYamlPath());

        runtimePanel.setLabelText(flexibleRuntime.map(FlexibleRuntime::toString).orElse(""));
        runtimePanel.setVisible(true);
      } catch (MalformedYamlFileException ex) {
        runtimePanel.setVisible(false);
      }
    } else {
      runtimePanel.setVisible(false);
    }
  }

  private void reloadAppYamls(Project project) {
    appYamlCombobox.setModel(new DefaultComboBoxModel<>(
        Arrays.stream(ModuleManager.getInstance(project).getModules())
            .filter(module ->
                AppEngineFlexibleFacet.getFacetByModule(module) != null)
            .map(AppEngineFlexibleFacet::getFacetByModule)
            .toArray(AppEngineFlexibleFacet[]::new)
    ));

    // For the case Flex isn't enabled for any modules, the user can still deploy filesystem
    // jars/wars.
    if (appYamlCombobox.getItemCount() == 0) {
      appYamlCombobox.setVisible(false);
      editAppYamlButton.setVisible(false);
      noAppYamlsWarningPanel.setVisible(true);
      dockerDirectoryPanel.setVisible(true);
    }

    // Since we updated the app.yaml selection, we need to update the runtime
    resetRuntimeDisplay();
  }

  @Override
  protected void resetEditorFrom(@NotNull AppEngineDeploymentConfiguration configuration) {
    commonConfig.resetEditorFrom(configuration);

    appYamlCombobox.setSelectedItem(
        AppEngineFlexibleFacet.getFacetByModuleName(configuration.getModuleName(), project));
    // todo confirm that the below is not needed
//    dockerfileDirectoryPathLink.setHyperlinkText(
//        tryTruncateConfigPathForDisplay(
//            configuration.getFlexibleFacet().getConfiguration().getDockerDirectory()));
    archiveSelector.setText(configuration.getUserSpecifiedArtifactPath());

    toggleDockerfileSection();
    updateServiceName();
  }

  @Override
  protected void applyEditorTo(@NotNull AppEngineDeploymentConfiguration configuration)
      throws ConfigurationException {
    validateConfiguration();

    commonConfig.applyEditorTo(configuration);

    configuration.setModuleName(
        ((AppEngineFlexibleFacet) appYamlCombobox.getSelectedItem()).getModule().getName());
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
          GctBundle.getString("appengine.deployment.error.staging.yaml"));
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
              GctBundle.getString("appengine.deployment.error.staging.dockerfile"));
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
        .map(FlexibleRuntime::isCustom)
        .orElse(false);
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
    dockerDirectoryPanel.setVisible(visible);
    if (visible) {
      dockerfileDirectoryPathLink.setHyperlinkText(
          tryTruncateConfigPathForDisplay(getDockerDirectoryPath()));
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
   * Returns the final app.yaml file path from the combobox.
   */
  private String getAppYamlPath() {
    if (appYamlCombobox.getSelectedItem() == null) {
      return "";
    }

    return Optional.ofNullable(
        ((AppEngineFlexibleFacet) appYamlCombobox.getSelectedItem()))
        .map(flexFacet -> flexFacet.getConfiguration().getAppYamlPath())
        .orElse("");
  }

  private String getDockerDirectoryPath() {
    if (appYamlCombobox.getSelectedItem() == null) {
      return "";
    }

    return Optional.ofNullable(
        ((AppEngineFlexibleFacet) appYamlCombobox.getSelectedItem()))
        .map(flexFacet -> flexFacet.getConfiguration().getDockerDirectory())
        .orElse("");
  }

  /**
   * Returns the project relative path of the supplied path for display. If the project path isn't
   * strictly a prefix of the supplied path, return the original path.
   */
  private String tryTruncateConfigPathForDisplay(String path) {
    String projectPath = project.getBasePath();

    if (projectPath != null && path.startsWith(projectPath)) {
      return path.replaceFirst("^" + projectPath, "");
    }

    return path;
  }

  @VisibleForTesting
  JCheckBox getStopPreviousVersionCheckBox() {
    return commonConfig.getStopPreviousVersionCheckbox();
  }

  @VisibleForTesting
  JComboBox getAppYamlCombobox() {
    return appYamlCombobox;
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
  JPanel getDockerDirectoryPanel() {
    return dockerDirectoryPanel;
  }

  @VisibleForTesting
  AppEngineFlexibleRuntimePanel getRuntimePanel() {
    return runtimePanel;
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
