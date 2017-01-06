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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import com.intellij.openapi.components.ServiceManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * A Service that handles App Engine Administration. This class provides some general caching logic,
 * but delegates to extending classes to performing the actual administrative actions.
 */
public abstract class AppEngineAdminService {

  private static final int APPLICATION_CACHE_MAX_SIZE = 10000;
  private static final int LOCATION_CACHE_MAX_SIZE = 10000;
  private static final String ALL_LOCATIONS_KEY = "ALL_LOCATIONS";

  // Cache of GCP application resources. This assumes that project IDs are globally unique. Null or
  // missing applications are not cached because they cannot be reliably invalidated.
  private final Cache<String, Application> appEngineApplicationCache = CacheBuilder.newBuilder()
      // arbitrary size limit
      .maximumSize(APPLICATION_CACHE_MAX_SIZE)
      // Even though these values should never change, it won't kill us to refresh once per day.
      .expireAfterWrite(1, TimeUnit.DAYS)
      .build();

  // Cache of all available GCP locations. This is expected to change very infrequently.
  private final Cache<String, List<Location>> appEngineLocationCache = CacheBuilder.newBuilder()
      // arbitrary size limit
      .maximumSize(LOCATION_CACHE_MAX_SIZE)
      .expireAfterWrite(1, TimeUnit.DAYS)
      .build();

  public static AppEngineAdminService getInstance() {
    return ServiceManager.getService(AppEngineAdminService.class);
  }

  /**
   * Returns an Application resource associated with the given project ID, or {@code null} if none
   * exists.
   *
   * @param projectId the GCP project ID
   * @param credential the authenticated user Credential
   * @throws GoogleApiException if the desired operation could not be completed
   * @throws IOException if there was a transient error connecting to the API
   */
  @Nullable
  public Application getApplicationForProjectId(@NotNull String projectId,
      @NotNull Credential credential) throws IOException, GoogleApiException {
    try {
      // Load from the cache if it exists, otherwise delegate to implementation classes.
      return appEngineApplicationCache.get(projectId, () ->
          fetchApplicationForProjectId(projectId, credential));

    } catch (ExecutionException e) {
      if (e.getCause() instanceof AppEngineApplicationNotFoundException) {
        return null;
      } else {
        handleExecutionException(e);
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Returns a list of all available App Engine Locations
   *
   * @param credential the authenticated user Credential
   * @throws IOException if there was a transient error connecting to the API
   * @throws GoogleApiException if the desired operation could not be completed
   */
  @NotNull
  public List<Location> getAllAppEngineLocations(Credential credential) throws IOException,
      GoogleApiException {
    try {
      return appEngineLocationCache.get(ALL_LOCATIONS_KEY, () ->
          fetchAllAppEngineLocations(credential));
    } catch (ExecutionException e) {
      handleExecutionException(e);
      throw new RuntimeException(e);
    }
  }

  /*
   * Unpacks exceptions thrown by the supplier to propagate correct types to the user.
   */
  private void handleExecutionException(ExecutionException e)
      throws GoogleApiException, IOException {
    Throwable cause = e.getCause();
    if (cause instanceof GoogleApiException) {
      throw (GoogleApiException) cause;
    } else if (cause instanceof IOException) {
      throw (IOException) cause;
    }
  }

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

  /**
   * Fetches a list of all app engine Locations that exist.
   *
   * @param credential the authenticated user Credential
   * @throws IOException if there was a transient error connecting to the API
   * @throws GoogleApiException if the desired operation could not be completed
   */
  protected abstract List<Location> fetchAllAppEngineLocations(Credential credential)
      throws GoogleApiException, IOException;

  /**
   * Fetches an Application. Should throw an AppEngineApplicationNotFoundException if the requested
   * application does not exist.
   *
   * @param projectId the GCP project ID
   * @param credential the authenticated user Credential
   * @throws IOException if there was a transient error connecting to the API
   * @throws GoogleApiException if the desired operation could not be completed
   * @throws AppEngineApplicationNotFoundException if the no application exists in the given project
   */
  @NotNull
  protected abstract Application fetchApplicationForProjectId(@NotNull String projectId,
      @NotNull Credential credential)
      throws IOException, GoogleApiException, AppEngineApplicationNotFoundException;

  /**
   * Exception type to mark a failed attempt to fetch an Application.
   */
  @VisibleForTesting
  static class AppEngineApplicationNotFoundException extends Exception {}

}
