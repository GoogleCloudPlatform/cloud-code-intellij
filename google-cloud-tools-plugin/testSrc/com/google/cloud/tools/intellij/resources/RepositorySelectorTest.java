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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.services.source.model.ListReposResponse;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.resources.RepositorySelector.ProjectNotSelectedPanel;
import com.google.cloud.tools.intellij.resources.RepositorySelector.RepositoryPanel;
import com.google.cloud.tools.intellij.vcs.CloudRepositoryService;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.ui.awt.RelativePoint;

import org.picocontainer.MutablePicoContainer;

import java.awt.Point;
import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.swing.JPanel;

/**
 * Tests for {@link RepositorySelector}
 */
public class RepositorySelectorTest extends PlatformTestCase {

  private CloudRepositoryService repositoryService;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    MutablePicoContainer applicationContainer = (MutablePicoContainer)
        ApplicationManager.getApplication().getPicoContainer();

    repositoryService = mock(CloudRepositoryService.class);

    applicationContainer.unregisterComponent(CloudRepositoryService.class.getName());
    applicationContainer.registerComponentInstance(
        CloudRepositoryService.class.getName(), repositoryService);
  }

  public void testShowsMissingProjectPanel_WhenProjectIsMissing() {
    RepositorySelector selector = new RepositorySelector(null, null, false);
    selector.showPopup(new RelativePoint(selector, new Point(0,0)));
    JPanel panel = selector.getPanel();

    // Shows the project not selected panel
    assertInstanceOf(panel, ProjectNotSelectedPanel.class);
  }

  public void testShowsRepositoryPanel_WhenProjectSelected()
      throws IOException, GeneralSecurityException {
    when(repositoryService.list(any(CredentialedUser.class), anyString())).thenReturn(new ListReposResponse());
    RepositorySelector selector = new RepositorySelector("my-project", mock(CredentialedUser.class), false);
    selector.showPopup(new RelativePoint(selector, new Point(0,0)));
    JPanel panel = selector.getPanel();

    // Shows the project not selected panel
    assertInstanceOf(panel, RepositoryPanel.class);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }
}
