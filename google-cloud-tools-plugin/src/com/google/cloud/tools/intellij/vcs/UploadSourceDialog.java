/*
 * Copyright (C) 2016 The Android Open Source Project
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

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.util.Key;
import com.google.api.client.util.Preconditions;
import com.google.api.services.source.Source;
import com.google.api.services.source.SourceRequest;
import com.google.api.services.source.model.ListReposResponse;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.resources.ProjectSelector;
import com.google.cloud.tools.intellij.resources.RepositorySelector;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.DocumentAdapter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Dimension;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;

/**
 * Shows a dialog that has one entry value which is a GCP project using the project selector. The
 * title and ok button text is passed into the constructor.
 */
public class UploadSourceDialog extends DialogWrapper {

  private JPanel rootPanel;
  private ProjectSelector projectSelector;
  private RepositorySelector repositorySelector;
  private String projectId;
  private String repositoryId;
  private CredentialedUser credentialedUser;

  /**
   * Initialize the project selection dialog.
   */
  public UploadSourceDialog(@NotNull Project project, @NotNull String title,
      @NotNull String okText) {
    super(project, true);
    init();
    setTitle(title);
    setOKButtonText(okText);
    setOKActionEnabled(false);
  }

  /**
   * Return the project ID selected by the user.
   */
  @NotNull
  public String getProjectId() {
    return projectId;
  }

  @NotNull String getRepositoryId() {
    return repositoryId;
  }

  /**
   * Return the credentialeduser that owns the ID returned from {@link #getProjectId()}.
   */
  @Nullable
  public CredentialedUser getCredentialedUser() {
    return credentialedUser;
  }

  @Override
  protected String getDimensionServiceKey() {
    return "UploadSourceDialog";
  }

  private void createUIComponents() {
    projectSelector = new ProjectSelector();
    projectSelector.setMinimumSize(new Dimension(300, 0));

    repositorySelector = new RepositorySelector(projectSelector.getText(), projectSelector.getSelectedUser(), true /*canCreateRepository*/);

    projectSelector.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent event) {
        setOKActionEnabled(projectSelector.getSelectedUser() != null);
        repositorySelector.setCloudProject(projectSelector.getText());
        repositorySelector.setUser(projectSelector.getSelectedUser());
      }
    });

    // todo figure out how to disable it when no project is selected
//    repositorySelector.setEnabled(false);
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return projectSelector;
  }

  @Override
  protected JComponent createCenterPanel() {
    return rootPanel;
  }

  @Override
  protected void doOKAction() {
    projectId = projectSelector.getText();
    repositoryId = repositorySelector.getText();
    credentialedUser = projectSelector.getSelectedUser();
    super.doOKAction();
  }

  // TODO move this and trim it down to what's necessary
  public static class MySourceList extends SourceRequest<ListReposResponse>{
    @Key
    private String projectId;

    public MySourceList(Source client, String projectId) {
//      super(client, method, uriTemplate, content, responseClass);
      super(client, "GET", "v1/projects/{projectId}/repos", null, ListReposResponse.class);

      this.projectId = (String) Preconditions.checkNotNull(projectId, "Required parameter projectId must be specified.");
    }

    public String getProjectId() {
          return this.projectId;
        }

        public MySourceList setProjectId(String projectId) {
          this.projectId = projectId;
          return this;
        }

        public MySourceList set(String parameterName, Object value) {
          return (MySourceList) super.set(parameterName, value);
        }

      public HttpResponse executeUsingHead() throws IOException {
        return super.executeUsingHead();
      }

      public HttpRequest buildHttpRequestUsingHead() throws IOException {
        return super.buildHttpRequestUsingHead();
      }

      public MySourceList setAlt(String alt) {
        return (MySourceList)super.setAlt(alt);
      }

      public MySourceList setFields(String fields) {
        return (MySourceList)super.setFields(fields);
      }

      public MySourceList setKey(String key) {
        return (MySourceList)super.setKey(key);
      }

      public MySourceList setOauthToken(String oauthToken) {
        return (MySourceList)super.setOauthToken(oauthToken);
      }

      public MySourceList setPrettyPrint(Boolean prettyPrint) {
        return (MySourceList)super.setPrettyPrint(prettyPrint);
      }

      public MySourceList setQuotaUser(String quotaUser) {
        return (MySourceList) super.setQuotaUser(quotaUser);
      }

      public MySourceList setUserIp(String userIp) {
        return (MySourceList) super.setUserIp(userIp);
      }

  }

}
