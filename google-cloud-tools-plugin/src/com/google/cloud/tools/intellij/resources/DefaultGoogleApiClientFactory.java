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
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.appengine.v1.Appengine;
import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.iam.v1.Iam;
import com.google.api.services.servicemanagement.ServiceManagement;
import com.google.auth.oauth2.UserCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.service.PluginInfoService;
import com.google.gdt.eclipse.login.common.GoogleLoginState;
import com.intellij.openapi.components.ServiceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultGoogleApiClientFactory extends GoogleApiClientFactory {

  private static final HttpTransport httpTransport = new NetHttpTransport();
  private static final JsonFactory jsonFactory = new JacksonFactory();

  @Override
  public CloudResourceManager getCloudResourceManagerClient(
      @Nullable HttpRequestInitializer httpRequestInitializer) {
    return new CloudResourceManager.Builder(httpTransport, jsonFactory, httpRequestInitializer)
        .setApplicationName(getApplicationName())
        .build();
  }

  @Override
  public ServiceManagement getServiceManagementClient(
      @Nullable HttpRequestInitializer httpRequestInitializer) {
    return new ServiceManagement.Builder(httpTransport, jsonFactory, httpRequestInitializer)
        .setApplicationName(getApplicationName())
        .build();
  }

  @Override
  public Iam getIamClient(@Nullable HttpRequestInitializer httpRequestInitializer) {
    return new Iam.Builder(httpTransport, jsonFactory, httpRequestInitializer)
        .setApplicationName(getApplicationName())
        .build();
  }

  @Override
  public Appengine getAppEngineApiClient(@Nullable HttpRequestInitializer httpRequestInitializer) {
    return new Appengine.Builder(httpTransport, jsonFactory, httpRequestInitializer)
        .setApplicationName(getApplicationName())
        .build();
  }

  @Override
  public Storage getCloudStorageApiClient(
      @NotNull String projectId, @NotNull CredentialedUser credentialedUser) {
    GoogleLoginState loginState = credentialedUser.getGoogleLoginState();
    String clientId = loginState.fetchOAuth2ClientId();
    String clientSecret = loginState.fetchOAuth2ClientSecret();
    String refreshToken = loginState.fetchOAuth2RefreshToken();

    return StorageOptions.newBuilder()
        .setProjectId(projectId)
        .setCredentials(new UserCredentials(clientId, clientSecret, refreshToken))
        .build()
        .getService();
  }

  private String getApplicationName() {
    return ServiceManager.getService(PluginInfoService.class).getUserAgent();
  }
}
