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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.util.GctBundle;
import java.awt.Component;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/** Tests {@link ProjectSelector}. */
public class ProjectSelectorTest {
  @Rule public CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  private ProjectSelector projectSelector;
  @Mock private ProjectSelectionListener projectSelectionListener;
  @Mock private ProjectSelectionDialog projectSelectionDialog;

  private static final CloudProject TEST_PROJECT = new CloudProject("test-1", "test@google.com");

  @Before
  public void setUp() {
    projectSelector =
        new ProjectSelector() {
          @Override
          ProjectSelectionDialog createProjectSelectionDialog(Component parent) {
            return projectSelectionDialog;
          }
        };
    projectSelector.addProjectSelectionListener(projectSelectionListener);
  }

  @Test
  public void projectSelector_startsEmpty() {
    assertThat(projectSelector.getSelectedProject()).isNull();
  }

  @Test
  public void setProject_updatesUi() {
    projectSelector.setSelectedProject(TEST_PROJECT);

    assertThat(projectSelector.getProjectNameLabel().getText())
        .isEqualTo(TEST_PROJECT.getProjectName());
    assertThat(projectSelector.getProjectAccountSeparatorLabel().isVisible()).isTrue();
    assertThat(projectSelector.getAccountInfoLabel().getText())
        .isEqualTo(TEST_PROJECT.getGoogleUsername());
  }

  @Test
  public void projectChange_returnValidValue() {
    when(projectSelectionDialog.showDialog(any())).thenReturn(TEST_PROJECT);

    projectSelector.handleOpenProjectSelectionDialog();

    assertThat(projectSelector.getSelectedProject().getProjectName())
        .isEqualTo(TEST_PROJECT.getProjectName());
    assertThat(projectSelector.getSelectedProject().getGoogleUsername())
        .isEqualTo(TEST_PROJECT.getGoogleUsername());
  }

  @Test
  public void projectChange_updatesUi() {
    when(projectSelectionDialog.showDialog(any())).thenReturn(TEST_PROJECT);

    projectSelector.handleOpenProjectSelectionDialog();

    assertThat(projectSelector.getProjectNameLabel().getText())
        .isEqualTo(TEST_PROJECT.getProjectName());
    assertThat(projectSelector.getProjectAccountSeparatorLabel().isVisible()).isTrue();
    assertThat(projectSelector.getAccountInfoLabel().getText())
        .isEqualTo(TEST_PROJECT.getGoogleUsername());
  }

  @Test
  public void projectChange_triggerListeners() {
    when(projectSelectionDialog.showDialog(any())).thenReturn(TEST_PROJECT);

    projectSelector.handleOpenProjectSelectionDialog();

    verify(projectSelectionListener).projectSelected(TEST_PROJECT);
  }

  @Test
  public void removedListener_isNotCalled() {
    when(projectSelectionDialog.showDialog(any())).thenReturn(TEST_PROJECT);

    projectSelector.removeProjectSelectionListener(projectSelectionListener);
    projectSelector.handleOpenProjectSelectionDialog();

    verifyNoMoreInteractions(projectSelectionListener);
  }

  @Test
  public void setProject_doesNotTriggerListeners() {
    projectSelector.setSelectedProject(TEST_PROJECT);

    verifyNoMoreInteractions(projectSelectionListener);
  }

  @Test
  public void projectIsNotSelected_doesNotTriggerListeners() {
    // this will return null (unselected) for default mock dialog.
    projectSelector.handleOpenProjectSelectionDialog();

    verifyNoMoreInteractions(projectSelectionListener);
  }

  @Test
  public void setNullProject_acceptsAndUpdatesUi() {
    projectSelector.setSelectedProject(TEST_PROJECT);
    projectSelector.setSelectedProject(null);

    assertThat(projectSelector.getProjectNameLabel().getText())
        .isEqualTo(GctBundle.getString("project.selector.no.selected.project"));
    // no account information UI is visible/populated.
    assertThat(projectSelector.getProjectAccountSeparatorLabel().isVisible()).isFalse();
  }
}
