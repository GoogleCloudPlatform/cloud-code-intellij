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

import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.GoogleLoginService;
import com.google.cloud.tools.intellij.testing.BasePluginTestCase;
import com.google.common.collect.ImmutableMap;
import com.google.gdt.eclipse.login.common.GoogleLoginState;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;
import java.io.FileReader;
import java.util.Map;

/**
 * Unit tests for {@link CloudSdkAppEngineHelper}
 */
public class CloudSdkAppEngineHelperTest extends BasePluginTestCase {

  @Mock private AppEngineDeploymentConfiguration deploymentConfiguration;
  @Mock private GoogleLoginService googleLoginService;
  @Mock private CredentialedUser credentialedUser;
  @Mock private GoogleLoginState loginState;
  CloudSdkAppEngineHelper helper;

  @Before
  public void initialize() {
    helper = new CloudSdkAppEngineHelper(
        getProject(),
        new File("someFile.jar"));

    registerService(GoogleLoginService.class, googleLoginService);
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
    helper.stageCredentials(username);
    File credentialFile = helper.getCredentialsPath();
      Map jsonMap = new Gson().fromJson(new FileReader(credentialFile), Map.class);
    assertEquals(clientId, jsonMap.get("client_id"));
    assertEquals(clientSecret, jsonMap.get("client_secret"));
    assertEquals(refreshToken, jsonMap.get("refresh_token"));
    assertEquals("authorized_user", jsonMap.get("type"));
    credentialFile.delete();
  }

}
