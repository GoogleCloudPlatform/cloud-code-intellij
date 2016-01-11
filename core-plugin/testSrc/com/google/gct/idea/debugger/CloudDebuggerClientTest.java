package com.google.gct.idea.debugger;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.services.clouddebugger.Clouddebugger.Debugger;
import com.google.gct.login.CredentialedUser;
import com.google.gct.login.GoogleLogin;
import com.google.gdt.eclipse.login.common.GoogleLoginState;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.LinkedHashMap;

@RunWith(MockitoJUnitRunner.class)
public class CloudDebuggerClientTest {

  @Before
  public void setUp() {
    GoogleLogin mockLogin = Mockito.mock(GoogleLogin.class);
    GoogleLogin.setInstance(mockLogin);

    LinkedHashMap<String, CredentialedUser> allUsers = new LinkedHashMap<String, CredentialedUser>();
    CredentialedUser user = Mockito.mock(CredentialedUser.class);
    allUsers.put("foo@example.com", user);
    Mockito.when(mockLogin.getAllUsers()).thenReturn(allUsers);
    Credential credential = Mockito.mock(Credential.class);
    Mockito.when(user.getCredential()).thenReturn(credential);
    GoogleLoginState loginState = Mockito.mock(GoogleLoginState.class);
    Mockito.when(user.getGoogleLoginState()).thenReturn(loginState);
  }

  @After
  public void tearDown() {
    GoogleLogin.setInstance(null);
  }

  @Test
  public void testUserAgent() throws IOException {
    Debugger client = CloudDebuggerClient.getLongTimeoutClient("foo@example.com");
    HttpRequest httpRequest = client.debuggees().list().buildHttpRequestUsingHead();
    HttpHeaders headers = httpRequest.getHeaders();
    String userAgent = headers.getUserAgent();
    Assert.assertTrue(userAgent.startsWith("GCP-Plugin-for-IntelliJ/0.91"));
    Assert.assertTrue(userAgent.endsWith("Google-API-Java-Client"));
    Assert.assertTrue(userAgent.contains("IntelliJ IDEA Community Edition/"));
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
