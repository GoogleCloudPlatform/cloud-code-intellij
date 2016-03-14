package com.google.gct.idea.debugger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gct.idea.testing.BasePluginTestCase;
import com.google.gct.login.CredentialedUser;
import com.google.gct.login.GoogleLoginListener;
import com.google.gct.login.GoogleLoginService;
import com.google.gdt.eclipse.login.common.GoogleLoginState;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.testFramework.LoggedErrorProcessor;
import com.intellij.testFramework.TestLoggerFactory;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.LinkedHashMap;

public class CloudDebugProcessHandlerTest extends BasePluginTestCase {

    private CloudDebugProcessHandler handler = new CloudDebugProcessHandler(null);
    private GoogleLoginService mockLoginService = mock(GoogleLoginService.class);

    @Before
    public void setUp() throws Exception {
        registerExtensionPoint(GoogleLoginListener.EP_NAME, GoogleLoginListener.class);
        registerService(GoogleLoginService.class, mockLoginService);
    }

    @BeforeClass
    public static void setUpClass() {
        LoggedErrorProcessor.setNewInstance(mock(LoggedErrorProcessor.class));
        Logger.setFactory(TestLoggerFactory.class);
    }

    @AfterClass
    public static void tearDownClass() {
        LoggedErrorProcessor.restoreDefaultProcessor();
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

    @Test
    public void testConstructorAddsLoginListenerIfUserFound() {
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

        new CloudDebugProcessHandler(cloudDebugProcess);
    }
}
