/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.vcs;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Key;
import com.google.api.client.util.Preconditions;
import com.google.api.services.source.Source;
import com.google.api.services.source.SourceRequest;
import com.google.api.services.source.model.ListReposResponse;
import com.google.cloud.tools.intellij.CloudToolsPluginInfoService;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.intellij.openapi.components.ServiceManager;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

/** Service for interacting with Google Cloud Source Repositories. */
public class CloudRepositoryService {

  private static final String CLOUD_SOURCE_API_ROOT_URL = "https://source.googleapis.com/";
  private static final String CLOUD_SOURCE_API_LIST_URL = "v1/projects/{projectId}/repos";
  private static int LIST_TIMEOUT_MS = 5000;

  @NotNull
  public ListReposResponse list(CredentialedUser user, String cloudProject)
      throws CloudRepositoryServiceException {
    try {
      Credential credential = user.getCredential();
      HttpRequestInitializer initializer =
          httpRequest -> {
            HttpHeaders headers = new HttpHeaders();
            httpRequest.setConnectTimeout(LIST_TIMEOUT_MS);
            httpRequest.setReadTimeout(LIST_TIMEOUT_MS);
            httpRequest.setHeaders(headers);
            credential.initialize(httpRequest);
          };

      HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
      String userAgent =
          ServiceManager.getService(CloudToolsPluginInfoService.class).getUserAgent();

      Source source =
          new Source.Builder(httpTransport, JacksonFactory.getDefaultInstance(), initializer)
              .setRootUrl(CLOUD_SOURCE_API_ROOT_URL)
              .setServicePath("")
              // this ends up prefixed to user agent
              .setApplicationName(userAgent)
              .build();

      return new CustomUrlSourceRequest(source, cloudProject).execute();
    } catch (IOException | GeneralSecurityException ex) {
      throw new CloudRepositoryServiceException();
    }
  }

  public CompletableFuture<ListReposResponse> listAsync(
      CredentialedUser user, String cloudProject) {
    return CompletableFuture.supplyAsync(() -> list(user, cloudProject));
  }

  /**
   * The currently used version of the Source API in {@link
   * com.google.api.services.source.Source.Repos.List} uses an outdated endpoint for listing repos.
   * This extends the base class {@link SourceRequest} to set the correct url.
   */
  public static class CustomUrlSourceRequest extends SourceRequest<ListReposResponse> {

    @Key private String projectId;

    CustomUrlSourceRequest(Source client, String projectId) {
      super(client, "GET", CLOUD_SOURCE_API_LIST_URL, null, ListReposResponse.class);

      this.projectId =
          Preconditions.checkNotNull(projectId, "Required parameter projectId must be specified.");
    }
  }

  public static class CloudRepositoryServiceException extends RuntimeException {}
}
