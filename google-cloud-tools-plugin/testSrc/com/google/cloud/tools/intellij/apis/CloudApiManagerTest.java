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
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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

  private ServiceAccount serviceAccount;
  private ServiceAccountKey serviceAccountKey;
  private Policy policy;

  @TestDirectory(name = "downlaodDir")
  private File downloadDir;

  private static final String CLOUD_PROJECT_NAME = "my-cloud-project";
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
  }

  @Test
  public void createServiceAccountAndDownloadKey_withNoRoles_createsKey() {
    Set<Role> roles = ImmutableSet.of();
    CloudApiManager.createServiceAccountAndDownloadKey(
        roles, "my-roles", downloadDir.toPath(), cloudProject, testFixture.getProject());

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
        roles, "my-roles", downloadDir.toPath(), cloudProject, testFixture.getProject());

    verify(progressIndicator, times(4)).setText(anyString());
    verify(progressIndicator, times(4)).setFraction(anyDouble());

    String[] contents = downloadDir.list();

    assertThat(contents.length).isEqualTo(1);
    assertThat(contents[0]).startsWith(CLOUD_PROJECT_NAME);
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
