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
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.IntegratedGoogleLoginService;
import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.project.ProjectSelector;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestService;
import com.google.cloud.tools.intellij.util.GctBundle;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/** Unit tests for {@link AppEngineDeploymentConfigurationPanel}. */
public final class AppEngineDeploymentConfigurationPanelTest {
  private AppEngineDeploymentConfigurationPanel configurationPanel;
  private AppEngineDeploymentConfiguration deploymentConfiguration;

  @Rule public CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @TestService @Mock private IntegratedGoogleLoginService mockGoogleLoginService;
  @Mock private AppEngineApplicationInfoPanel infoPanel;
  @Mock private ProjectSelector projectSelector;
  @Mock private CredentialedUser credentialedUser;
  @Mock private Credential credential;

  @Before
  public void setUp() {
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
    CloudProject project = CloudProject.create("projectId", "projectId", "some-user-id");
    when(projectSelector.getSelectedProject()).thenReturn(project);

    deploymentConfiguration.setCloudProjectName("projectName");
    deploymentConfiguration.setGoogleUsername("some-user-id");
    configurationPanel.resetEditorFrom(deploymentConfiguration);

    verify(infoPanel)
        .setMessage(GctBundle.getString("appengine.infopanel.no.region"), true /* isError*/);
  }

  @Test
  public void projectSelector_validProjectSelected_infoPanelShowsProjectDetails() {
    String projectId = "projectId";
    String googleUsername = "some-user-id";
    CloudProject project = CloudProject.create(projectId, projectId, googleUsername);
    when(credentialedUser.getCredential()).thenReturn(credential);
    when(mockGoogleLoginService.getLoggedInUser(googleUsername))
        .thenReturn(Optional.of(credentialedUser));
    when(projectSelector.getSelectedProject()).thenReturn(project);

    deploymentConfiguration.setCloudProjectName(projectId);
    deploymentConfiguration.setGoogleUsername(googleUsername);
    configurationPanel.resetEditorFrom(deploymentConfiguration);

    verify(infoPanel).refresh(projectId, credential);
  }
}
