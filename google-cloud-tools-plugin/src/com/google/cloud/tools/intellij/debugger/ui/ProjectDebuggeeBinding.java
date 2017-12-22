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

package com.google.cloud.tools.intellij.debugger.ui;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.clouddebugger.v2.Clouddebugger.Debugger;
import com.google.api.services.clouddebugger.v2.model.Debuggee;
import com.google.api.services.clouddebugger.v2.model.ListDebuggeesResponse;
import com.google.cloud.tools.intellij.CloudToolsPluginInfoService;
import com.google.cloud.tools.intellij.debugger.CloudDebugProcessState;
import com.google.cloud.tools.intellij.debugger.CloudDebuggerClient;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.project.ProjectSelector;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.base.Strings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.HashMap;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This binding between the project and debuggee is refactored out to make it reusable in the
 * future.
 */
@SuppressWarnings("FutureReturnValueIgnored")
class ProjectDebuggeeBinding {

  private static final Logger LOG = Logger.getInstance(ProjectDebuggeeBinding.class);
  private final JComboBox<DebugTargetSelectorItem> targetSelector;
  private final ProjectSelector projectSelector;
  private final Action okAction;
  private Debugger cloudDebuggerClient = null;
  private CredentialedUser credentialedUser = null;
  private CloudDebugProcessState inputState;
  // Avoids attach dialog starting with an error message that goes away shortly.
  // We should only say a user doesn't have access to a project after querying CDB for debuggees.
  // TODO(joaomartins,eshaul): understand why the first invocation of refreshDebugTargetList
  //   has projectSelector.getProjectNumber() set to null.
  private boolean isCdbQueried = false;

  @SuppressWarnings("unchecked")
  ProjectDebuggeeBinding(
      @NotNull ProjectSelector projectSelector,
      @NotNull JComboBox targetSelector,
      @NotNull Action okAction) {
    this.projectSelector = projectSelector;
    this.targetSelector = targetSelector;
    this.okAction = okAction;

    this.projectSelector.addProjectSelectionListener(this::refreshDebugTargetList);
  }

  @NotNull
  public CloudDebugProcessState buildResult(Project project) {
    CloudProject cloudProject = projectSelector.getSelectedProject();
    String projectId = Optional.ofNullable(cloudProject).map(CloudProject::projectId).orElse("");
    String projectNumberString =
        Optional.ofNullable(cloudProject)
            .map(CloudProject::projectNumber)
            .map(Object::toString)
            .orElse(null);

    DebugTarget selectedItem = (DebugTarget) targetSelector.getSelectedItem();
    String savedDebuggeeId = selectedItem != null ? selectedItem.getId() : null;

    return new CloudDebugProcessState(
        credentialedUser != null ? credentialedUser.getEmail() : null,
        savedDebuggeeId,
        projectId,
        projectNumberString,
        project);
  }

  @Nullable
  private Debugger getCloudDebuggerClient() {
    CloudProject cloudProject = projectSelector.getSelectedProject();
    CredentialedUser credentialedUser =
        cloudProject == null
            ? null
            : Services.getLoginService()
                .getLoggedInUser(cloudProject.googleUsername())
                .orElse(null);

    if (this.credentialedUser == credentialedUser) {
      return cloudDebuggerClient;
    }

    this.credentialedUser = credentialedUser;
    cloudDebuggerClient =
        this.credentialedUser != null
            ? CloudDebuggerClient.getLongTimeoutClient(this.credentialedUser.getEmail())
            : null;

    return cloudDebuggerClient;
  }

  @Nullable
  public CloudDebugProcessState getInputState() {
    return inputState;
  }

  public void setInputState(@Nullable CloudDebugProcessState inputState) {
    this.inputState = inputState;
    if (this.inputState != null
        && !Strings.isNullOrEmpty(this.inputState.getProjectName())
        && !Strings.isNullOrEmpty(this.inputState.getUserEmail())) {
      Long projectNumber = null;
      if (!Strings.isNullOrEmpty(this.inputState.getProjectNumber())) {
        projectNumber = Long.parseLong(this.inputState.getProjectNumber());
      }
      projectSelector.setSelectedProject(
          CloudProject.create(
              this.inputState.getProjectName(), // TODO(ivanporty) add separate project name/ID
              this.inputState.getProjectName(),
              projectNumber,
              this.inputState.getUserEmail()));

      // update the state here as well
      refreshDebugTargetList(projectSelector.getSelectedProject());
    }
  }

  /** Refreshes the list of attachable debug targets based on the project selection. */
  @SuppressWarnings("FutureReturnValueIgnored")
  private void refreshDebugTargetList(CloudProject cloudProject) {
    targetSelector.removeAllItems();
    ApplicationManager.getApplication()
        .executeOnPooledThread(
            () -> {
              try {
                String projectNumber =
                    Optional.ofNullable(cloudProject.projectNumber())
                        .map(Object::toString)
                        .orElse(null);

                if (projectNumber != null && getCloudDebuggerClient() != null) {
                  final ListDebuggeesResponse debuggees =
                      getCloudDebuggerClient()
                          .debuggees()
                          .list()
                          .setProject(projectNumber)
                          .setClientVersion(
                              ServiceManager.getService(CloudToolsPluginInfoService.class)
                                  .getClientVersionForCloudDebugger())
                          .execute();
                  isCdbQueried = true;

                  SwingUtilities.invokeLater(
                      () -> {
                        DebugTarget targetSelection = null;

                        if (debuggees == null
                            || debuggees.getDebuggees() == null
                            || debuggees.getDebuggees().isEmpty()) {
                          disableTargetSelector(GctBundle.getString("clouddebug.nomodulesfound"));
                        } else {
                          targetSelector.setEnabled(true);
                          Map<String, DebugTarget> perModuleCache = new HashMap<>();

                          for (Debuggee debuggee : debuggees.getDebuggees()) {
                            DebugTarget item = new DebugTarget(debuggee, cloudProject.projectId());
                            if (!Strings.isNullOrEmpty(item.getModule())
                                && !Strings.isNullOrEmpty(item.getVersion())) {
                              // If we already have an existing item for that module+version,
                              // compare the minor versions and only use the latest minor version.
                              String key =
                                  String.format("%s:%s", item.getModule(), item.getVersion());
                              DebugTarget existing = perModuleCache.get(key);
                              if (existing != null
                                  && existing.getMinorVersion() > item.getMinorVersion()) {
                                continue;
                              }
                              if (existing != null) {
                                targetSelector.removeItem(existing);
                              }
                              perModuleCache.put(key, item);
                            }
                            if (inputState != null
                                && !Strings.isNullOrEmpty(inputState.getDebuggeeId())) {
                              if (inputState.getDebuggeeId().equals(item.getId())) {
                                targetSelection = item;
                              }
                            }
                            targetSelector.addItem(item);
                            okAction.setEnabled(true);
                          }
                        }
                        if (targetSelection != null) {
                          targetSelector.setSelectedItem(targetSelection);
                        }
                      });
                }
              } catch (final IOException ex) {
                SwingUtilities.invokeLater(() -> disableTargetSelector(ex));

                LOG.warn("Error listing debuggees from Cloud Debugger API", ex);
              }
            });
  }

  private void disableTargetSelector(Throwable reason) {
    targetSelector.setEnabled(false);

    String errorMessage = resolveErrorToMessage(reason);
    disableTargetSelector(errorMessage);
  }

  private void disableTargetSelector(String reason) {
    targetSelector.setEnabled(false);

    if (targetSelector.getSelectedItem() instanceof ErrorHolder) {
      ((ErrorHolder) targetSelector.getSelectedItem()).setErrorMessage(reason);
    } else {
      targetSelector.addItem(new ErrorHolder(reason));
    }
  }

  private static String resolveErrorToMessage(Throwable reason) {
    if (reason instanceof GoogleJsonResponseException) {
      return resolveJsonResponseToMessage((GoogleJsonResponseException) reason);
    } else {
      return GctBundle.getString("clouddebug.debug.targets.error", reason.getLocalizedMessage());
    }
  }

  private static String resolveJsonResponseToMessage(GoogleJsonResponseException reason) {
    switch (reason.getStatusCode()) {
      case 403:
        return GctBundle.message("clouddebug.debug.targets.accessdenied");
      default:
        return GctBundle.getString(
            "clouddebug.debug.targets.error", reason.getDetails().getMessage());
    }
  }

  public boolean isCdbQueried() {
    return isCdbQueried;
  }
}
