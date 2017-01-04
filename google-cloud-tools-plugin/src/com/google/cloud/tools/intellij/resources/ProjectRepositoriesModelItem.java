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
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.source.Source;
import com.google.api.services.source.model.ListReposResponse;
import com.google.api.services.source.model.Repo;
import com.google.cloud.tools.intellij.CloudToolsPluginInfoService;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.vcs.UploadSourceDialog.MySourceList;

import com.intellij.openapi.components.ServiceManager;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Created by eshaul on 12/19/16.
 */
public class ProjectRepositoriesModelItem extends DefaultMutableTreeNode {

  private String cloudProject;
  private CredentialedUser user;

  // TODO user can be null on first instantiation
  public ProjectRepositoriesModelItem(@NotNull String cloudProject, @NotNull CredentialedUser user) {
//    removeAllChildren();
    this.cloudProject = cloudProject;
    this.user = user;

    setUserObject(cloudProject); // sets the text
    loadRepositories();
  }

  private void loadRepositories() {
    // TODO load from API
    // TODO UI thread?

    try {
      final Credential credential = (user != null ? user.getCredential() : null);
      HttpRequestInitializer initializer = new HttpRequestInitializer() {
        @Override
        public void initialize(HttpRequest httpRequest) throws IOException {
          HttpHeaders headers = new HttpHeaders();
          httpRequest.setConnectTimeout(5000);
          httpRequest.setReadTimeout(5000);
          httpRequest.setHeaders(headers);
          credential.initialize(httpRequest);
        }
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

      MySourceList sourceList = new MySourceList(source, cloudProject);

      ListReposResponse response = sourceList.execute();
      for(Repo repo : response.getRepos()) {
        add(new RepositoryModelItem(repo.get("name").toString())); // TODO null check?
      }

//      Repo newRepo = new Repo();

//      newRepo.setName("from-ij");
//      source.projects().repos().create("test-metrics", newRepo).execute();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    } catch (GeneralSecurityException gse) {
      gse.printStackTrace();
    }
  }
}
