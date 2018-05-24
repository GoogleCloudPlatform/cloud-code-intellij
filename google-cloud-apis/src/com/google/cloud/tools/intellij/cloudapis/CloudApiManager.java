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
import com.google.cloud.tools.intellij.GoogleCloudCoreIcons;
import com.google.cloud.tools.intellij.analytics.GctTracking;
import com.google.cloud.tools.intellij.analytics.UsageTrackerService;
import com.google.cloud.tools.intellij.flags.PropertiesFileFlagReader;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.resources.GoogleApiClientFactory;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import git4idea.DialogManager;
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
import java.util.regex.Pattern;
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
          GoogleCloudCoreIcons.CLOUD);

  private static final DateTimeFormatter SERVICE_ACCOUNT_TIMESTAMP_FORMAT =
      DateTimeFormatter.ofPattern("yyMMddHHmmss");

  private static final String SERVICE_REQUEST_PROJECT_FORMAT = "project:%s";
  private static final String SERVICE_ACCOUNT_CREATE_REQUEST_PROJECT_FORMAT = "projects/%s";
  private static final String SERVICE_ACCOUNT_ROLE_REQUEST_PREFIX = "serviceAccount:";
  private static final String SERVICE_ACCOUNT_KEY_FILE_NAME_FORMAT = "%s-%s.json";

  private static final int SERVICE_ACCOUNT_ID_MAX_LEN = 30;
  static final int SERVICE_ACCOUNT_NAME_MAX_LEN = 100;
  static final Pattern SERVICE_ACCOUNT_ID_PATTERN = Pattern.compile("[a-z][a-z\\d\\-]*[a-z\\d].");

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

        updateProgress(
            progress,
            GoogleCloudApisMessageBundle.message(
                "cloud.apis.enable.progress.message",
                library.getName(),
                cloudProject.projectName()),
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

  /**
   * Creates a new {@link ServiceAccount}, adds the supplied set of {@link Role roles} to it, and
   * creates and downloads the service account private key to the user's file system.
   *
   * @param roles the set of {@link Role} to add to the new service account
   * @param name the name of the new service account to be created
   * @param downloadDir the {@link Path} of the download directory of the service account private
   *     key json file
   * @param cloudProject the current {@link CloudProject}
   * @param project the current {@link Project}
   */
  static void createServiceAccountAndDownloadKey(
      Set<Role> roles, String name, Path downloadDir, CloudProject cloudProject, Project project) {
    Optional<CredentialedUser> user =
        Services.getLoginService().getLoggedInUser(cloudProject.googleUsername());

    if (!user.isPresent()) {
      LOG.error("Cannot enable APIs: logged in user not found.");
      return;
    }

    ProgressIndicator progress =
        ServiceManager.getService(ProgressManager.class).getProgressIndicator();

    try {
      int numSteps = roles.isEmpty() ? 3 : 4;
      double step = 0;

      updateProgress(
          progress,
          GoogleCloudApisMessageBundle.message(
              "cloud.apis.service.account.create.account.progress.message", name),
          step / numSteps);
      step++;
      ServiceAccount serviceAccount = createServiceAccount(user.get(), name, cloudProject);

      if (!roles.isEmpty()) {
        updateProgress(
            progress,
            GoogleCloudApisMessageBundle.message(
                "cloud.apis.service.account.add.roles.progress.message"),
            step / numSteps);
        step++;
        addRolesToServiceAccount(user.get(), serviceAccount, roles, cloudProject);
      }

      updateProgress(
          progress,
          GoogleCloudApisMessageBundle.message(
              "cloud.apis.service.account.create.key.progress.message"),
          step / numSteps);
      step++;
      ServiceAccountKey serviceAccountKey = createServiceAccountKey(user.get(), serviceAccount);

      updateProgress(
          progress,
          GoogleCloudApisMessageBundle.message(
              "cloud.apis.service.account.download.key.progress.message"),
          step / numSteps);
      Path keyPath = writeServiceAccountKey(serviceAccountKey, downloadDir, cloudProject);

      UsageTrackerService.getInstance()
          .trackEvent(GctTracking.CLIENT_LIBRARY_NEW_SERVICE_ACCOUNT)
          .addMetadata(
              GctTracking.METADATA_SERVICE_ACCOUNT_ROLES,
              roles.stream().map(Role::getName).collect(Collectors.joining(",")))
          .ping();

      notifyServiceAccountCreated(project, cloudProject, name, keyPath);
    } catch (IOException e) {
      LOG.warn(
          "Exception occurred attempting to create service account on GCP and download its key", e);
      notifyServiceAccountError(project, name, e.toString());
    }
  }

  /** Creates a new {@link ServiceAccount} for the given {@link CloudProject} using the IAM API. */
  private static ServiceAccount createServiceAccount(
      CredentialedUser user, String name, CloudProject cloudProject) throws IOException {
    CreateServiceAccountRequest request = new CreateServiceAccountRequest();
    ServiceAccount serviceAccount = new ServiceAccount();
    serviceAccount.setDisplayName(name);
    request.setServiceAccount(serviceAccount);
    request.setAccountId(createServiceAccountId(name));

    Iam iam = GoogleApiClientFactory.getInstance().getIamClient(user.getCredential());

    return iam.projects()
        .serviceAccounts()
        .create(
            String.format(SERVICE_ACCOUNT_CREATE_REQUEST_PROJECT_FORMAT, cloudProject.projectId()),
            request)
        .execute();
  }

  /**
   * Adds a set of {@link Role roles} to a {@link ServiceAccount}.
   *
   * <p>This is done by fetching the cloud project's existing IAM Policy, adding the new roles to
   * the given service account, and then writing the updated policy back to the cloud project.
   *
   * @param user the current {@link CredentialedUser}
   * @param serviceAccount the {@link ServiceAccount} to which to add roles
   * @param roles the set of {@link Role} to be added to the service account
   * @param cloudProject the current {@link CloudProject}
   * @throws IOException if the API call fails to update the IAM policy
   */
  private static void addRolesToServiceAccount(
      CredentialedUser user,
      ServiceAccount serviceAccount,
      Set<Role> roles,
      CloudProject cloudProject)
      throws IOException {
    CloudResourceManager resourceManager =
        GoogleApiClientFactory.getInstance().getCloudResourceManagerClient(user.getCredential());

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
                  binding.setMembers(createServiceAccountMemberBindings(serviceAccount));
                  return binding;
                })
            .collect(Collectors.toList());

    bindings.addAll(additionalBindings);

    SetIamPolicyRequest policyRequest = new SetIamPolicyRequest();
    Policy newPolicy = new Policy();
    newPolicy.setBindings(bindings);
    policyRequest.setPolicy(newPolicy);

    resourceManager.projects().setIamPolicy(cloudProject.projectId(), policyRequest).execute();
  }

  /**
   * Given an {@link ServiceAccount}, return the singleton list of strings representing the service
   * account member requesting access to the resource. Used to pass into {@link
   * Binding#setMembers(List)}.
   */
  private static List<String> createServiceAccountMemberBindings(ServiceAccount serviceAccount) {
    return ImmutableList.of(SERVICE_ACCOUNT_ROLE_REQUEST_PREFIX + serviceAccount.getEmail());
  }

  /**
   * Using the supplied {@link ServiceAccount}, this creates and returns a new {@link
   * ServiceAccountKey}.
   */
  private static ServiceAccountKey createServiceAccountKey(
      CredentialedUser user, ServiceAccount serviceAccount) throws IOException {
    Iam iam = GoogleApiClientFactory.getInstance().getIamClient(user.getCredential());

    CreateServiceAccountKeyRequest keyRequest = new CreateServiceAccountKeyRequest();
    return iam.projects()
        .serviceAccounts()
        .keys()
        .create(serviceAccount.getName(), keyRequest)
        .execute();
  }

  /**
   * Writes the service account private key data in JSON form to the filesystem. The private key
   * itself is contained within the {@link ServiceAccountKey} returned from the IAM API. The json
   * key is encoded within {@link ServiceAccountKey#getPrivateKeyData()} in base64.
   *
   * @param key the {@link ServiceAccountKey} containing the base64 encoding of the json private key
   * @param downloadDir the {@link Path} on the file system to download the ky
   * @param cloudProject the {@link CloudProject} associated with this service account
   * @return the {@link Path} to the service account key that was written
   * @throws IOException if the an IO error occurs when writing the file
   */
  private static Path writeServiceAccountKey(
      ServiceAccountKey key, Path downloadDir, CloudProject cloudProject) throws IOException {
    Path keyPath =
        Paths.get(downloadDir.toString(), getServiceAccountKeyName(cloudProject.projectName()));
    return Files.write(keyPath, Base64.decodeBase64(key.getPrivateKeyData()));
  }

  private static String getServiceAccountKeyName(String cloudProjectName) {
    return String.format(SERVICE_ACCOUNT_KEY_FILE_NAME_FORMAT, cloudProjectName, getTimestamp());
  }

  private static void enableApi(
      CloudLibrary library, CloudProject cloudProject, CredentialedUser user) throws IOException {

    UsageTrackerService.getInstance()
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
                    String.format(SERVICE_REQUEST_PROJECT_FORMAT, cloudProject.projectId())))
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
   * Creates the unique ID for the service account in the form: [name]-[timestamp].
   *
   * <p>Trims the id if necessary to be less than the max allowed characters by first trimming the
   * service account name and then appending the timestamp so that the timestamp is always trailing.
   *
   * @param name the name chosen by the user for the service account
   * @return an usable ID for GCP that is a combination of a prefix, the filtered name, and a
   *     timestamp
   */
  private static String createServiceAccountId(String name) {
    String timestamp = getTimestamp();
    int maxLengthMinusTimestamp = SERVICE_ACCOUNT_ID_MAX_LEN - (timestamp.length() + 1);

    String trimmed = name;
    if (name.length() > maxLengthMinusTimestamp) {
      trimmed = name.substring(0, maxLengthMinusTimestamp);
    }

    return String.format("%s-%s", trimmed, getTimestamp());
  }

  private static String getTimestamp() {
    return SERVICE_ACCOUNT_TIMESTAMP_FORMAT.format(ZonedDateTime.now());
  }

  private static void notifyApisEnabled(
      Set<CloudLibrary> libraries, String cloudProjectId, Project project) {
    Notification notification =
        NOTIFICATION_GROUP.createNotification(
            GoogleCloudApisMessageBundle.message("cloud.apis.enabled.title"),
            null /*subtitle*/,
            GoogleCloudApisMessageBundle.message(
                "cloud.apis.enabled.message", cloudProjectId, joinApiNames(libraries)),
            NotificationType.INFORMATION);
    notification.notify(project);
  }

  private static void notifyApiEnableError(Set<CloudLibrary> apis, Project project) {
    Notification notification =
        NOTIFICATION_GROUP.createNotification(
            GoogleCloudApisMessageBundle.message("cloud.apis.enable.error.title"),
            null /*subtitle*/,
            GoogleCloudApisMessageBundle.message(
                "cloud.apis.enable.error.message", joinApiNames(apis)),
            NotificationType.ERROR);
    notification.notify(project);
  }

  private static void notifyApiEnableSkipped(Set<CloudLibrary> apis, Project project) {
    Notification notification =
        NOTIFICATION_GROUP.createNotification(
            GoogleCloudApisMessageBundle.message("cloud.apis.enable.skipped.title"),
            null /*subtitle*/,
            GoogleCloudApisMessageBundle.message(
                "cloud.apis.enable.skipped.message", joinApiNames(apis)),
            NotificationType.ERROR);
    notification.notify(project);
  }

  private static void notifyServiceAccountCreated(
      Project project, CloudProject cloudProject, String name, Path downloadDir) {
    Notification notification =
        NOTIFICATION_GROUP.createNotification(
            GoogleCloudApisMessageBundle.message("cloud.apis.service.account.created.title"),
            null /*subtitle*/,
            GoogleCloudApisMessageBundle.message(
                "cloud.apis.service.account.created.message", name),
            NotificationType.INFORMATION);
    notification.notify(project);

    ApplicationManager.getApplication()
        .invokeLater(
            () -> {
              ServiceAccountKeyDialogService dialogService =
                  ServiceManager.getService(ServiceAccountKeyDialogService.class);
              DialogWrapper keyDialog =
                  dialogService.getDialog(project, cloudProject, downloadDir.toString());
              DialogManager.show(keyDialog);
            });
  }

  private static void notifyServiceAccountError(Project project, String name, String errorMessage) {
    Notification notification =
        NOTIFICATION_GROUP.createNotification(
            GoogleCloudApisMessageBundle.message("cloud.apis.service.account.created.error.title"),
            null /*subtitle*/,
            GoogleCloudApisMessageBundle.message(
                "cloud.apis.service.account.created.error.message", name, errorMessage),
            NotificationType.ERROR);
    notification.notify(project);
  }

  private static String joinApiNames(Set<CloudLibrary> apis) {
    return apis.stream().map(CloudLibrary::getName).collect(Collectors.joining("<br>"));
  }

  private static void updateProgress(ProgressIndicator indicator, String message, double fraction) {
    indicator.setText(message);
    indicator.setFraction(fraction);
  }
}
