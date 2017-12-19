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

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeployable;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfigurationPanel;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineEnvironment;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;

/** Editor for an App Engine Deployment runtime configuration. */
public final class AppEngineStandardDeploymentEditor
    extends SettingsEditor<AppEngineDeploymentConfiguration> {
  private AppEngineDeploymentConfigurationPanel commonConfig;
  private JPanel editorPanel;

  private Project project;
  private AppEngineDeployable deploymentSource;

  private static final boolean DEPLOY_ALL_APPENGINE_CONFIGS_DEFAULT = true;

  /** Initializes the UI components. */
  public AppEngineStandardDeploymentEditor(Project project, AppEngineDeployable deploymentSource) {
    this.project = project;
    this.deploymentSource = deploymentSource;

    commonConfig.getDeployAllConfigsCheckbox().setSelected(DEPLOY_ALL_APPENGINE_CONFIGS_DEFAULT);
    commonConfig
        .getServiceLabel()
        .setText(
            AppEngineProjectService.getInstance()
                .getServiceNameFromAppEngineWebXml(project, deploymentSource));

    if (deploymentSource.getEnvironment() != null) {
      commonConfig
          .getEnvironmentLabel()
          .setText(deploymentSource.getEnvironment().localizedLabel());

      if (!deploymentSource.getEnvironment().isFlexCompat()) {
        commonConfig.getAppEngineCostWarningPanel().setVisible(false);
      }
    }
  }

  @Override
  protected void resetEditorFrom(@NotNull AppEngineDeploymentConfiguration configuration) {
    commonConfig.resetEditorFrom(configuration);
  }

  @Override
  protected void applyEditorTo(@NotNull AppEngineDeploymentConfiguration configuration) {
    commonConfig.applyEditorTo(configuration);
    commonConfig.setDeploymentProjectAndVersion(deploymentSource);

    boolean isFlexCompat =
        AppEngineProjectService.getInstance().isFlexCompat(project, deploymentSource);
    configuration.setEnvironment(
        isFlexCompat
            ? AppEngineEnvironment.APP_ENGINE_FLEX_COMPAT
            : AppEngineEnvironment.APP_ENGINE_STANDARD);
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    return editorPanel;
  }

  @VisibleForTesting
  AppEngineDeploymentConfigurationPanel getCommonConfig() {
    return commonConfig;
  }

  private void createUIComponents() {}
}
