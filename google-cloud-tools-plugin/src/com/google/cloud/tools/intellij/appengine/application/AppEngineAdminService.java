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
import com.google.api.services.appengine.v1.model.Application;
import com.google.api.services.appengine.v1.model.Location;

import com.intellij.openapi.components.ServiceManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * A service that handles App Engine Administration. This class provides some general caching logic,
 * but delegates to extending classes to performing the actual administrative actions.
 */
public abstract class AppEngineAdminService {

  public static AppEngineAdminService getInstance() {
    return ServiceManager.getService(AppEngineAdminService.class);
  }

  /**
   * Returns an Application resource associated with the given project ID, or {@code null} if none
   * exists.
   *
   * @param projectId the GCP project ID
   * @param credential the authenticated user credential
   * @throws GoogleApiException if the desired operation could not be completed
   * @throws IOException if there was a transient error connecting to the API
   */
  @Nullable
  public abstract Application getApplicationForProjectId(@NotNull String projectId,
      @NotNull Credential credential) throws IOException, GoogleApiException;

  /**
   * Returns a list of all available App Engine Locations
   *
   * @param credential the authenticated user Credential
   * @throws IOException if there was a transient error connecting to the API
   * @throws GoogleApiException if the desired operation could not be completed
   */
  @NotNull
  public abstract List<Location> getAllAppEngineLocations(Credential credential) throws IOException,
      GoogleApiException;

  /**
   * Creates an Application for the given project in the given location. This is a long-running
   * operation that can typically take up to one minute.
   *
   * @param locationId The GCP Location in which the application will be located
   * @param projectId the GCP project ID
   * @param credential the authenticated user Credential
   * @throws IOException if there was a transient error connecting to the API
   * @throws GoogleApiException if the desired operation could not be completed
   */
  @NotNull
  public abstract Application createApplication(@NotNull String locationId,
      @NotNull final String projectId, @NotNull final Credential credential)
      throws IOException, GoogleApiException;

}
