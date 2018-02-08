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

import com.google.api.client.util.Base64;
import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.model.Binding;
import com.google.api.services.cloudresourcemanager.model.GetIamPolicyRequest;
import com.google.api.services.cloudresourcemanager.model.Policy;
import com.google.api.services.cloudresourcemanager.model.SetIamPolicyRequest;
import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.model.CreateServiceAccountKeyRequest;
import com.google.api.services.iam.v1.model.CreateServiceAccountRequest;
import com.google.api.services.iam.v1.model.Role;
import com.google.api.services.iam.v1.model.ServiceAccount;
import com.google.api.services.iam.v1.model.ServiceAccountKey;
import com.google.api.services.servicemanagement.ServiceManagement;
import com.google.api.services.servicemanagement.model.EnableServiceRequest;
import com.google.cloud.tools.intellij.analytics.UsageTrackerProvider;
import com.google.cloud.tools.intellij.flags.PropertiesFileFlagReader;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.resources.GoogleApiClientFactory;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.cloud.tools.intellij.util.GctTracking;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.fest.util.Lists;

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

  private static final DateTimeFormatter SERVICE_ACCOUNT_TIMESTAMP_FORMAT =
      DateTimeFormatter.ofPattern("yyMMddHHmmss");

  private static final String SERVICE_REQUEST_PROJECT_PATTERN = "project:%s";
  private static final String SERVICE_ACCOUNT_CREATE_REQUEST_PROJECT_PATTERN = "projects/%s";

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

  static void createServiceAccountAndDownloadKey(
      Set<Role> roles, String name, Path downloadDir, CloudProject cloudProject, Project project) {
    Optional<CredentialedUser> user =
        Services.getLoginService().getLoggedInUser(cloudProject.googleUsername());

    if (!user.isPresent()) {
      LOG.error("Cannot enable APIs: logged in user not found.");
      return;
    }

    ProgressIndicator progress = ProgressManager.getInstance().getProgressIndicator();

    CreateServiceAccountRequest request = new CreateServiceAccountRequest();
    ServiceAccount serviceAccount = new ServiceAccount();
    serviceAccount.setDisplayName(name);
    request.setServiceAccount(serviceAccount);
    request.setAccountId(createServiceAccountId(name));

    Iam iam = GoogleApiClientFactory.getInstance().getIamClient(user.get().getCredential());
    CloudResourceManager resourceManager =
        GoogleApiClientFactory.getInstance()
            .getCloudResourceManagerClient(user.get().getCredential());

    try {
      ServiceAccount newServiceAccount =
          iam.projects()
              .serviceAccounts()
              .create(
                  String.format(
                      SERVICE_ACCOUNT_CREATE_REQUEST_PROJECT_PATTERN, cloudProject.projectId()),
                  request)
              .execute();

      Policy existingPolicy =
          resourceManager
              .projects()
              .getIamPolicy(cloudProject.projectId(), new GetIamPolicyRequest())
              .execute();
      List<Binding> bindings = Lists.newArrayList(existingPolicy.getBindings());

      List<Binding> additionalBindings =
          roles
              .stream()
              .map(
                  role -> {
                    Binding binding = new Binding();
                    binding.setRole(role.getName());
                    binding.setMembers(
                        ImmutableList.of("serviceAccount:" + newServiceAccount.getEmail()));
                    return binding;
                  })
              .collect(Collectors.toList());

      bindings.addAll(additionalBindings);

      SetIamPolicyRequest policyRequest = new SetIamPolicyRequest();
      Policy newPolicy = new Policy();
      newPolicy.setBindings(bindings);
      policyRequest.setPolicy(newPolicy);

      resourceManager.projects().setIamPolicy(cloudProject.projectId(), policyRequest).execute();

      // create the key
      CreateServiceAccountKeyRequest keyRequest = new CreateServiceAccountKeyRequest();
      ServiceAccountKey key =
          iam.projects()
              .serviceAccounts()
              .keys()
              .create(newServiceAccount.getName(), keyRequest)
              .execute();

      Path keyPath =
          Paths.get(
              downloadDir.toString(), cloudProject.projectName() + "-" + getTimestamp() + ".json");
      Files.write(keyPath, Base64.decodeBase64(key.getPrivateKeyData()));

      notifyServiceAccountCreated(project, name, keyPath);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void enableApi(
      CloudLibrary library, CloudProject cloudProject, CredentialedUser user) throws IOException {

    UsageTrackerProvider.getInstance()
        .trackEvent(GctTracking.CLIENT_LIBRARY_ENABLE_API)
        .addMetadata(GctTracking.METADATA_LABEL_KEY, library.getName())
        .ping();

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

  /**
   * Fetches the list of {@link Role} for the supplied {@link CloudProject} by querying the Iam API.
   */
  static List<Role> getServiceAccountRoles(CloudProject cloudProject) {
    Optional<CredentialedUser> user =
        Services.getLoginService().getLoggedInUser(cloudProject.googleUsername());

    if (!user.isPresent()) {
      LOG.error("Cannot fetch service account roles: logged in user not found.");
      return ImmutableList.of();
    }

    Iam iam = GoogleApiClientFactory.getInstance().getIamClient(user.get().getCredential());

    try {
      return iam.roles().list().execute().getRoles();
    } catch (IOException e) {
      LOG.warn("Exception occurred attempting to fetch service account roles");
      return ImmutableList.of();
    }
  }

  /**
   * Creates the unique ID of the service account.
   *
   * <p>Must be less than 30 characters.
   *
   * @param prefix the name chosen by the user for the service account
   * @return an ID that is a combination of the prefix and a timestamp
   */
  private static String createServiceAccountId(String prefix) {
    int maxLen = 30;
    String timestamp = getTimestamp();
    int maxPrefixLen = maxLen - timestamp.length() - 1;
    String trimmedPrefix =
        prefix.length() <= maxPrefixLen ? prefix : prefix.substring(0, maxPrefixLen);
    return String.format("%s-%s", trimmedPrefix, timestamp);
  }

  private static String getTimestamp() {
    return SERVICE_ACCOUNT_TIMESTAMP_FORMAT.format(ZonedDateTime.now());
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

  private static void notifyServiceAccountCreated(Project project, String name, Path downloadDir) {
    Notification notification =
        NOTIFICATION_GROUP.createNotification(
            GctBundle.message("cloud.apis.service.account.created.title"),
            null /*subtitle*/,
            "Service account " + name + " was created",
            NotificationType.INFORMATION);
    notification.notify(project);

    ApplicationManager.getApplication()
        .invokeLater(
            () ->
                Messages.showInfoMessage(
                    "Your service account key was downloaded to "
                        + downloadDir
                        + "\n\nTo access the APIs locally set the following environment variable of your "
                        + "local dev server to point to the key:\n\nGOOGLE_APPLICATION_CREDENTIALS",
                    "Service Account Key Created"));
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
