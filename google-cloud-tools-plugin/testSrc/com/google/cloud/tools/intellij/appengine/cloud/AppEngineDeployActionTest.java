/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.cloud.tools.intellij.testing.BasePluginTestCase;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.GoogleLoginService;
import com.google.gdt.eclipse.login.common.GoogleLoginState;
import com.google.gson.Gson;

import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance.DeploymentOperationCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;
import java.io.FileReader;
import java.util.Map;

/**
 * Test case for {@link AppEngineDeployAction}.
 */
public class AppEngineDeployActionTest extends BasePluginTestCase {

  @Mock private LoggingHandler loggingHandler;
  @Mock private File deploymentArtifactPath;
  @Mock private File appYamlPath;
  @Mock private File dockerFilePath;
  @Mock private AppEngineHelper appEngineHelper;
  @Mock private DeploymentOperationCallback callback;
  @Mock private GoogleLoginService googleLoginService;
  @Mock private CredentialedUser credentialedUser;
  @Mock private GoogleLoginState loginState;
  AppEngineDeployAction appEngineDeployAction;
  File credentialFile;

  @Before
  public void initialize() {
    registerService(GoogleLoginService.class, googleLoginService);
    when(deploymentArtifactPath.getPath()).thenReturn("bla.jar");
    appEngineDeployAction = new AppEngineDeployAction(
        appEngineHelper,
        loggingHandler,
        project,
        deploymentArtifactPath,
        appYamlPath,
        dockerFilePath,
        "version",
        callback);
  }
  @Test
  public void testCreateApplicationDefaultCredentials() throws Exception {
    String username = "jones@gmail.com";
    String clientId = "clientId";
    String clientSecret = "clientSecret";
    String refreshToken = "refreshToken";
    when(appEngineHelper.getGoogleUsername()).thenReturn(username);
    when(googleLoginService.getAllUsers())
        .thenReturn(ImmutableMap.of(username, credentialedUser));
    when(credentialedUser.getGoogleLoginState()).thenReturn(loginState);
    when(loginState.fetchOAuth2ClientId()).thenReturn(clientId);
    when(loginState.fetchOAuth2ClientSecret()).thenReturn(clientSecret);
    when(loginState.fetchOAuth2RefreshToken()).thenReturn(refreshToken);
    credentialFile = appEngineDeployAction.createApplicationDefaultCredentials();
    Map jsonMap = new Gson().fromJson(new FileReader(credentialFile), Map.class);
    assertEquals(clientId, jsonMap.get("client_id"));
    assertEquals(clientSecret, jsonMap.get("client_secret"));
    assertEquals(refreshToken, jsonMap.get("refresh_token"));
    assertEquals("authorized_user", jsonMap.get("type"));
  }

  @After
  public void deleteCredentialFile() {
    credentialFile.delete();
  }
}
