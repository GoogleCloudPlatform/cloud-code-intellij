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

package com.google.cloud.tools.intellij.resources;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.appengine.v1.Appengine;
import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.cloud.storage.Storage;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.intellij.openapi.components.ServiceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Factory class for creating instances of Google API clients. */
public abstract class GoogleApiClientFactory {

  public static GoogleApiClientFactory getInstance() {
    return ServiceManager.getService(GoogleApiClientFactory.class);
  }

  /**
   * Creates a new instance of a {@link CloudResourceManager} client
   *
   * @param httpRequestInitializer optional HttpRequestInitializer
   */
  public abstract CloudResourceManager getCloudResourceManagerClient(
      @Nullable HttpRequestInitializer httpRequestInitializer);

  /**
   * Creates a new instance of a {@link Appengine} client
   *
   * @param httpRequestInitializer optional HttpRequestInitializer
   */
  public abstract Appengine getAppEngineApiClient(
      @Nullable HttpRequestInitializer httpRequestInitializer);

  /**
   * Creates a new instance of {@link Storage} client.
   *
   * @param projectId id of the cloud project
   * @param credentialedUser credentialed user to use for authentication
   */
  public abstract Storage getCloudStorageApiClient(
      @NotNull String projectId, @NotNull CredentialedUser credentialedUser);
}
