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

package com.google.cloud.tools.intellij.appengine.java.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.google.cloud.tools.intellij.appengine.java.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.java.sdk.CloudSdkServiceManager;
import com.google.cloud.tools.intellij.appengine.java.sdk.CloudSdkValidator;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.IntegratedGoogleLoginService;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.google.cloud.tools.intellij.testing.TestService;
import com.google.common.collect.ImmutableSet;
import com.google.gdt.eclipse.login.common.GoogleLoginState;
import com.google.gson.Gson;
import com.intellij.openapi.vcs.impl.CancellableRunnable;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance.DeploymentOperationCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import java.io.File;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/** Unit tests for {@link CloudSdkAppEngineHelper} */
public class CloudSdkAppEngineHelperTest {
  @Rule public CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @TestFixture private IdeaProjectTestFixture testFixture;

  private CloudSdkAppEngineHelper helper;
  @Mock private AppEngineDeploymentConfiguration deploymentConfiguration;
  @TestService @Mock private IntegratedGoogleLoginService googleLoginService;
  @Mock private CredentialedUser credentialedUser;
  @Mock private GoogleLoginState loginState;
  @Mock private LoggingHandler loggingHandler;
  @Mock private DeploymentOperationCallback callback;

  @TestService @Mock private CloudSdkServiceManager mockCloudSdkServiceManager;
  @TestService @Mock private CloudSdkService sdkService;
  @TestService @Mock private CloudSdkValidator cloudSdkValidator;
  @Mock private DeploymentSource undeployableDeploymentSource;

  @Before
  public void initialize() {
    helper = new CloudSdkAppEngineHelper(testFixture.getProject());

    when(mockCloudSdkServiceManager.getCloudSdkService()).thenReturn(sdkService);
  }

  @Test
  public void testStageCredentials_withValidCreds() throws Exception {
    String username = "jones@gmail.com";
    String clientId = "clientId";
    String clientSecret = "clientSecret";
    String refreshToken = "refreshToken";
    when(googleLoginService.ensureLoggedIn(username)).thenReturn(true);
    when(googleLoginService.getLoggedInUser(username)).thenReturn(Optional.of(credentialedUser));
    when(credentialedUser.getGoogleLoginState()).thenReturn(loginState);
    when(loginState.fetchOAuth2ClientId()).thenReturn(clientId);
    when(loginState.fetchOAuth2ClientSecret()).thenReturn(clientSecret);
    when(loginState.fetchOAuth2RefreshToken()).thenReturn(refreshToken);
    helper.stageCredentials(username);
    Path credentialFile = helper.getCredentialsPath();
    Reader credentialFileReader = Files.newBufferedReader(credentialFile, Charset.defaultCharset());
    Map jsonMap = new Gson().fromJson(credentialFileReader, Map.class);
    credentialFileReader.close();
    assertEquals(clientId, jsonMap.get("client_id"));
    assertEquals(clientSecret, jsonMap.get("client_secret"));
    assertEquals(refreshToken, jsonMap.get("refresh_token"));
    assertEquals("authorized_user", jsonMap.get("type"));
    Files.delete(credentialFile);
  }

  @Test
  public void testStageCredentials_withoutValidCreds() throws Exception {
    String username = "jones@gmail.com";
    when(googleLoginService.ensureLoggedIn(username)).thenReturn(false);
    assertFalse(helper.stageCredentials(username).isPresent());
  }

  @Test
  public void testCreateDeployRunnerInvalidDeploymentSourceType_returnsNull() {
    Optional<CancellableRunnable> runner =
        helper.createDeployRunner(
            loggingHandler, undeployableDeploymentSource, deploymentConfiguration, callback);

    assertFalse(runner.isPresent());
    verify(callback, times(1)).errorOccurred("Invalid deployment source.");
  }

  @Test
  public void testCreateDeployRunnerInvalidDeploymentSourceFile_returnsNull() {
    when(cloudSdkValidator.validateCloudSdk()).thenReturn(ImmutableSet.of());

    Optional<CancellableRunnable> runner =
        helper.createDeployRunner(
            loggingHandler,
            createMockDeployableDeploymentSource(),
            deploymentConfiguration,
            callback);

    assertFalse(runner.isPresent());
    verify(callback, times(1)).errorOccurred("Deployment source not found: null.");
  }

  @Test
  public void testCreateFlexDeployRunner_noPersistedModule() {
    when(deploymentConfiguration.getModuleName()).thenReturn(null);

    DeploymentSource flexSource = createMockDeployableDeploymentSource();
    when(((AppEngineDeployable) flexSource).getEnvironment())
        .thenReturn(AppEngineEnvironment.APP_ENGINE_FLEX);
    File mockSourceFile = mock(File.class);
    when(mockSourceFile.exists()).thenReturn(true);
    when(flexSource.getFile()).thenReturn(mockSourceFile);

    Optional<CancellableRunnable> runner =
        helper.createDeployRunner(loggingHandler, flexSource, deploymentConfiguration, callback);

    assertFalse(runner.isPresent());
    verify(callback, times(1)).errorOccurred("No app.yaml specified for flexible deployment.");
  }

  private static DeploymentSource createMockDeployableDeploymentSource() {
    return mock(DeploymentSource.class, withSettings().extraInterfaces(AppEngineDeployable.class));
  }
}
