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

package com.google.cloud.tools.intellij.debugger;

import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.services.clouddebugger.v2.Clouddebugger.Debugger;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.IntegratedGoogleLoginService;
import com.google.cloud.tools.intellij.service.PluginInfoService;
import com.google.cloud.tools.intellij.testing.BasePluginTestCase;
import com.google.gdt.eclipse.login.common.GoogleLoginState;
import java.io.IOException;
import java.util.LinkedHashMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CloudDebuggerClientTest extends BasePluginTestCase {

  @Mock PluginInfoService mockInfoService;

  @Before
  public void setUp() {
    IntegratedGoogleLoginService mockLogin = Mockito.mock(IntegratedGoogleLoginService.class);
    registerService(IntegratedGoogleLoginService.class, mockLogin);
    registerService(PluginInfoService.class, mockInfoService);
    LinkedHashMap<String, CredentialedUser> allUsers =
        new LinkedHashMap<String, CredentialedUser>();
    CredentialedUser user = Mockito.mock(CredentialedUser.class);
    allUsers.put("foo@example.com", user);
    when(mockLogin.getAllUsers()).thenReturn(allUsers);
    Credential credential = Mockito.mock(Credential.class);
    when(user.getCredential()).thenReturn(credential);
    GoogleLoginState loginState = Mockito.mock(GoogleLoginState.class);
    when(user.getGoogleLoginState()).thenReturn(loginState);
  }

  @Test
  public void testUserAgent() throws IOException {
    when(mockInfoService.getUserAgent()).thenReturn("userAgent");
    Debugger client = CloudDebuggerClient.getLongTimeoutClient("foo@example.com");
    HttpRequest httpRequest = client.debuggees().list().buildHttpRequestUsingHead();
    HttpHeaders headers = httpRequest.getHeaders();
    String userAgent = headers.getUserAgent();
    Assert.assertTrue(userAgent.startsWith("userAgent"));
    Assert.assertTrue(userAgent.endsWith("Google-API-Java-Client"));
  }

  @Test
  public void testGetShortTimeoutClient_fromUserEmail() {
    Debugger client = CloudDebuggerClient.getShortTimeoutClient("foo@example.com");
    Assert.assertNotNull(client.debuggees().breakpoints());
  }

  @Test
  public void testGetLongTimeoutClient_fromNullUserEmail() {
    Assert.assertNull(CloudDebuggerClient.getLongTimeoutClient((String) null));
  }

  @Test
  public void testGetShortTimeoutClient_fromNullUserEmail() {
    Assert.assertNull(CloudDebuggerClient.getShortTimeoutClient((String) null));
  }
}
