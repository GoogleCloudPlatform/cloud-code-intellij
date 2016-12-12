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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.appengine.v1.model.Application;
import com.google.api.services.appengine.v1.model.ListLocationsResponse;
import com.google.api.services.appengine.v1.model.Location;
import com.google.api.services.appengine.v1.model.Operation;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineOperationFailedException;
import com.google.cloud.tools.intellij.resources.GoogleApiClientFactory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

public class DefaultAppEngineAdminService extends AppEngineAdminService {

  private final static String APP_ENGINE_RESOURCE_WILDCARD = "-";
  private final long CREATE_APPLICATION_POLLING_INTERVAL_MS = 1000;

  @Override
  @Nullable
  public Application getApplicationForProjectId(@NotNull String projectId,
      @NotNull Credential credential) throws IOException {
    try {
      return GoogleApiClientFactory.getInstance().getAppEngineApiClient(credential)
          .apps().get(projectId).execute();
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == 404) {
        // the application does not exist
        return null;
      }
      throw e;
    }
  }

  @Override
  public Application createApplication(@NotNull String locationId, @NotNull final String projectId,
      @NotNull final Credential credential) throws IOException, AppEngineOperationFailedException {

    Application arg = new Application();
    arg.setId(projectId);
    arg.setLocationId(locationId);
    Operation operation = GoogleApiClientFactory.getInstance().getAppEngineApiClient(credential)
        .apps().create(arg).execute();

    boolean done = false;
    while (!done) {
      try {
        Thread.sleep(CREATE_APPLICATION_POLLING_INTERVAL_MS);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      operation = getOperation(projectId, operation.getName(), credential);
      if (operation.getDone() != null) {
        done = operation.getDone();
      }
    }

    if (operation.getError() != null) {
      throw new AppEngineOperationFailedException(operation.getError());
    } else {
      Application result = new Application();
      result.putAll(operation.getResponse());
      return result;
    }
  }

  private Operation getOperation(@NotNull String projectId, @NotNull String operationName,
      @NotNull Credential credential) throws IOException {
    // The operation ID is the final slash-separated component of the operation name.
    String[] nameParts = operationName.split("/");
    if (nameParts.length < 1) {
      throw new IllegalArgumentException("Operation name " + operationName + " is malformatted");
    }
    String id = nameParts[nameParts.length - 1];

    return GoogleApiClientFactory.getInstance().getAppEngineApiClient(credential).apps()
        .operations().get(projectId, id).execute();
  }

  @Override
  public List<Location> getAllAppEngineLocations(Credential credential) throws IOException {
    ListLocationsResponse response = getAppEngineRegions(credential, null);
    List<Location> locations = response.getLocations();

    while (response.getNextPageToken() != null) {
      response = getAppEngineRegions(credential, response.getNextPageToken());
      locations.addAll(response.getLocations());
    }

    return locations;
  }

  private ListLocationsResponse getAppEngineRegions(Credential credential, @Nullable String
      pageToken) throws IOException {
     return GoogleApiClientFactory.getInstance().getAppEngineApiClient(credential).apps().locations()
         .list(APP_ENGINE_RESOURCE_WILDCARD)
         .setPageToken(pageToken)
         .execute();
  }

}
