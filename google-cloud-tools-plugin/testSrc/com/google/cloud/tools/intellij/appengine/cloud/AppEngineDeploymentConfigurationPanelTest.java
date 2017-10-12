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

package com.google.cloud.tools.intellij.appengine.cloud;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.cloud.tools.intellij.appengine.application.GoogleApiException;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.resources.ProjectSelector;
import com.google.cloud.tools.intellij.util.GctBundle;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/** Unit tests for {@link AppEngineDeploymentConfigurationPanel}. */
@RunWith(MockitoJUnitRunner.class)
public final class AppEngineDeploymentConfigurationPanelTest {
  private AppEngineDeploymentConfigurationPanel configurationPanel;
  private AppEngineDeploymentConfiguration deploymentConfiguration;

  @Mock private AppEngineApplicationInfoPanel infoPanel;
  @Mock private ProjectSelector projectSelector;
  @Mock private CredentialedUser credentialedUser;
  @Mock private Credential credential;

  @Before
  public void setUp() throws Exception {
    configurationPanel = new AppEngineDeploymentConfigurationPanel();
    deploymentConfiguration = new AppEngineDeploymentConfiguration();
    configurationPanel.setApplicationInfoPanel(infoPanel);
    configurationPanel.setProjectSelector(projectSelector);
  }

  @Test
  public void projectSelector_noProjectSelected_infoPanelIsEmpty() {
    configurationPanel.resetEditorFrom(deploymentConfiguration);
    verify(infoPanel).clearMessage();
  }

  @Test
  public void projectSelector_invalidProjectSelected_infoPanelShowsError() {
    when(projectSelector.getText()).thenReturn("projectId");
    deploymentConfiguration.setCloudProjectName("projectName");
    configurationPanel.resetEditorFrom(deploymentConfiguration);
    verify(infoPanel).setMessage(
        GctBundle.getString("appengine.infopanel.no.region"), true /* isError*/);
  }

  @Test
  public void projectSelector_validProjectSelected_infoPanelShowsProjectDetails()
      throws IOException, GoogleApiException {
    String projectId = "projectId";
    Project project = new Project();
    project.setProjectId(projectId);
    when(credentialedUser.getCredential()).thenReturn(credential);
    when(projectSelector.getProject()).thenReturn(project);
    when(projectSelector.getSelectedUser()).thenReturn(credentialedUser);
    when(projectSelector.getText()).thenReturn(projectId);

    deploymentConfiguration.setCloudProjectName(projectId);
    configurationPanel.resetEditorFrom(deploymentConfiguration);

    verify(infoPanel).refresh(projectId, credential);
  }
}
