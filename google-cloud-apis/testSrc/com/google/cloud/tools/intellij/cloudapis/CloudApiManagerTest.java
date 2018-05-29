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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.model.Policy;
import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.Iam.Projects.ServiceAccounts;
import com.google.api.services.iam.v1.Iam.Projects.ServiceAccounts.Keys;
import com.google.api.services.iam.v1.model.CreateServiceAccountRequest;
import com.google.api.services.iam.v1.model.Role;
import com.google.api.services.iam.v1.model.ServiceAccount;
import com.google.api.services.iam.v1.model.ServiceAccountKey;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.IntegratedGoogleLoginService;
import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.resources.GoogleApiClientFactory;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestDirectory;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.google.cloud.tools.intellij.testing.TestService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.testFramework.ThreadTracker;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

/** Tests for {@link CloudApiManager} */
public class CloudApiManagerTest {

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
  @TestFixture private IdeaProjectTestFixture testFixture;

  @TestService @Mock private IntegratedGoogleLoginService googleLoginService;
  @TestService @Mock private ProgressManager progressManager;
  @TestService @Mock private GoogleApiClientFactory apiClientFactory;

  @Mock private CredentialedUser user;
  @Mock private ProgressIndicator progressIndicator;
  @Mock private Iam iam;
  @Mock private CloudResourceManager resourceManager;
  @Mock private Iam.Projects iamProjects;
  @Mock private CloudResourceManager.Projects resourceManagerProjects;
  @Mock private ServiceAccounts serviceAccounts;
  @Mock private CloudResourceManager.Projects.GetIamPolicy getIamPolicy;
  @Mock private CloudResourceManager.Projects.SetIamPolicy setIamPolicy;
  @Mock private Keys keys;
  @Mock private ServiceAccounts.Create serviceAccountCreate;
  @Mock private Keys.Create keysCreate;

  @Mock Notifications notifications;

  private ServiceAccount serviceAccount;
  private ServiceAccountKey serviceAccountKey;
  private Policy policy;

  @TestDirectory(name = "downloadDir")
  private File downloadDir;

  private static final String CLOUD_PROJECT_NAME = "my-cloud-project";
  private static final String SERVICE_ACCOUNT_NAME = "my-service-account";
  private final CloudProject cloudProject = CloudProject.create(CLOUD_PROJECT_NAME, "id", "user");

  @Before
  public void setUp() throws IOException {
    when(googleLoginService.getLoggedInUser("user")).thenReturn(Optional.of(user));
    when(progressManager.getProgressIndicator()).thenReturn(progressIndicator);
    when(apiClientFactory.getIamClient(any())).thenReturn(iam);
    when(apiClientFactory.getCloudResourceManagerClient(any())).thenReturn(resourceManager);

    setupFakeServiceAccount();
    setupFakeServiceAccountKey();
    setupFakePolicy();
    setupMockIamClient();
    setupMockResourceManagerClient();

    testFixture
        .getProject()
        .getMessageBus()
        .connect()
        .subscribe(Notifications.TOPIC, notifications);

    // TODO: consider shutting down timer instead when clear what is creating the timer.
    ThreadTracker.longRunningThreadCreated(ApplicationManager.getApplication(), "Timer-0");
  }

  @Test
  public void createServiceAccountAndDownloadKey_withNoRoles_createsKey() {
    Set<Role> roles = ImmutableSet.of();
    CloudApiManager.createServiceAccountAndDownloadKey(
        roles, SERVICE_ACCOUNT_NAME, downloadDir.toPath(), cloudProject, testFixture.getProject());

    verify(progressIndicator, times(3)).setText(anyString());
    verify(progressIndicator, times(3)).setFraction(anyDouble());

    String[] contents = downloadDir.list();
    assertThat(contents.length).isEqualTo(1);
    assertThat(contents[0]).startsWith(CLOUD_PROJECT_NAME);
  }

  @Test
  public void createServiceAccountAndDownloadKey_withRoles_createsKey() {
    Role role = new Role();
    role.setName("my-role");
    Set<Role> roles = ImmutableSet.of(role);
    CloudApiManager.createServiceAccountAndDownloadKey(
        roles, SERVICE_ACCOUNT_NAME, downloadDir.toPath(), cloudProject, testFixture.getProject());

    verify(progressIndicator, times(4)).setText(anyString());
    verify(progressIndicator, times(4)).setFraction(anyDouble());

    String[] contents = downloadDir.list();
    assertThat(contents.length).isEqualTo(1);
    assertThat(contents[0]).startsWith(CLOUD_PROJECT_NAME);
  }

  @Test
  public void createServiceAccountAndDownloadKey_whenThrowingException_notifiesUser()
      throws IOException {
    when(serviceAccountCreate.execute()).thenThrow(new IOException());

    Set<Role> roles = ImmutableSet.of();
    CloudApiManager.createServiceAccountAndDownloadKey(
        roles, SERVICE_ACCOUNT_NAME, downloadDir.toPath(), cloudProject, testFixture.getProject());

    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notifications).notify(captor.capture());
    assertThat(captor.getAllValues().size()).isEqualTo(1);
    assertThat(captor.getValue().getTitle()).isEqualTo("Error Creating Service Account");
  }

  @Test
  public void createServiceAccount_withLongerThanMaxLengthName_trims30ToCharsWithTimestamp()
      throws IOException {
    String serviceAccountNameToTrim = "delete-me-spring-initlzr-demo";
    ArgumentCaptor<CreateServiceAccountRequest> captor =
        captureServiceAccountCreateRequest(serviceAccountNameToTrim);

    String accountId = captor.getValue().getAccountId();
    assertThat(accountId.length()).isEqualTo(30);
    // Full expected string is "delete-me-spring--[timestamp]
    // The double "--" are because of the trimming at the end of the service account name plus
    // the leading "-" from the timestamp
    assertThat(accountId).matches("delete-me-spring--[0-9]{12}");
  }

  @Test
  public void createServiceAccount_withExactMaxLengthName_doesNotTrim() throws IOException {
    String serviceAccountName = "abcdefghijklmnopq";
    ArgumentCaptor<CreateServiceAccountRequest> captor =
        captureServiceAccountCreateRequest(serviceAccountName);

    String accountId = captor.getValue().getAccountId();
    assertThat(accountId.length()).isEqualTo(30);
    assertThat(accountId).matches(serviceAccountName + "-[0-9]{12}");
  }

  @Test
  public void createServiceAccount_withLessThanMaxLengthName_doesNotTrim() throws IOException {
    String serviceAccountName = "abcdefghijklmnop";
    ArgumentCaptor<CreateServiceAccountRequest> captor =
        captureServiceAccountCreateRequest(serviceAccountName);

    String accountId = captor.getValue().getAccountId();
    assertThat(accountId.length()).isEqualTo(29);
    assertThat(accountId).matches(serviceAccountName + "-[0-9]{12}");
  }

  private ArgumentCaptor<CreateServiceAccountRequest> captureServiceAccountCreateRequest(
      String serviceAccountName) throws IOException {
    Set<Role> roles = ImmutableSet.of();
    CloudApiManager.createServiceAccountAndDownloadKey(
        roles, serviceAccountName, downloadDir.toPath(), cloudProject, testFixture.getProject());

    ArgumentCaptor<CreateServiceAccountRequest> captor =
        ArgumentCaptor.forClass(CreateServiceAccountRequest.class);
    verify(serviceAccounts).create(anyString(), captor.capture());

    return captor;
  }

  private void setupMockIamClient() throws IOException {
    when(iam.projects()).thenReturn(iamProjects);
    when(iamProjects.serviceAccounts()).thenReturn(serviceAccounts);
    when(serviceAccounts.keys()).thenReturn(keys);
    when(serviceAccounts.create(anyString(), any())).thenReturn(serviceAccountCreate);
    when(keys.create(anyString(), any())).thenReturn(keysCreate);
    when(serviceAccountCreate.execute()).thenReturn(serviceAccount);
    when(keysCreate.execute()).thenReturn(serviceAccountKey);
  }

  private void setupMockResourceManagerClient() throws IOException {
    when(resourceManager.projects()).thenReturn(resourceManagerProjects);
    when(resourceManagerProjects.getIamPolicy(anyString(), any())).thenReturn(getIamPolicy);
    when(getIamPolicy.execute()).thenReturn(policy);
    when(resourceManagerProjects.setIamPolicy(anyString(), any())).thenReturn(setIamPolicy);
  }

  private void setupFakeServiceAccount() {
    serviceAccount = new ServiceAccount();
    serviceAccount.setName("my-service-account");
  }

  private void setupFakeServiceAccountKey() {
    serviceAccountKey = new ServiceAccountKey();
    serviceAccountKey.setPrivateKeyData("data");
  }

  private void setupFakePolicy() {
    policy = new Policy();
    policy.setBindings(ImmutableList.of());
  }
}
