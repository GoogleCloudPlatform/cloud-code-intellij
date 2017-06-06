/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import com.google.cloud.tools.intellij.resources.ProjectSelector;
import com.google.cloud.tools.intellij.ui.BrowserOpeningHyperLinkListener;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.tree.TreeModelAdapter;

import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.TreeModelEvent;

/**
 * Common App Engine deployment configuration UI shared by flexible and standard deployments.
 */
public class AppEngineDeploymentConfigurationPanel {

  private JPanel commonConfigPanel;
  private ProjectSelector projectSelector;
  private JLabel environmentLabel;
  private AppEngineApplicationInfoPanel applicationInfoPanel;

  private JBTextField versionIdField;
  private JCheckBox promoteCheckbox;
  private JCheckBox stopPreviousVersionCheckbox;
  private JCheckBox deployAllConfigsCheckbox;
  private HyperlinkLabel promoteInfoLabel;
  private HyperlinkLabel appEngineCostWarningLabel;
  private JLabel serviceLabel;

  private static final boolean PROMOTE_DEFAULT = false;
  private static final boolean STOP_PREVIOUS_VERSION_DEFAULT = false;

  public AppEngineDeploymentConfigurationPanel() {
    versionIdField
        .getEmptyText()
        .setText(GctBundle.message("appengine.flex.version.placeholder.text"));

    promoteCheckbox.setSelected(PROMOTE_DEFAULT);
    stopPreviousVersionCheckbox.setSelected(STOP_PREVIOUS_VERSION_DEFAULT);
    stopPreviousVersionCheckbox.setEnabled(STOP_PREVIOUS_VERSION_DEFAULT);

    promoteInfoLabel.setHyperlinkText(
        GctBundle.getString("appengine.promote.info.label.beforeLink") + " ",
        GctBundle.getString("appengine.promote.info.label.link"),
        "");
    promoteInfoLabel.addHyperlinkListener(new BrowserOpeningHyperLinkListener());
    promoteInfoLabel.setHyperlinkTarget(GctBundle.getString("appengine.promoteinfo.url"));

    promoteCheckbox.addItemListener(
        event -> {
          boolean isPromoteSelected = ((JCheckBox) event.getItem()).isSelected();

          stopPreviousVersionCheckbox.setEnabled(isPromoteSelected);
          stopPreviousVersionCheckbox.setSelected(isPromoteSelected);
        });

    appEngineCostWarningLabel.setHyperlinkText(
        GctBundle.getString("appengine.flex.deployment.cost.warning.beforeLink"),
        GctBundle.getString("appengine.flex.deployment.cost.warning.link"),
        " " + GctBundle.getString("appengine.flex.deployment.cost.warning.afterLink"));
    appEngineCostWarningLabel.addHyperlinkListener(new BrowserOpeningHyperLinkListener());
    appEngineCostWarningLabel.setHyperlinkTarget(CloudSdkAppEngineHelper.APP_ENGINE_BILLING_URL);

    projectSelector.addProjectSelectionListener(applicationInfoPanel::refresh);

    projectSelector.addModelListener(
        new TreeModelAdapter() {
          @Override
          public void treeStructureChanged(TreeModelEvent event) {
            // projects have finished loading
            refreshApplicationInfoPanel();
          }
        });
  }

  /**
   * Shared implementation of
   * {@link com.intellij.openapi.options.SettingsEditor#resetEditorFrom(Object)}. To be invoked
   * by users of this panel in the overriden method.
   */
  public void resetEditorFrom(@NotNull AppEngineDeploymentConfiguration configuration) {
    promoteCheckbox.setSelected(configuration.isPromote());
    versionIdField.setText(configuration.getVersion());
    stopPreviousVersionCheckbox.setSelected(configuration.isStopPreviousVersion());
    projectSelector.setText(configuration.getCloudProjectName());

    refreshApplicationInfoPanel();
  }

  /**
   * Shared implementation of
   * {@link com.intellij.openapi.options.SettingsEditor#applyEditorTo(Object)}. To be invoked
   * by users of this panel in the overriden method.
   */
  public void applyEditorTo(@NotNull AppEngineDeploymentConfiguration configuration) {
    configuration.setVersion(versionIdField.getText());
    configuration.setPromote(promoteCheckbox.isSelected());
    configuration.setCloudProjectName(projectSelector.getText());
    configuration.setStopPreviousVersion(stopPreviousVersionCheckbox.isSelected());
  }

  /**
   * Sets the project / version to allow the deployment line items to be decorated with additional
   * identifying data. See {@link AppEngineRuntimeInstance#getDeploymentName}.
   */
  public void setDeploymentProjectAndVersion(DeploymentSource deploymentSource) {
    if (deploymentSource instanceof AppEngineDeployable) {
      ((AppEngineDeployable) deploymentSource).setProjectName(projectSelector.getText());
      ((AppEngineDeployable) deploymentSource).setVersion(
          Strings.isNullOrEmpty(versionIdField.getText()) ? "auto" : versionIdField.getText());
    }
  }

  private void refreshApplicationInfoPanel() {
    if (projectSelector.getProject() != null && projectSelector.getSelectedUser() != null) {
      applicationInfoPanel.refresh(
          projectSelector.getProject().getProjectId(),
          projectSelector.getSelectedUser().getCredential());
    } else {
      applicationInfoPanel.setMessage(
          GctBundle.getString("appengine.infopanel.noproject"), true /* isError*/);
    }
  }

  public ProjectSelector getProjectSelector() {
    return projectSelector;
  }

  public JLabel getEnvironmentLabel() {
    return environmentLabel;
  }

  public AppEngineApplicationInfoPanel getApplicationInfoPanel() {
    return applicationInfoPanel;
  }

  public JCheckBox getStopPreviousVersionCheckbox() {
    return stopPreviousVersionCheckbox;
  }

  public JCheckBox getDeployAllConfigsCheckbox() {
    return deployAllConfigsCheckbox;
  }

  public HyperlinkLabel getAppEngineCostWarningLabel() {
    return appEngineCostWarningLabel;
  }

  public JLabel getServiceLabel() {
    return serviceLabel;
  }

  public JCheckBox getPromoteCheckbox() {
    return promoteCheckbox;
  }

  @VisibleForTesting
  public void setProjectSelector(ProjectSelector projectSelector) {
    this.projectSelector = projectSelector;
  }

  @VisibleForTesting
  public void setApplicationInfoPanel(AppEngineApplicationInfoPanel applicationInfoPanel) {
    this.applicationInfoPanel = applicationInfoPanel;
  }
}
