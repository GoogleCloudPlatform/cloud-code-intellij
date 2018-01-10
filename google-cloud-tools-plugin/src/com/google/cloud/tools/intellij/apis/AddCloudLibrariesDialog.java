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

package com.google.cloud.tools.intellij.apis;

import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.cloud.tools.libraries.CloudLibraries;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import git4idea.DialogManager;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;

/** The dialog for adding GCP client libraries and managing GCP APIs. */
final class AddCloudLibrariesDialog extends DialogWrapper {

  private static final Logger logger = Logger.getInstance(AddCloudLibrariesAction.class);

  private final Project project;
  private final GoogleCloudApiSelectorPanel cloudApiSelectorPanel;

  AddCloudLibrariesDialog(Project project) {
    super(false);

    List<CloudLibrary> libraries;
    try {
      libraries = CloudLibraries.getCloudLibraries();
    } catch (IOException e) {
      logger.error(e);
      libraries = ImmutableList.of();
    }

    this.project = project;
    this.cloudApiSelectorPanel = new GoogleCloudApiSelectorPanel(libraries, project);
    init();
  }

  @Override
  protected void init() {
    super.init();
    setTitle(GctBundle.message("cloud.libraries.dialog.title"));
    setOKButtonText(GctBundle.message("cloud.libraries.ok.button.text"));
  }

  /** Returns the selected {@link Module}. */
  Module getSelectedModule() {
    return cloudApiSelectorPanel.getSelectedModule();
  }

  /** Returns the set of selected {@link CloudLibrary CloudLibraries}. */
  Set<CloudLibrary> getSelectedLibraries() {
    return cloudApiSelectorPanel.getSelectedLibraries();
  }

  /** Returns the selected {@link CloudProject}. */
  CloudProject getCloudProject() {
    return cloudApiSelectorPanel.getCloudProject();
  }

  /** Returns the set of {@link CloudLibrary APIs} to enable. */
  Set<CloudLibrary> getApisToEnable() {
    return cloudApiSelectorPanel.getApisToEnable();
  }

  /**
   * Overrides {@link DialogWrapper#doOKAction()} to first check if there are any APIs to enable on
   * GCP.
   *
   * <p>If so, the {@link CloudApiManagementConfirmationDialog} is opened confirming the API changes
   * to be made. If the user cancels, the user is returned to this parent dialog. Otherwise, it is
   * closed and the default {@link DialogWrapper#doOKAction()} is invoked.
   */
  @Override
  protected void doOKAction() {
    CloudProject cloudProject = getCloudProject();
    Set<CloudLibrary> apisToEnable = getApisToEnable();

    if (cloudProject != null && !apisToEnable.isEmpty()) {
      CloudApiManagementConfirmationDialog managementDialog =
          new CloudApiManagementConfirmationDialog(project, apisToEnable);
      DialogManager.show(managementDialog);

      if (managementDialog.isOK()) {
        ProgressManager.getInstance()
            .runProcessWithProgressSynchronously(
                () -> CloudApiManager.enableApis(apisToEnable, cloudProject, project),
                GctBundle.message("cloud.apis.enable.progress.title"),
                true /*canBeCanceled*/,
                project);

        super.doOKAction();
      }
    } else {
      super.doOKAction();
    }
  }

  @Override
  protected JComponent createCenterPanel() {
    return cloudApiSelectorPanel.getPanel();
  }
}
