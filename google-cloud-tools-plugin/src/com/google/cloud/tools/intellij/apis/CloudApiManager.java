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

import com.google.api.services.servicemanagement.ServiceManagement;
import com.google.api.services.servicemanagement.model.EnableServiceRequest;
import com.google.cloud.tools.intellij.flags.PropertiesFileFlagReader;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.resources.GoogleApiClientFactory;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.common.collect.Sets;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Cloud API manager responsible for API management tasks on GCP such as API enablement. */
class CloudApiManager {
  private static final Logger LOG = Logger.getInstance(CloudApiManager.class);

  private static final NotificationGroup NOTIFICATION_GROUP =
      new NotificationGroup(
          new PropertiesFileFlagReader().getFlagString("notifications.plugin.groupdisplayid"),
          NotificationDisplayType.BALLOON,
          true,
          null,
          GoogleCloudToolsIcons.CLOUD);
  private static final String SERVICE_REQUEST_PROJECT_PATTERN = "project:%s";

  private CloudApiManager() {}

  /**
   * Enables the supplied set of {@link CloudLibrary CloudLibraries} on GCP.
   *
   * <p>Configures the {@link ProgressIndicator} to display the progress of the tasks. Also notifies
   * the user of the success / failure of API enablement via messages on the event log.
   *
   * @param libraries the set of {@link CloudLibrary CloudLibraries} to enable on GCP
   * @param cloudProject the {@link CloudProject} on which to enable the APIs
   * @param project the currently open IntelliJ {@link Project}
   */
  static void enableApis(Set<CloudLibrary> libraries, CloudProject cloudProject, Project project) {
    Optional<CredentialedUser> user =
        Services.getLoginService().getLoggedInUser(cloudProject.googleUsername());

    if (!user.isPresent()) {
      LOG.error("Cannot enable APIs: logged in user not found.");
      return;
    }

    List<CloudLibrary> libraryList = new ArrayList<>(libraries);
    Set<CloudLibrary> enabledApis = Sets.newHashSet();
    Set<CloudLibrary> erroredApis = Sets.newHashSet();

    for (int i = 0; i < libraryList.size(); i++) {
      CloudLibrary library = libraryList.get(i);

      try {
        ProgressIndicator progress = ProgressManager.getInstance().getProgressIndicator();

        if (progress.isCanceled()) {
          LOG.info("API enablement canceled by user");
          notifyApiEnableSkipped(Sets.difference(libraries, enabledApis), project);
          return;
        }

        setApiEnableProgress(
            progress,
            library.getName(),
            cloudProject.projectName(),
            (double) i / libraryList.size());
        enableApi(library, cloudProject, user.get());

        enabledApis.add(library);
      } catch (IOException e) {
        LOG.warn("Exception occurred attempting to enable API " + library.getName() + " on GCP", e);
        erroredApis.add(library);
      }
    }

    if (!erroredApis.isEmpty()) {
      notifyApiEnableError(erroredApis, project);
    }
    if (!enabledApis.isEmpty()) {
      notifyApisEnabled(enabledApis, cloudProject.projectId(), project);
    }
  }

  private static void enableApi(
      CloudLibrary library, CloudProject cloudProject, CredentialedUser user) throws IOException {
    ServiceManagement serviceManagement =
        GoogleApiClientFactory.getInstance().getServiceManagementClient(user.getCredential());

    serviceManagement
        .services()
        .enable(
            library.getServiceName(),
            new EnableServiceRequest()
                .setConsumerId(
                    String.format(SERVICE_REQUEST_PROJECT_PATTERN, cloudProject.projectId())))
        .execute();
  }

  private static void notifyApisEnabled(
      Set<CloudLibrary> libraries, String cloudProjectId, Project project) {
    Notification notification =
        NOTIFICATION_GROUP.createNotification(
            GctBundle.message("cloud.apis.enabled.title"),
            null /*subtitle*/,
            GctBundle.message(
                "cloud.apis.enabled.message", cloudProjectId, joinApiNames(libraries)),
            NotificationType.INFORMATION);
    notification.notify(project);
  }

  private static void notifyApiEnableError(Set<CloudLibrary> apis, Project project) {
    Notification notification =
        NOTIFICATION_GROUP.createNotification(
            GctBundle.message("cloud.apis.enable.error.title"),
            null /*subtitle*/,
            GctBundle.message("cloud.apis.enable.error.message", joinApiNames(apis)),
            NotificationType.ERROR);
    notification.notify(project);
  }

  private static void notifyApiEnableSkipped(Set<CloudLibrary> apis, Project project) {
    Notification notification =
        NOTIFICATION_GROUP.createNotification(
            GctBundle.message("cloud.apis.enable.skipped.title"),
            null /*subtitle*/,
            GctBundle.message("cloud.apis.enable.skipped.message", joinApiNames(apis)),
            NotificationType.ERROR);
    notification.notify(project);
  }

  private static String joinApiNames(Set<CloudLibrary> apis) {
    return apis.stream().map(CloudLibrary::getName).collect(Collectors.joining("<br>"));
  }

  private static void setApiEnableProgress(
      ProgressIndicator indicator, String apiName, String cloudProjectName, double fraction) {
    indicator.setText(
        GctBundle.message("cloud.apis.enable.progress.message", apiName, cloudProjectName));
    indicator.setFraction(fraction);
  }
}
