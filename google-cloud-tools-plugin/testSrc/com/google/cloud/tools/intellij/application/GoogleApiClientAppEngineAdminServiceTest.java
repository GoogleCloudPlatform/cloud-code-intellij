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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.appengine.v1.Appengine;
import com.google.api.services.appengine.v1.model.Application;
import com.google.api.services.appengine.v1.model.Operation;
import com.google.cloud.tools.intellij.appengine.application.GoogleApiClientAppEngineAdminService;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineOperationFailedException;
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
  public void testGetApplicationForProjectId_success() throws IOException {
    Application result = new Application();
    when(appengineClientMock.getAppsGetQuery().execute()).thenReturn(result);

    String projectId = "my-project";
    assertEquals(result, service.getApplicationForProjectId(projectId, mock(Credential.class)));
    verify(appengineClientMock.apps(), times(1)).get(eq(projectId));
  }

  @Test
  public void testCreateApplication() throws IOException, AppEngineOperationFailedException {
    String operationId = "my-operation-id";
    String operationName = "apps/-/operations/" + operationId;

    Operation inProgressOperation = new Operation();
    inProgressOperation.setName(operationName);
    inProgressOperation.setDone(false);
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

    // ensure the service call was made with the correct args
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

    assertEquals(projectId, result.getName());
    assertEquals(locationId, result.getLocationId());
  }

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
