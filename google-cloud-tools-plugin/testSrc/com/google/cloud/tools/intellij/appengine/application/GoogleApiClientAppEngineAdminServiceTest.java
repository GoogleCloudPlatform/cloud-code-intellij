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

package com.google.cloud.tools.intellij.appengine.application;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.appengine.v1.Appengine;
import com.google.api.services.appengine.v1.model.Application;
import com.google.api.services.appengine.v1.model.ListLocationsResponse;
import com.google.api.services.appengine.v1.model.Location;
import com.google.api.services.appengine.v1.model.Operation;
import com.google.api.services.appengine.v1.model.Status;
import com.google.cloud.tools.intellij.appengine.application.GoogleApiClientAppEngineAdminService.AppEngineApplicationNotFoundException;
import com.google.cloud.tools.intellij.resources.GoogleApiClientFactory;
import com.google.cloud.tools.intellij.testing.BasePluginTestCase;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


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

    service = spy(new GoogleApiClientAppEngineAdminService());
  }

  @Test
  public void testGetApplicationForProjectId_success() throws IOException, GoogleApiException {
    Application result = new Application();
    when(appengineClientMock.getAppsGetQuery().execute()).thenReturn(result);

    String projectId = "my-project";
    assertEquals(result, service.getApplicationForProjectId(projectId, mock(Credential.class)));
    verify(appengineClientMock.apps()).get(eq(projectId));
    verify(appengineClientMock.getAppsGetQuery()).execute();

    // make the call again, and assert that the cached result is returned without calling the API
    service.getApplicationForProjectId(projectId, mock(Credential.class));
    verifyNoMoreInteractions(appengineClientMock.apps());
    verifyNoMoreInteractions(appengineClientMock.getAppsGetQuery());
  }

  @Test(expected = GoogleApiException.class)
  public void testGetApplicationForProjectId_GoogleJsonException() throws IOException,
      GoogleApiException {
    when(appengineClientMock.getAppsGetQuery().execute())
        .thenThrow(GoogleJsonResponseException.class);

    service.getApplicationForProjectId("my-project", mock(Credential.class));
  }

  @Test
  public void testGetApplicationForProjectId_empty()
      throws Exception {
    String projectId = "some-id";
    doThrow(AppEngineApplicationNotFoundException.class).when(service)
        .fetchApplicationForProjectId(eq(projectId), any(Credential.class));

    // call the method twice, then assert that the result was not cached
    int calls = 2;
    for (int i = 0; i < calls; i++) {
      assertNull(service.getApplicationForProjectId(projectId, mock(Credential.class)));
    }
    verify(service, times(calls))
        .fetchApplicationForProjectId(eq(projectId), any(Credential.class));
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

    Operation doneOperation = new Operation();
    doneOperation.setName(operationName);
    doneOperation.setDone(true);
    doneOperation.setResponse(response);
    // require polling several times
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
                application ->
                    application.getId().equals(projectId)
                        && application.getLocationId().equals(locationId)));

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

  @Test
  public void testGetAllAppEngineLocations_success() throws IOException, GoogleApiException {
    String pageToken = "page-token";
    ListLocationsResponse response1 = new ListLocationsResponse();
    List<Location> locationsPage1 = Arrays.asList(
        createMockLocation("location-1"), createMockLocation("location-2"));
    response1.setLocations(locationsPage1);
    response1.setNextPageToken(pageToken);

    List<Location> locationsPage2 = Arrays.asList(
        createMockLocation("location-3"), createMockLocation("location-4"));
    ListLocationsResponse response2 = new ListLocationsResponse();
    response2.setLocations(locationsPage2);
    response2.setNextPageToken(null);

    when(appengineClientMock.getAppsLocationsListQuery().setPageToken(any()))
        .thenReturn(appengineClientMock.getAppsLocationsListQuery());
    when(appengineClientMock.getAppsLocationsListQuery().execute()).thenReturn(response1);

    Appengine.Apps.Locations.List appsLocationsListQuery2
        = mock(Appengine.Apps.Locations.List.class);
    when(appengineClientMock.getAppsLocationsListQuery().setPageToken(eq(pageToken)))
        .thenReturn(appsLocationsListQuery2);
    when(appsLocationsListQuery2.execute()).thenReturn(response2);

    List<Location> expectedResults = new ArrayList<>(locationsPage1);
    expectedResults.addAll(locationsPage2);

    // make the call twice. the service should only be hit once per page
    assertEquals(expectedResults, service.getAllAppEngineLocations(mock(Credential.class)));
    assertEquals(expectedResults, service.getAllAppEngineLocations(mock(Credential.class)));
    verify(appengineClientMock.getAppsLocationsListQuery(), times(1)).execute();
    verify(appsLocationsListQuery2, times(1)).execute();
  }

  private Location createMockLocation(String id) {
    Location location = new Location();
    location.setLocationId(id);
    return location;
  }

  /**
   * A mock implementation of {@code Appengine} client lib, to make it easier to mock responses and
   * perform assertions.
   */
  private static class AppengineMock extends Appengine {

    @Mock private Appengine.Apps apps;
    @Mock private Appengine.Apps.Get appsGet;
    @Mock private Appengine.Apps.Create appsCreate;
    @Mock private Appengine.Apps.Operations appsOperations;
    @Mock private Appengine.Apps.Operations.Get appsOperationsGet;
    @Mock private Appengine.Apps.Locations appsLocations;
    @Mock private Appengine.Apps.Locations.List appsLocationsList;

    public AppengineMock() {
      super(mock(HttpTransport.class), mock(JsonFactory.class), null);

      MockitoAnnotations.initMocks(this);
      try {
        when(apps.get(anyString())).thenReturn(appsGet);
        when(apps.create(any(Application.class))).thenReturn(appsCreate);
        when(apps.operations()).thenReturn(appsOperations);
        when(apps.locations()).thenReturn(appsLocations);
        when(appsOperations.get(anyString(), anyString())).thenReturn(appsOperationsGet);
        when(appsLocations.list(anyString())).thenReturn(appsLocationsList);
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
    public Apps.Locations.List getAppsLocationsListQuery() {
      return appsLocationsList;
    }
  }
}
