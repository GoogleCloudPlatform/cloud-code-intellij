/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.application;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.appengine.v1.Appengine;
import com.google.api.services.appengine.v1.model.Application;
import com.google.api.services.appengine.v1.model.Operation;
import com.google.api.services.appengine.v1.model.Status;
import com.google.cloud.tools.intellij.appengine.application.GoogleApiClientAppEngineAdminService;
import com.google.cloud.tools.intellij.appengine.application.GoogleApiException;
import com.google.cloud.tools.intellij.resources.GoogleApiClientFactory;
import com.google.cloud.tools.intellij.testing.BasePluginTestCase;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link GoogleApiClientAppEngineAdminService}
 */
public class GoogleApiClientAppEngineAdminServiceTest extends BasePluginTestCase {

  @Mock private GoogleApiClientFactory apiClientFactoryMock;

  private AppengineMock appengineClientMock;

  private GoogleApiClientAppEngineAdminService service;

  @Before
  public void setUp() throws IOException {
    registerService(GoogleApiClientFactory.class, apiClientFactoryMock);

    appengineClientMock = new AppengineMock();
    when(apiClientFactoryMock.getAppEngineApiClient(any(HttpRequestInitializer.class)))
        .thenReturn(appengineClientMock);

    service = new GoogleApiClientAppEngineAdminService();
  }

  @Test
  public void testGetApplicationForProjectId_success() throws IOException, GoogleApiException {
    Application result = new Application();
    when(appengineClientMock.getAppsGetQuery().execute()).thenReturn(result);

    String projectId = "my-project";
    assertEquals(result, service.getApplicationForProjectId(projectId, mock(Credential.class)));
    verify(appengineClientMock.apps(), times(1)).get(eq(projectId));
    verify(appengineClientMock.getAppsGetQuery(), times(1)).execute();
  }

  @Test(expected = GoogleApiException.class)
  public void testGetApplicationForProjectId_GoogleJsonExceptoin() throws IOException,
      GoogleApiException {
    when(appengineClientMock.getAppsGetQuery().execute())
        .thenThrow(GoogleJsonResponseException.class);

    service.getApplicationForProjectId("my-project", mock(Credential.class));
  }

  @Test
  public void testCreateApplication() throws IOException, GoogleApiException {
    String operationId = "my-operation-id";
    String operationName = "apps/-/operations/" + operationId;

    Operation inProgressOperation = buildInProgressOperation(operationName);
    when(appengineClientMock.getAppsCreateQuery().execute()).thenReturn(inProgressOperation);

    final String locationId = "us-east1";
    final String projectId = "my-project";

    Map<String, Object> response = new HashMap<>();
    response.put("name", projectId);
    response.put("locationId", locationId);

    // require polling several times
    Operation doneOperation = new Operation();
    doneOperation.setName(operationName);
    doneOperation.setDone(true);
    doneOperation.setResponse(response);
    when(appengineClientMock.getAppsOperationsGetQuery().execute())
        .thenReturn(inProgressOperation)
        .thenReturn(inProgressOperation)
        .thenReturn(doneOperation);

    Application result = service.createApplication(locationId, projectId, mock(Credential.class));

    // ensure the 'getOperation' API call(s) were made correctly
    verify(appengineClientMock.apps().operations(), atLeastOnce())
        .get(eq(projectId), eq(operationId));

    // ensure the 'createApplication' API call was made with the correct args
    verify(appengineClientMock.apps(), times(1))
        .create(
            argThat(
                new ArgumentMatcher<Application>() {
                  @Override
                  public boolean matches(Object argument) {
                    Application application = (Application) argument;
                    return application.getId().equals(projectId)
                        && application.getLocationId().equals(locationId);
                  }
                }));

    // ensure the 'createApplication' API call was only made once
    verify(appengineClientMock.getAppsCreateQuery(), times(1)).execute();

    assertEquals(projectId, result.getName());
    assertEquals(locationId, result.getLocationId());
  }

  private Operation buildInProgressOperation(String operationName) {
    Operation inProgressOperation = new Operation();
    inProgressOperation.setName(operationName);
    inProgressOperation.setDone(false);
    return inProgressOperation;
  }

  @Test
  public void testCreateApplication_operationFailed() throws IOException, GoogleApiException {
    String operationName = "apps/-/operations/12345";
    Operation inProgressOperation = buildInProgressOperation(operationName);
    when(appengineClientMock.getAppsCreateQuery().execute()).thenReturn(inProgressOperation);

    String errorMessage = "The operation failed.";
    int errorCode = 400;
    Status status = new Status();
    status.setMessage(errorMessage);
    status.setCode(errorCode);
    Operation failedOperation = new Operation();
    failedOperation.setError(status);
    failedOperation.setDone(true);
    failedOperation.setName(operationName);

    when(appengineClientMock.getAppsCreateQuery().execute()).thenReturn(inProgressOperation);
    when(appengineClientMock.getAppsOperationsGetQuery().execute()).thenReturn(failedOperation);

    try {
      service.createApplication("us-east", "my-project-id", mock(Credential.class));
    } catch (GoogleApiException expected) {
      assertEquals(errorCode, expected.getStatusCode());
      assertEquals(errorMessage, expected.getMessage());
      return;
    }
    fail();
  }

  @Test(expected = GoogleApiException.class)
  public void testCreateApplication_GoogleJsonException() throws IOException, GoogleApiException {
    when(appengineClientMock.getAppsCreateQuery().execute())
        .thenThrow(GoogleJsonResponseException.class);
    service.createApplication("us-east", "my-project-id", mock(Credential.class));
  }

  /**
   * A mock implementation of {@code Appengine} client lib, to make it easier to mock responses and
   * perform assertions.
   */
  private class AppengineMock extends Appengine {

    @Mock private Appengine.Apps apps;
    @Mock private Appengine.Apps.Get appsGet;
    @Mock private Appengine.Apps.Create appsCreate;
    @Mock private Appengine.Apps.Operations appsOperations;
    @Mock private Appengine.Apps.Operations.Get appsOperationsGet;

    public AppengineMock() {
      super(mock(HttpTransport.class), mock(JsonFactory.class), null);

      MockitoAnnotations.initMocks(this);
      try {
        when(apps.get(anyString())).thenReturn(appsGet);
        when(apps.create(any(Application.class))).thenReturn(appsCreate);
        when(apps.operations()).thenReturn(appsOperations);
        when(appsOperations.get(anyString(), anyString())).thenReturn(appsOperationsGet);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public Appengine.Apps apps() {
      return apps;
    }

    public Apps.Get getAppsGetQuery() {
      return appsGet;
    }
    public Apps.Create getAppsCreateQuery() {
      return appsCreate;
    }
    public Apps.Operations.Get getAppsOperationsGetQuery() {
      return appsOperationsGet;
    }
  }
}
