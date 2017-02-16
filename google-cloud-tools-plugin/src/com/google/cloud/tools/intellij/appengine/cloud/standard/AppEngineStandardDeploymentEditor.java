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
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineEnvironment;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineRuntimeInstance;
import com.google.cloud.tools.intellij.appengine.cloud.CloudSdkAppEngineHelper;
import com.google.cloud.tools.intellij.appengine.cloud.flexible.UserSpecifiedPathDeploymentSource;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.resources.ProjectSelector;
import com.google.cloud.tools.intellij.ui.BrowserOpeningHyperLinkListener;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.tree.TreeModelAdapter;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.TreeModelEvent;

/**
 * Editor for an App Engine Deployment runtime configuration.
 */
public class AppEngineStandardDeploymentEditor extends
    SettingsEditor<AppEngineDeploymentConfiguration> {
  private JPanel editorPanel;
  private JBTextField versionIdField;
  private ProjectSelector projectSelector;
  private JCheckBox promoteCheckbox;
  private JCheckBox stopPreviousVersionCheckbox;
  private HyperlinkLabel promoteInfoLabel;
  private AppEngineApplicationInfoPanel applicationInfoPanel;
  private JLabel serviceLabel;
  private JLabel environmentLabel;
  private HyperlinkLabel appEngineCostWarningLabel;

  private Project project;
  private AppEngineDeployable deploymentSource;

  private static final boolean PROMOTE_DEFAULT = true;
  private static final boolean STOP_PREVIOUS_VERSION_DEFAULT = true;

  /**
   * Initializes the UI components.
   */
  public AppEngineStandardDeploymentEditor(Project project,
      final AppEngineDeployable deploymentSource) {
    this.project = project;
    this.deploymentSource = deploymentSource;

    versionIdField.getEmptyText().setText(
        GctBundle.message("appengine.flex.version.placeholder.text"));
    promoteCheckbox.setSelected(PROMOTE_DEFAULT);
    stopPreviousVersionCheckbox.setVisible(
        AppEngineProjectService.getInstance().isFlexCompat(project, deploymentSource)
    );
    stopPreviousVersionCheckbox.setSelected(STOP_PREVIOUS_VERSION_DEFAULT);

    promoteInfoLabel.setHyperlinkText(
        GctBundle.getString("appengine.promote.info.label.beforeLink") + " ",
        GctBundle.getString("appengine.promote.info.label.link"),
        "");
    promoteInfoLabel.addHyperlinkListener(new BrowserOpeningHyperLinkListener());
    promoteInfoLabel.setHyperlinkTarget(GctBundle.getString("appengine.promoteinfo.url"));

    promoteCheckbox.addItemListener(event -> {
      boolean isPromoteSelected = ((JCheckBox) event.getItem()).isSelected();

      stopPreviousVersionCheckbox.setEnabled(isPromoteSelected);

      if (!isPromoteSelected) {
        stopPreviousVersionCheckbox.setSelected(false);
      }
    });

    projectSelector.addProjectSelectionListener(applicationInfoPanel::refresh);

    projectSelector.addModelListener(new TreeModelAdapter() {
      @Override
      public void treeStructureChanged(TreeModelEvent event) {
        // projects have finished loading
        refreshApplicationInfoPanel();
      }
    });

    serviceLabel.setText(AppEngineProjectService.getInstance()
        .getServiceNameFromAppEngineWebXml(project, deploymentSource));

    if (deploymentSource.getEnvironment() != null) {
      environmentLabel.setText(deploymentSource.getEnvironment().localizedLabel());
      if (deploymentSource.getEnvironment().isFlexCompat()) {
        appEngineCostWarningLabel.setHyperlinkText(
            GctBundle.getString("appengine.flex.deployment.cost.warning.beforeLink"),
            GctBundle.getString("appengine.flex.deployment.cost.warning.link"),
            " " + GctBundle.getString("appengine.flex.deployment.cost.warning.afterLink"));
        appEngineCostWarningLabel.addHyperlinkListener(new BrowserOpeningHyperLinkListener());
        appEngineCostWarningLabel.setHyperlinkTarget(
            CloudSdkAppEngineHelper.APP_ENGINE_BILLING_URL);
      }
    } else {
      appEngineCostWarningLabel.setVisible(false);
    }
  }

  private void refreshApplicationInfoPanel() {
    if (projectSelector.getProject() != null && projectSelector.getSelectedUser() != null) {
      applicationInfoPanel.refresh(projectSelector.getProject().getProjectId(),
          projectSelector.getSelectedUser().getCredential());
    }
  }

  @Override
  protected void resetEditorFrom(@NotNull AppEngineDeploymentConfiguration configuration) {
    projectSelector.setText(configuration.getCloudProjectName());
    refreshApplicationInfoPanel();

    promoteCheckbox.setSelected(configuration.isPromote());
    stopPreviousVersionCheckbox.setVisible(
        AppEngineProjectService.getInstance().isFlexCompat(project, deploymentSource)
    );
    stopPreviousVersionCheckbox.setSelected(configuration.isStopPreviousVersion());
    versionIdField.setText(configuration.getVersion());
    if (deploymentSource.getEnvironment() != null) {
      environmentLabel.setText(deploymentSource.getEnvironment().localizedLabel());
    }
  }

  @Override
  protected void applyEditorTo(@NotNull AppEngineDeploymentConfiguration configuration)
      throws ConfigurationException {
    validateConfiguration();

    configuration.setCloudProjectName(projectSelector.getText());
    CredentialedUser selectedUser = projectSelector.getSelectedUser();
    if (selectedUser != null) {
      configuration.setGoogleUsername(selectedUser.getEmail());
    }
    boolean isFlexCompat =
        AppEngineProjectService.getInstance().isFlexCompat(project, deploymentSource);
    configuration.setEnvironment(
        isFlexCompat ? AppEngineEnvironment.APP_ENGINE_FLEX_COMPAT.name()
            : AppEngineEnvironment.APP_ENGINE_STANDARD.name());
    if (isFlexCompat) {
      configuration.setStopPreviousVersion(stopPreviousVersionCheckbox.isSelected());
    }
    configuration.setVersion(versionIdField.getText());
    configuration.setPromote(promoteCheckbox.isSelected());

    setDeploymentProjectAndVersion();
  }

  /**
   * Sets the project / version to allow the deployment line items to be decorated with additional
   * identifying data. See {@link AppEngineRuntimeInstance#getDeploymentName}.
   */
  private void setDeploymentProjectAndVersion() {
    deploymentSource.setProjectName(projectSelector.getText());
    deploymentSource.setVersion(Strings.isNullOrEmpty(versionIdField.getText())
        ? "auto" : versionIdField.getText());
  }

  private void validateConfiguration() throws ConfigurationException {
    if (!(deploymentSource instanceof UserSpecifiedPathDeploymentSource)
        && !deploymentSource.isValid()) {
      throw new ConfigurationException(
          GctBundle.message("appengine.config.deployment.source.error"));
    }
    if (StringUtils.isBlank(projectSelector.getText())) {
      throw new ConfigurationException(
          GctBundle.message("appengine.flex.config.project.missing.message"));
    }
    if (!applicationInfoPanel.isApplicationValid()) {
      throw new ConfigurationException(
          GctBundle.message("appengine.application.required.deployment"));
    }
    Set<CloudSdkValidationResult> validationResults =
        CloudSdkService.getInstance().validateCloudSdk();
    if (validationResults.contains(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND)) {
      throw new ConfigurationException(GctBundle.message(
            "appengine.cloudsdk.deploymentconfiguration.location.invalid.message"));
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
  JCheckBox getPromoteCheckbox() {
    return promoteCheckbox;
  }

  @VisibleForTesting
  JCheckBox getStopPreviousVersionCheckbox() {
    return stopPreviousVersionCheckbox;
  }

  @VisibleForTesting
  void setProjectSelector(ProjectSelector projectSelector) {
    this.projectSelector = projectSelector;
  }

  @VisibleForTesting
  void setApplicationInfoPanel(AppEngineApplicationInfoPanel applicationInfoPanel) {
    this.applicationInfoPanel = applicationInfoPanel;
  }
}
