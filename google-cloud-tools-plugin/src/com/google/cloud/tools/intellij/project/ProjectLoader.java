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

package com.google.cloud.tools.intellij.project;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.model.ListProjectsResponse;
import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.resources.GoogleApiClientFactory;
import com.google.cloud.tools.intellij.util.ThreadUtil;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Loads list of {@link Project} for a {@link CredentialedUser}. */
class ProjectLoader {
  private static final int PROJECTS_MAX_PAGE_SIZE = 300;
  private static final String PROJECT_DELETE_REQUESTED = "DELETE_REQUESTED";

  /**
   * Use {@link com.google.common.util.concurrent.Futures#addCallback(ListenableFuture,
   * FutureCallback)} to receive notifications of load completion and result or exceptions.
   */
  ListenableFuture<List<Project>> loadUserProjectsInBackground(CredentialedUser user) {
    return ThreadUtil.getInstance().executeInBackground(() -> loadUserProjects(user));
  }

  private List<Project> loadUserProjects(CredentialedUser user) throws IOException {
    CloudResourceManager cloudResourceManagerClient =
        GoogleApiClientFactory.getInstance().getCloudResourceManagerClient(user.getCredential());
    final List<Project> result = new ArrayList<>();

    ListProjectsResponse response =
        cloudResourceManagerClient.projects().list().setPageSize(PROJECTS_MAX_PAGE_SIZE).execute();

    if (response != null && response.getProjects() != null) {
      // Create a sorted set to sort the projects list by project name.
      Set<Project> allProjects =
          new TreeSet<>(Comparator.comparing(project -> project.getName().toLowerCase()));

      allProjects.addAll(response.getProjects());

      while (!Strings.isNullOrEmpty(response.getNextPageToken())) {
        response =
            cloudResourceManagerClient
                .projects()
                .list()
                .setPageToken(response.getNextPageToken())
                .setPageSize(PROJECTS_MAX_PAGE_SIZE)
                .execute();
        allProjects.addAll(response.getProjects());
      }

      allProjects
          .stream()
          // Filter out any projects that are scheduled for deletion.
          .filter((project) -> !PROJECT_DELETE_REQUESTED.equals(project.getLifecycleState()))
          .filter((project) -> !Strings.isNullOrEmpty(project.getProjectId()))
          // Add remaining projects to the set.
          .forEach(result::add);
    }

    return result;
  }
}
