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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.source.Source;
import com.google.api.services.source.SourceRequest;
import com.google.api.services.source.model.ListReposResponse;
import com.google.api.services.source.model.Repo;
import com.google.cloud.tools.intellij.CloudToolsPluginInfoService;
import com.google.cloud.tools.intellij.login.CredentialedUser;

import com.intellij.openapi.components.ServiceManager;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * TreeNode representation of the set of available Cloud Source Repositories for a given GCP
 * project.
 */
public class ProjectRepositoriesModelItem extends DefaultMutableTreeNode {

  private String cloudProject;
  private CredentialedUser user;

  // TODO user can be null on first instantiation
  public ProjectRepositoriesModelItem(@NotNull String cloudProject,
      @NotNull CredentialedUser user) {
    this.cloudProject = cloudProject;
    this.user = user;

    setUserObject(cloudProject);
  }

  public void loadRepositories() {
    loadRepositories(false);
  }
  public void loadRepositories(boolean empty) {

    // TODO remove
//      if (empty) {
//        removeAllChildren();
////        add (new ResourceErrorModelItem("Error loading repositories."));
//        add (new ResourceEmptyModelItem("There are no cloud repositories for this project"));
//        return;
//      }

    // todo remove this
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }


    try {
      Credential credential = user.getCredential();
      HttpRequestInitializer initializer = httpRequest -> {
        HttpHeaders headers = new HttpHeaders();
        httpRequest.setConnectTimeout(5000);
        httpRequest.setReadTimeout(5000);
        httpRequest.setHeaders(headers);
        credential.initialize(httpRequest);
      };

      HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
      String userAgent = ServiceManager
          .getService(CloudToolsPluginInfoService.class).getUserAgent();

      Source source = new Source.Builder(httpTransport, JacksonFactory.getDefaultInstance(),
          initializer)
          .setRootUrl("https://source.googleapis.com/")
          .setServicePath("")
          // this ends up prefixed to user agent
          .setApplicationName(userAgent)
          .build();

      ListReposResponse response = new CustomUrlSourceRequest(source, cloudProject).execute();

      removeAllChildren();
      List<Repo> repositories = response.getRepos();
      if (!response.isEmpty() && repositories != null) {
        for (Repo repo : repositories) {
          add(new RepositoryModelItem(repo.get("name").toString())); // TODO null check?
        }
      } else {
        add (new ResourceEmptyModelItem("There are no cloud repositories for this project"));
      }
    } catch (IOException | GeneralSecurityException ex) {
      removeAllChildren();
      add (new ResourceErrorModelItem("Error loading repositories."));
    }
  }

  /**
   * The currently used version of the Source API in
   * {@link com.google.api.services.source.Source.Repos.List} uses an outdated endpoint for listing
   * repos. This extends the base class {@link SourceRequest} to set the correct url.
   */
  public static class CustomUrlSourceRequest extends SourceRequest<ListReposResponse> {

    CustomUrlSourceRequest(Source client, String projectId) {
      super(client, "GET", "v1/projects/" + projectId +"/repos", null,
          ListReposResponse.class);
    }
  }
}
