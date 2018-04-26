/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.cloudapis;

import com.google.api.services.iam.v1.model.Role;
import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.common.collect.Sets;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import git4idea.DialogManager;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JComponent;

/** The dialog for adding GCP client libraries and managing GCP APIs. */
final class AddCloudLibrariesDialog extends DialogWrapper {

  private static final Logger logger = Logger.getInstance(AddCloudLibrariesAction.class);

  private final Project project;
  private final GoogleCloudApiSelectorPanel cloudApiSelectorPanel;

  AddCloudLibrariesDialog(Project project) {
    super(project);

    List<CloudLibrary> libraries = CloudLibrariesService.getInstance().getCloudLibraries();

    this.project = project;

    cloudApiSelectorPanel = new GoogleCloudApiSelectorPanel(libraries, project);
    cloudApiSelectorPanel.addModuleSelectionListener(
        listener -> setOKActionEnabled(isReadyToSubmit()));
    cloudApiSelectorPanel.addTableModelListener(
        cloudProject -> setOKActionEnabled(isReadyToSubmit()));

    init();
  }

  @Override
  protected void init() {
    super.init();
    setTitle(GoogleCloudApisMessageBundle.message("cloud.libraries.dialog.title"));
    setOKButtonText(GoogleCloudApisMessageBundle.message("cloud.libraries.ok.button.text"));
    setOKActionEnabled(isReadyToSubmit());
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

  /** Returns, optionally, the selected BOM version. */
  Optional<String> getSelectedBomVersion() {
    return cloudApiSelectorPanel.getSelectedBomVersion();
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
    Set<CloudLibrary> selectedApis = getSelectedLibraries();
    Set<CloudLibrary> apisToEnable = getApisToEnable();
    Set<CloudLibrary> apisNotEnabled = Sets.difference(selectedApis, apisToEnable);

    if (cloudProject != null) {
      Set<Role> roles = getServiceAccountRoles(selectedApis);

      CloudApiManagementConfirmationDialog managementDialog =
          new CloudApiManagementConfirmationDialog(
              getSelectedModule(), cloudProject, apisToEnable, apisNotEnabled, roles);
      DialogManager.show(managementDialog);

      if (managementDialog.isOK()) {
        if (!apisToEnable.isEmpty()) {
          runApiEnablement(apisToEnable);
        }

        if (managementDialog.isCreateNewServiceAccount()) {
          runServiceAccountManagement(
              managementDialog.getSelectedRoles(),
              managementDialog.getServiceAccountName(),
              managementDialog.getServiceAccountKeyDownloadPath());
        }

        super.doOKAction();
      }
    } else {
      super.doOKAction();
    }
  }

  /**
   * Runs the process that enables the set of {@link CloudLibrary apis} on GCP.
   *
   * @param apisToEnable the APIs selected by the user for enablement
   */
  private void runApiEnablement(Set<CloudLibrary> apisToEnable) {
    ProgressManager.getInstance()
        .runProcessWithProgressSynchronously(
            () -> CloudApiManager.enableApis(apisToEnable, getCloudProject(), project),
            GoogleCloudApisMessageBundle.message("cloud.apis.enable.progress.title"),
            true /*canBeCanceled*/,
            project);
  }

  /**
   * Runs the process that creates the service account and generates the service account key file.
   *
   * @param selectedRoles the {@link Role roles} selected by the user
   * @param serviceAccountName the name of the service account
   * @param serviceAccountKeyDownloadPath the {@link Path} to the chosen download folder
   */
  private void runServiceAccountManagement(
      Set<Role> selectedRoles, String serviceAccountName, Path serviceAccountKeyDownloadPath) {
    ProgressManager.getInstance()
        .runProcessWithProgressSynchronously(
            () ->
                CloudApiManager.createServiceAccountAndDownloadKey(
                    selectedRoles,
                    serviceAccountName,
                    serviceAccountKeyDownloadPath,
                    getCloudProject(),
                    project),
            GoogleCloudApisMessageBundle.message(
                "cloud.apis.service.account.create.progress.title"),
            true /*canBeCanceled*/,
            project);
  }

  /**
   * Fetches all the available roles for the {@link CloudProject} and returns the set of roles
   * corresponding to the selected client libraries.
   *
   * @param apis the set of {@link CloudLibrary apis} selected
   * @return the set of {@link Role roles} corresponding to the selected cloudapis
   */
  private Set<Role> getServiceAccountRoles(Set<CloudLibrary> apis) {
    List<Role> serviceAccountRoles = CloudApiManager.getServiceAccountRoles(getCloudProject());

    Set<String> roleIds =
        apis.stream()
            .map(CloudLibrary::getServiceRoles)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

    return serviceAccountRoles
        .stream()
        .filter(role -> roleIds.contains(role.getName()))
        .collect(Collectors.toSet());
  }

  @Override
  protected JComponent createCenterPanel() {
    return cloudApiSelectorPanel.getPanel();
  }

  private boolean isReadyToSubmit() {
    return getSelectedModule() != null && !getSelectedLibraries().isEmpty();
  }
}
