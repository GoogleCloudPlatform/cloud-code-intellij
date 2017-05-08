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

package com.google.cloud.tools.intellij.debugger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.GoogleLoginListener;
import com.google.cloud.tools.intellij.login.GoogleLoginService;
import com.google.cloud.tools.intellij.testing.BasePluginTestCase;
import com.google.gdt.eclipse.login.common.GoogleLoginState;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashMap;

public class CloudDebugProcessHandlerTest extends BasePluginTestCase {

    private CloudDebugProcessHandler handler;
    private GoogleLoginService mockLoginService = mock(GoogleLoginService.class);

    @Before
    public void setUp() throws Exception {
        registerExtensionPoint(GoogleLoginListener.EP_NAME, GoogleLoginListener.class);
        registerService(GoogleLoginService.class, mockLoginService);

        setupMocks();
    }

    @Test
    public void testIsSilentlyDisposed() {
        Assert.assertTrue(handler.isSilentlyDestroyOnClose());
    }

    @Test
    public void testDetachIsDefault() {
        Assert.assertTrue(handler.detachIsDefault());
    }

    @Test
    public void testGetProcessInput() {
        Assert.assertNull(handler.getProcessInput());
    }

    private void setupMocks() {
        GoogleLoginState googleLoginState = mock(GoogleLoginState.class);
        CredentialedUser credentialedUser = mock(CredentialedUser.class);
        when(credentialedUser.getGoogleLoginState()).thenReturn(googleLoginState);

        LinkedHashMap<String, CredentialedUser> users = new LinkedHashMap<String, CredentialedUser>();
        users.put("mockUser@foo.bar", credentialedUser);
        when(mockLoginService.getAllUsers()).thenReturn(users);

        CloudDebugProcessState cloudDebugProcessState = mock(CloudDebugProcessState.class);
        when(cloudDebugProcessState.getUserEmail()).thenReturn("mockUser@foo.bar");

        CloudDebugProcess cloudDebugProcess = mock(CloudDebugProcess.class);
        when(cloudDebugProcess.getProcessState()).thenReturn(cloudDebugProcessState);

        handler = new CloudDebugProcessHandler(cloudDebugProcess);
    }
}
