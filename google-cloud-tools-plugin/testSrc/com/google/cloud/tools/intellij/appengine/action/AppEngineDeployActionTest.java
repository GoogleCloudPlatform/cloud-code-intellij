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

package com.google.cloud.tools.intellij.appengine.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.action.configuration.AppEngineDeploymentConfiguration;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.GoogleLoginService;
import com.google.cloud.tools.intellij.testing.BasePluginTestCase;
import com.google.common.collect.ImmutableMap;
import com.google.gdt.eclipse.login.common.GoogleLoginState;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance.DeploymentOperationCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

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
  @Mock private AppEngineDeploymentConfiguration deploymentConfiguration;
  @Mock private AppEngineHelper appEngineHelper;
  @Mock private DeploymentOperationCallback callback;
  @Mock private GoogleLoginService googleLoginService;
  @Mock private CredentialedUser credentialedUser;
  @Mock private GoogleLoginState loginState;
  AppEngineDeployAction appEngineDeployAction;

  @Before
  public void initialize() {
    registerService(GoogleLoginService.class, googleLoginService);
    when(deploymentArtifactPath.getPath()).thenReturn("bla.jar");
    appEngineDeployAction = new AppEngineDeployAction(
        appEngineHelper,
        loggingHandler,
        project,
        deploymentArtifactPath,
        deploymentConfiguration,
        callback);
  }
  @Test
  public void testCreateApplicationDefaultCredentials() throws Exception {
    String username = "jones@gmail.com";
    String clientId = "clientId";
    String clientSecret = "clientSecret";
    String refreshToken = "refreshToken";
    when(deploymentConfiguration.getGoogleUsername()).thenReturn(username);
    when(googleLoginService.getAllUsers())
        .thenReturn(ImmutableMap.of(username, credentialedUser));
    when(credentialedUser.getGoogleLoginState()).thenReturn(loginState);
    when(loginState.fetchOAuth2ClientId()).thenReturn(clientId);
    when(loginState.fetchOAuth2ClientSecret()).thenReturn(clientSecret);
    when(loginState.fetchOAuth2RefreshToken()).thenReturn(refreshToken);
    File credentialFile = appEngineDeployAction.createApplicationDefaultCredentials();
    Map jsonMap = new Gson().fromJson(new FileReader(credentialFile), Map.class);
    assertEquals(clientId, jsonMap.get("client_id"));
    assertEquals(clientSecret, jsonMap.get("client_secret"));
    assertEquals(refreshToken, jsonMap.get("refresh_token"));
    assertEquals("authorized_user", jsonMap.get("type"));
    credentialFile.delete();
  }

  @Test
  public void testDeployOutputJsonParsingOneVersion() {
    String jsonOutput =
        "{\n" +
        "  \"configs\": [],\n" +
        "  \"versions\": [\n" +
        "    {\n" +
        "      \"id\": \"20160429t112518\",\n" +
        "      \"last_deployed_time\": null,\n" +
        "      \"project\": \"some-project\",\n" +
        "      \"service\": \"default\",\n" +
        "      \"traffic_split\": null,\n" +
        "      \"version\": null\n" +
        "    }\n" +
        "  ]\n" +
        "}\n";

    AppEngineDeployAction.DeployOutput deployOutput =
        AppEngineDeployAction.parseDeployOutput(jsonOutput);
    assertEquals(deployOutput.getVersion(), "20160429t112518");
    assertEquals(deployOutput.getService(), "default");
  }

  @Test
  public void testDeployOutputJsonParsingTwoVersions() {
    String jsonOutput =
        "{\n" +
        "  \"configs\": [],\n" +
        "  \"versions\": [\n" +
        "    {\n" +
        "      \"id\": \"20160429t112518\",\n" +
        "      \"last_deployed_time\": null,\n" +
        "      \"project\": \"some-project\",\n" +
        "      \"service\": \"default\",\n" +
        "      \"traffic_split\": null,\n" +
        "      \"version\": null\n" +
        "    },\n" +
        "    {\n" +
        "      \"id\": \"20160429t112518\",\n" +
        "      \"last_deployed_time\": null,\n" +
        "      \"project\": \"some-project\",\n" +
        "      \"service\": \"default\",\n" +
        "      \"traffic_split\": null,\n" +
        "      \"version\": null\n" +
        "    }\n" +
        "  ]\n" +
        "}\n";

    try {
      AppEngineDeployAction.parseDeployOutput(jsonOutput);
      fail();
    } catch (JsonParseException e) {
      // Success! Should throw a JsonParseException.
    }
  }

  @Test
  public void testDeployOutputJsonParsingOldFormat() {
    String jsonOutput =
        "{\n" +
        "  \"default\": \"https://springboot-maven-project.appspot.com\"\n" +
        "}\n";

    try {
      AppEngineDeployAction.parseDeployOutput(jsonOutput);
      fail();
    } catch (JsonParseException e) {
      // Success! Should throw a JsonParseException.
    }
  }
}
