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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.GoogleLoginService;
import com.google.cloud.tools.intellij.testing.BasePluginTestCase;
import com.google.common.collect.ImmutableSet;
import com.google.gdt.eclipse.login.common.GoogleLoginState;
import com.google.gson.Gson;

import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.configuration.deployment.DeploymentSourceType;
import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance.DeploymentOperationCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import javax.swing.Icon;

/** Unit tests for {@link CloudSdkAppEngineHelper} */
public class CloudSdkAppEngineHelperTest extends BasePluginTestCase {

  CloudSdkAppEngineHelper helper;
  @Mock private AppEngineDeploymentConfiguration deploymentConfiguration;
  @Mock private GoogleLoginService googleLoginService;
  @Mock private CredentialedUser credentialedUser;
  @Mock private GoogleLoginState loginState;
  @Mock private LoggingHandler loggingHandler;
  @Mock private DeploymentOperationCallback callback;
  @Mock private CloudSdkService sdkService;

  @Before
  public void initialize() {
    helper = new CloudSdkAppEngineHelper(getProject());

    registerService(GoogleLoginService.class, googleLoginService);
    registerService(CloudSdkService.class, sdkService);
  }

  @Test
  public void testStageCredentials_withValidCreds() throws Exception {
    String username = "jones@gmail.com";
    String clientId = "clientId";
    String clientSecret = "clientSecret";
    String refreshToken = "refreshToken";
    when(deploymentConfiguration.getGoogleUsername()).thenReturn(username);
    when(googleLoginService.ensureLoggedIn(username)).thenReturn(true);
    when(googleLoginService.getLoggedInUser(username)).thenReturn(Optional.of(credentialedUser));
    when(credentialedUser.getGoogleLoginState()).thenReturn(loginState);
    when(loginState.fetchOAuth2ClientId()).thenReturn(clientId);
    when(loginState.fetchOAuth2ClientSecret()).thenReturn(clientSecret);
    when(loginState.fetchOAuth2RefreshToken()).thenReturn(refreshToken);
    helper.stageCredentials(username);
    Path credentialFile = helper.getCredentialsPath();
    Reader credentialFileReader = Files.newBufferedReader(credentialFile, UTF_8);
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
    when(deploymentConfiguration.getGoogleUsername()).thenReturn(username);
    when(googleLoginService.ensureLoggedIn(username)).thenReturn(false);
    assertNull(helper.stageCredentials(username));
  }

  @Test
  public void testCreateDeployRunnerInvalidDeploymentSourceType_throwsException() {
    try {
      helper.createDeployRunner(
          loggingHandler, new SimpleDeploymentSource(), deploymentConfiguration, callback);
      fail("Expected RuntimeException");
    } catch (RuntimeException re) {
      verify(callback, times(1)).errorOccurred("Invalid deployment source.");
    }
  }

  @Test
  public void testCreateDeployRunnerInvalidDeploymentSourceFile_returnsNull() {
    when(sdkService.validateCloudSdk()).thenReturn(ImmutableSet.of());

    Runnable runner =
        helper.createDeployRunner(
            loggingHandler, new DeployableDeploymentSource(), deploymentConfiguration, callback);

    assertNull(runner);
    verify(callback, times(1)).errorOccurred("Deployment source not found: null.");
  }

  @Test
  public void testCreateDeployRunnerInvalidSdk() {
    when(sdkService.validateCloudSdk())
        .thenReturn(ImmutableSet.of(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND));
    Path path = Paths.get(("/this/path"));
    when(sdkService.getSdkHomePath()).thenReturn(path);

    Runnable runner =
        helper.createDeployRunner(
            loggingHandler, new DeployableDeploymentSource(), deploymentConfiguration, callback);

    assertNull(runner);
    verify(callback, times(1))
        .errorOccurred("No Cloud SDK was found in the specified directory. " + path.toString());
  }

  private static class SimpleDeploymentSource implements DeploymentSource {

    @Nullable
    @Override
    public File getFile() {
      return null;
    }

    @Nullable
    @Override
    public String getFilePath() {
      return null;
    }

    @NotNull
    @Override
    public String getPresentableName() {
      return null;
    }

    @Nullable
    @Override
    public Icon getIcon() {
      return null;
    }

    @Override
    public boolean isValid() {
      return false;
    }

    @Override
    public boolean isArchive() {
      return false;
    }

    @NotNull
    @Override
    public DeploymentSourceType<?> getType() {
      return null;
    }
  }

  private static class DeployableDeploymentSource implements DeploymentSource, AppEngineDeployable {

    @Override
    public AppEngineEnvironment getEnvironment() {
      return null;
    }

    @Override
    public String getProjectName() {
      return null;
    }

    @Override
    public void setProjectName(String projectName) {}

    @Override
    public String getVersion() {
      return null;
    }

    @Override
    public void setVersion(String version) {}

    @Nullable
    @Override
    public File getFile() {
      return null;
    }

    @Nullable
    @Override
    public String getFilePath() {
      return null;
    }

    @NotNull
    @Override
    public String getPresentableName() {
      return null;
    }

    @Nullable
    @Override
    public Icon getIcon() {
      return null;
    }

    @Override
    public boolean isValid() {
      return false;
    }

    @Override
    public boolean isArchive() {
      return false;
    }

    @NotNull
    @Override
    public DeploymentSourceType<?> getType() {
      return null;
    }
  }
}
