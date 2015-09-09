/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.google.gct.idea.git;

import com.google.gct.idea.elysium.SelectUserDialog;
import com.google.gct.login.CredentialedUser;
import com.google.gct.login.GoogleLogin;
import com.google.gct.login.MockGoogleLogin;
import com.google.gdt.eclipse.login.common.GoogleLoginState;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.testFramework.LightIdeaTestCase;
import com.intellij.util.AuthData;
import git4idea.DialogManager;
import git4idea.test.TestDialogHandler;
import git4idea.test.TestDialogManager;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;

import java.util.LinkedHashMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link com.google.gct.idea.git.GcpHttpAuthDataProvider}
 */
public class GcpHttpAuthProviderTest extends LightIdeaTestCase {
  public static final String GOOGLE_URL = "https://source.developers.google.com";
  private static final String USER = "user@gmail.com";
  private static final String PASSWORD = "123";
  private static final String CACHE_KEY = "com.google.gct.idea.git.username";

  @NotNull private TestDialogManager myDialogManager;
  private MockGoogleLogin myGoogleLogin;
  private boolean myDialogShown;

  @Override
  protected final void setUp() throws Exception {
    super.setUp();
    myDialogManager = (TestDialogManager) ServiceManager.getService(DialogManager.class);

    myGoogleLogin = new MockGoogleLogin();
    myGoogleLogin.install();

    GoogleLoginState googleLoginState = Mockito.mock(GoogleLoginState.class);
    CredentialedUser user = Mockito.mock(CredentialedUser.class);
    LinkedHashMap<String, CredentialedUser> allusers = new LinkedHashMap();

    when(user.getEmail()).thenReturn(USER);
    when(user.getGoogleLoginState()).thenReturn(googleLoginState);
    when(googleLoginState.fetchAccessToken()).thenReturn(PASSWORD);
    when(GoogleLogin.getInstance().getAllUsers()).thenReturn(allusers);
    allusers.put(USER, user);

    PropertiesComponent.getInstance(ourProject).unsetValue(CACHE_KEY);
    GcpHttpAuthDataProvider.setCurrentProject(ourProject);

    myDialogShown = false;
    myDialogManager.registerDialogHandler(SelectUserDialog.class, new TestDialogHandler<SelectUserDialog>() {
      @Override
      public int handleDialog(SelectUserDialog dialog) {
        dialog.setSelectedUser(USER);
        myDialogShown = true;
        return 0;
      }
    });
  }

  @Override
  protected final void tearDown() throws Exception {
    myGoogleLogin.cleanup();
    PropertiesComponent.getInstance(ourProject).unsetValue(CACHE_KEY);
    GcpHttpAuthDataProvider.setCurrentProject(null);
    myDialogManager.cleanup();
    super.tearDown();
  }

  public void testOnlyForGcp() {
    GcpHttpAuthDataProvider authDataProvider = new GcpHttpAuthDataProvider();
    AuthData result = authDataProvider.getAuthData("http://someotherurl.myurl.com");

    assertFalse(result == null);
  }

  public void testForGcpPrompt() throws Exception {
    GcpHttpAuthDataProvider authDataProvider = new GcpHttpAuthDataProvider();
    AuthData result = authDataProvider.getAuthData(GOOGLE_URL);

    assertTrue(myDialogShown);
    assertTrue(result.getLogin() == USER);
    assertTrue(result.getPassword() == PASSWORD);
    assertTrue(PropertiesComponent.getInstance(ourProject).getValue(CACHE_KEY) == USER);
  }

  public void testForCachedState() throws Exception {
    PropertiesComponent.getInstance(ourProject).setValue(CACHE_KEY, USER);

    GcpHttpAuthDataProvider authDataProvider = new GcpHttpAuthDataProvider();
    AuthData result = authDataProvider.getAuthData(GOOGLE_URL);

    assertTrue(!myDialogShown);
    assertTrue(result.getLogin() == USER);
    assertTrue(result.getPassword() == PASSWORD);
    assertTrue(PropertiesComponent.getInstance(ourProject).getValue(CACHE_KEY) == USER);
  }

  public void testForInvalidCachedState() throws Exception {
    PropertiesComponent.getInstance(ourProject).setValue(CACHE_KEY, "invalidusername");

    GcpHttpAuthDataProvider authDataProvider = new GcpHttpAuthDataProvider();
    AuthData result = authDataProvider.getAuthData(GOOGLE_URL);

    assertTrue(myDialogShown);
    assertTrue(result.getLogin() == USER);
    assertTrue(result.getPassword() == PASSWORD);
    assertTrue(PropertiesComponent.getInstance(ourProject).getValue(CACHE_KEY) == USER);
  }
}
