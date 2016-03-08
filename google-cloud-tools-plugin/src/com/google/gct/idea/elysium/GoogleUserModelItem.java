/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.gct.idea.elysium;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.developerprojects.Developerprojects;
import com.google.api.services.developerprojects.model.ListProjectsResponse;
import com.google.api.services.developerprojects.model.Project;
import com.google.gct.idea.CloudToolsPluginInfoService;
import com.google.gct.idea.util.GctBundle;
import com.google.gct.login.CredentialedUser;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.jetbrains.annotations.NotNull;

import java.awt.Image;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * This model item represents a {@link com.google.gct.login.GoogleLogin} credentialed user
 * in the treeview of the project selector.
 */
class GoogleUserModelItem extends DefaultMutableTreeNode {
  private static final Logger LOG = Logger.getInstance(GoogleUserModelItem.class);
  private static final int PROJECTS_MAX_PAGE_SIZE = 300;

  private final CredentialedUser myUser;
  private final DefaultTreeModel myTreeModel;
  private volatile boolean myIsSynchronizing;
  private volatile boolean myNeedsSynchronizing;
  private Developerprojects developerProjectsClient;

  GoogleUserModelItem(@NotNull CredentialedUser user, @NotNull DefaultTreeModel treeModel) {
    myUser = user;
    myTreeModel = treeModel;
    setNeedsSynchronizing();
    developerProjectsClient = new Developerprojects.Builder(
        new NetHttpTransport(), new JacksonFactory(), myUser.getCredential())
        .setApplicationName(
            ServiceManager.getService(CloudToolsPluginInfoService.class).getUserAgent())
        .build();
  }

  public CredentialedUser getCredentialedUser() {
    return myUser;
  }

  public Image getImage() {
    return myUser.getPicture();
  }

  public String getName() {
    return myUser.getName();
  }

  public String getEmail() {
    return myUser.getEmail();
  }

  // This method "dirties" the node, indicating that it needs another call to elysium to get its projects.
  // The call may not happen immediately if the google login is collapsed in the tree view.
  public void setNeedsSynchronizing() {
    myNeedsSynchronizing = true;

    removeAllChildren();
    add(new ElysiumLoadingModelItem());
    myTreeModel.reload(this);
  }

  /*
   * This method kicks off synchronization of this user asynchronously.
   * If synchronization is already in progress, this call is ignored.
   */
  public void synchronize() {
    if (!myNeedsSynchronizing || myIsSynchronizing) {
      return;
    }
    myIsSynchronizing = true;

    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
      @Override
      public void run() {
        try {
          loadUserProjects();
          myNeedsSynchronizing = false;
        }
        finally {
          myIsSynchronizing = false;
        }
      }
    });
  }

  public boolean isSynchronizing() {
    return myIsSynchronizing;
  }

  // If an error occurs during the elysium call, we load a model that shows the error.
  private void loadErrorState(@NotNull final String errorMessage) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      @SuppressFBWarnings(value="DM_STRING_CTOR", justification="Warning due to string concatenation")
      public void run() {
        GoogleUserModelItem.this.removeAllChildren();
        GoogleUserModelItem.this.add(new ElysiumErrorModelItem("Error: " + errorMessage));
        myTreeModel.reload(GoogleUserModelItem.this);
      }
    });
  }

  @SuppressFBWarnings(value="DM_STRING_CTOR", justification="Warning due to string concatenation")
  private void loadUserProjects() {
    final List<DefaultMutableTreeNode> result = new ArrayList<DefaultMutableTreeNode>();

    try {
      ListProjectsResponse response = developerProjectsClient.projects().list()
          .setPageSize(PROJECTS_MAX_PAGE_SIZE).execute();
      if (response != null && response.getProjects() != null) {
        // Sorts the projects list by project ID.
        Set<Project> allProjects = new TreeSet<Project>(new Comparator<Project>() {
          @Override
          public int compare(Project p1, Project p2) {
            return p1.getTitle().toLowerCase().compareTo(p2.getTitle().toLowerCase());
          }
        });
        allProjects.addAll(response.getProjects());
        while(!Strings.isNullOrEmpty(response.getNextPageToken())) {
          response = developerProjectsClient.projects().list()
              .setPageToken(response.getNextPageToken())
              .setPageSize(PROJECTS_MAX_PAGE_SIZE)
              .execute();
          allProjects.addAll(response.getProjects());
        }
        for (Project pantheonProject : allProjects) {
          if (!Strings.isNullOrEmpty(pantheonProject.getProjectId())) {
            result.add(new ElysiumProjectModelItem(pantheonProject.getTitle(),
                                                   pantheonProject.getProjectId(),
                                                   pantheonProject.getProjectNumber()));
          }
        }
      }
    }
    catch (IOException ex) {
      // https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/323
      loadErrorState(GctBundle.getString("clouddebug.couldnotconnect"));
      return;
    }
    catch (RuntimeException ex) {
      LOG.error("Exception loading projects for " + myUser.getName(), ex);
      loadErrorState(ex.getMessage());
      return;
    }

    result.add(new ElysiumNewProjectModelItem());

    try {
      // We invoke back to the UI thread to update the model and treeview.
      SwingUtilities.invokeAndWait(new Runnable() {
        @Override
        public void run() {
          GoogleUserModelItem.this.removeAllChildren();

          for (DefaultMutableTreeNode item : result) {
            GoogleUserModelItem.this.add(item);
          }

          myTreeModel.reload(GoogleUserModelItem.this);
        }
      });
    }
    catch (InterruptedException ex) {
      LOG.error("InterruptedException loading projects for " + myUser.getName(), ex);
      loadErrorState(ex.getMessage());
      Thread.currentThread().interrupt();
    }
    catch (InvocationTargetException ex) {
      LOG.error("InvocationTargetException loading projects for " + myUser.getName(), ex);
      loadErrorState(ex.getMessage());
    }
  }
}
