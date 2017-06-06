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

package com.google.cloud.tools.intellij.appengine.cloud.standard;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineApplicationInfoPanel;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeployable;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfigurationPanel;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineEnvironment;
import com.google.cloud.tools.intellij.appengine.cloud.flexible.UserSpecifiedPathDeploymentSource;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.resources.ProjectSelector;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

/** Editor for an App Engine Deployment runtime configuration. */
public class AppEngineStandardDeploymentEditor
    extends SettingsEditor<AppEngineDeploymentConfiguration> {
  private AppEngineDeploymentConfigurationPanel commonConfig;
  private JPanel editorPanel;

  private Project project;
  private AppEngineDeployable deploymentSource;

  private static final boolean DEPLOY_ALL_APPENGINE_CONFIGS_DEFAULT = false;

  /** Initializes the UI components. */
  public AppEngineStandardDeploymentEditor(
      Project project, final AppEngineDeployable deploymentSource) {
    this.project = project;
    this.deploymentSource = deploymentSource;

    commonConfig.getDeployAllConfigsCheckbox().setSelected(DEPLOY_ALL_APPENGINE_CONFIGS_DEFAULT);

    commonConfig
        .getServiceLabel()
        .setText(
            AppEngineProjectService.getInstance()
                .getServiceNameFromAppEngineWebXml(project, deploymentSource));

    if (deploymentSource.getEnvironment() != null) {
      commonConfig.getEnvironmentLabel().setText(deploymentSource.getEnvironment().localizedLabel());

      if (!deploymentSource.getEnvironment().isFlexCompat()) {
        commonConfig.getAppEngineCostWarningLabel().setVisible(false);
      }
    }
  }

  @Override
  protected void resetEditorFrom(@NotNull AppEngineDeploymentConfiguration configuration) {
    commonConfig.resetEditorFrom(configuration);

    commonConfig.getDeployAllConfigsCheckbox().setSelected(configuration.isDeployAllConfigs());
    if (deploymentSource.getEnvironment() != null) {
      commonConfig.getEnvironmentLabel().setText(deploymentSource.getEnvironment().localizedLabel());
    }
  }

  @Override
  protected void applyEditorTo(@NotNull AppEngineDeploymentConfiguration configuration)
      throws ConfigurationException {
    validateConfiguration();

    commonConfig.applyEditorTo(configuration);

    CredentialedUser selectedUser = commonConfig.getProjectSelector().getSelectedUser();
    if (selectedUser != null) {
      configuration.setGoogleUsername(selectedUser.getEmail());
    }
    boolean isFlexCompat =
        AppEngineProjectService.getInstance().isFlexCompat(project, deploymentSource);
    configuration.setEnvironment(
        isFlexCompat
            ? AppEngineEnvironment.APP_ENGINE_FLEX_COMPAT.name()
            : AppEngineEnvironment.APP_ENGINE_STANDARD.name());

    configuration.setDeployAllConfigs(commonConfig.getDeployAllConfigsCheckbox().isSelected());

    commonConfig.setDeploymentProjectAndVersion(deploymentSource);
  }

  private void validateConfiguration() throws ConfigurationException {
    if (!(deploymentSource instanceof UserSpecifiedPathDeploymentSource)
        && !deploymentSource.isValid()) {
      throw new ConfigurationException(
          GctBundle.message("appengine.config.deployment.source.error"));
    }
    if (StringUtils.isBlank(commonConfig.getProjectSelector().getText())) {
      throw new ConfigurationException(
          GctBundle.message("appengine.flex.config.project.missing.message"));
    }
    if (!commonConfig.getApplicationInfoPanel().isApplicationValid()) {
      throw new ConfigurationException(
          GctBundle.message("appengine.application.required.deployment"));
    }
    Set<CloudSdkValidationResult> validationResults =
        CloudSdkService.getInstance().validateCloudSdk();
    if (validationResults.contains(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND)) {
      throw new ConfigurationException(
          GctBundle.message("appengine.cloudsdk.deploymentconfiguration.location.invalid.message"));
    }
    if (validationResults.contains(CloudSdkValidationResult.NO_APP_ENGINE_COMPONENT)) {
      throw new ConfigurationException(
          CloudSdkValidationResult.NO_APP_ENGINE_COMPONENT.getMessage());
    }
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    return editorPanel;
  }

  @VisibleForTesting
  JCheckBox getStopPreviousVersionCheckbox() {
    return commonConfig.getStopPreviousVersionCheckbox();
  }

  @VisibleForTesting
  JCheckBox getDeployAllConfigsCheckbox() {
    return commonConfig.getDeployAllConfigsCheckbox();
  }

  @VisibleForTesting
  AppEngineDeploymentConfigurationPanel getCommonConfig() {
    return commonConfig;
  }

  @VisibleForTesting
  void setProjectSelector(ProjectSelector projectSelector) {
    commonConfig.setProjectSelector(projectSelector);
  }

  @VisibleForTesting
  void setApplicationInfoPanel(AppEngineApplicationInfoPanel applicationInfoPanel) {
    commonConfig.setApplicationInfoPanel(applicationInfoPanel);
  }
}
