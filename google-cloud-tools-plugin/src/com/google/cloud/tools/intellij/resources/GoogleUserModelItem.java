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

package com.google.cloud.tools.intellij.resources;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.model.ListProjectsResponse;
import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.IntellijGoogleLoginService;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;

import org.jetbrains.annotations.NotNull;

import java.awt.Image;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * This model item represents a {@link IntellijGoogleLoginService} credentialed user in the treeview
 * of the project selector.
 */
class GoogleUserModelItem extends DefaultMutableTreeNode {

  private static final Logger LOG = Logger.getInstance(GoogleUserModelItem.class);
  private static final int PROJECTS_MAX_PAGE_SIZE = 300;
  private static final String PROJECT_DELETE_REQUESTED = "DELETE_REQUESTED";

  private final CredentialedUser user;
  private final DefaultTreeModel treeModel;
  private volatile boolean isSynchronizing;
  private volatile boolean needsSynchronizing;
  private CloudResourceManager cloudResourceManagerClient;

  GoogleUserModelItem(@NotNull CredentialedUser user, @NotNull DefaultTreeModel treeModel) {
    this.user = user;
    this.treeModel = treeModel;
    setNeedsSynchronizing();

    cloudResourceManagerClient
        = GoogleApiClientFactory.getInstance().getCloudResourceManagerClient(user.getCredential());
  }

  public CredentialedUser getCredentialedUser() {
    return user;
  }

  public Image getImage() {
    return user.getPicture();
  }

  public String getName() {
    return user.getName();
  }

  public String getEmail() {
    return user.getEmail();
  }

  // This method "dirties" the node, indicating that it needs another call to resource manager to
  // get its projects. The call may not happen immediately if the google login is collapsed in the
  // tree view.
  public void setNeedsSynchronizing() {
    needsSynchronizing = true;

    removeAllChildren();
    add(new ResourceLoadingModelItem());
    treeModel.reload(this);
  }

  /*
   * This method kicks off synchronization of this user asynchronously.
   * If synchronization is already in progress, this call is ignored.
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  public void synchronize() {
    if (!needsSynchronizing || isSynchronizing) {
      return;
    }
    isSynchronizing = true;

    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      try {
        loadUserProjects();
        needsSynchronizing = false;
      } finally {
        isSynchronizing = false;
      }
    });
  }

  public boolean isSynchronizing() {
    return isSynchronizing;
  }

  // If an error occurs during the resource manager call, we load a model that shows the error.
  private void loadErrorState(@NotNull final String errorMessage) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        GoogleUserModelItem.this.removeAllChildren();
        GoogleUserModelItem.this.add(new ResourceErrorModelItem("Error: " + errorMessage));
        treeModel.reload(GoogleUserModelItem.this);
      }
    });
  }

  private void loadUserProjects() {
    final List<DefaultMutableTreeNode> result = new ArrayList<>();

    try {

      ListProjectsResponse response = cloudResourceManagerClient.projects().list()
          .setPageSize(PROJECTS_MAX_PAGE_SIZE).execute();

      if (response != null && response.getProjects() != null) {
        // Create a sorted set to sort the projects list by project ID.
        Set<Project> allProjects = new TreeSet<>((Project p1, Project p2) ->
            p1.getName().toLowerCase().compareTo(p2.getName().toLowerCase()));

        response.getProjects().stream()
            // Filter out any projects that are scheduled for deletion.
            .filter((project) -> !PROJECT_DELETE_REQUESTED.equals(project.getLifecycleState()))
            // Add remaining projects to the set.
            .forEach(allProjects::add);

        while (!Strings.isNullOrEmpty(response.getNextPageToken())) {
          response = cloudResourceManagerClient.projects().list()
              .setPageToken(response.getNextPageToken())
              .setPageSize(PROJECTS_MAX_PAGE_SIZE)
              .execute();
          allProjects.addAll(response.getProjects());
        }
        for (Project pantheonProject : allProjects) {
          if (!Strings.isNullOrEmpty(pantheonProject.getProjectId())) {
            result.add(new ResourceProjectModelItem(pantheonProject));
          }
        }
      }
    } catch (IOException ex) {
      // https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/323
      loadErrorState(GctBundle.getString("clouddebug.couldnotconnect"));
      return;
    } catch (RuntimeException ex) {
      LOG.error("Exception loading projects for " + user.getName(), ex);
      loadErrorState(ex.getMessage());
      return;
    }

    result.add(new ResourceNewProjectModelItem());

    try {
      // We invoke back to the UI thread to update the model and treeview.
      SwingUtilities.invokeAndWait(new Runnable() {
        @Override
        public void run() {
          GoogleUserModelItem.this.removeAllChildren();

          for (DefaultMutableTreeNode item : result) {
            GoogleUserModelItem.this.add(item);
          }

          treeModel.reload(GoogleUserModelItem.this);
        }
      });
    } catch (InterruptedException ex) {
      LOG.error("InterruptedException loading projects for " + user.getName(), ex);
      loadErrorState(ex.getMessage());
      Thread.currentThread().interrupt();
    } catch (InvocationTargetException ex) {
      LOG.error("InvocationTargetException loading projects for " + user.getName(), ex);
      loadErrorState(ex.getMessage());
    }
  }
}
