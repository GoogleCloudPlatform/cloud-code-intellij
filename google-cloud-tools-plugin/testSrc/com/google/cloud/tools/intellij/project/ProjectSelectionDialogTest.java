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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.IntegratedGoogleLoginService;
import com.google.cloud.tools.intellij.project.ProjectLoader.ProjectLoaderResultCallback;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestService;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;

/** Tests {@link ProjectSelectionDialog} data model and UI interactions. */
public class ProjectSelectionDialogTest {

  @Rule public CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  private static final String TEST_USER_EMAIL = "test@google.com";
  private static final String TEST_PROJECT_NAME = "test-1";

  private CloudProject testUiProject;
  private Project testCloudProject;

  @TestService @Mock private IntegratedGoogleLoginService googleLoginService;
  @Mock private CredentialedUser mockTestUser;
  @Mock private ProjectLoader mockProjectLoader;

  @Mock private JButton mockDialogButton;

  private ProjectSelectionDialog projectSelectionDialog;

  @Before
  public void setUp() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          projectSelectionDialog = spy(new ProjectSelectionDialog());
          doNothing().when(projectSelectionDialog).init();
          projectSelectionDialog.setProjectLoader(mockProjectLoader);
          doReturn(mockDialogButton).when(projectSelectionDialog).getButton(any());
          projectSelectionDialog.createUIComponents();
          projectSelectionDialog.loadUsersAndProjects();
        });

    testUiProject = new CloudProject(TEST_PROJECT_NAME, TEST_USER_EMAIL);
    testCloudProject = new Project();
    testCloudProject.setName(TEST_PROJECT_NAME);
    testCloudProject.setProjectId(TEST_PROJECT_NAME + "-id");

    when(mockTestUser.getEmail()).thenReturn(TEST_USER_EMAIL);
  }

  @Test
  public void starts_empty_signInScreen_noSelection() {
    assertThat(projectSelectionDialog.getCenterPanelWrapper().getComponent(0))
        .isInstanceOf(ProjectSelectorSignInPanel.class);

    assertThat(projectSelectionDialog.getAccountComboBox().getSelectedItem()).isNull();
    assertThat(projectSelectionDialog.getProjectListTable().getSelectedRow()).isLessThan(0);
    assertThat(projectSelectionDialog.getProjectListTableModel().getRowCount()).isEqualTo(0);
  }

  @Test
  public void existingSignedInUser_signInScreen_notVisible() {
    mockUserList(Collections.singletonList(mockTestUser));
    projectSelectionDialog.loadUsersAndProjects();

    assertThat(projectSelectionDialog.getCenterPanelWrapper().getComponent(0))
        .isNotInstanceOf(ProjectSelectorSignInPanel.class);
  }

  @Test
  public void nullProject_activeUser_selected_noProject_selected() {
    prepareOneTestUserOneTestProjectDialog(null);

    assertThat(projectSelectionDialog.getAccountComboBox().getSelectedItem())
        .isEqualTo(mockTestUser);
    assertThat(projectSelectionDialog.getProjectListTableModel().getRowCount()).isEqualTo(1);
    assertThat(projectSelectionDialog.getProjectListTable().getSelectedRow()).isEqualTo(-1);
  }

  @Test
  public void emptyCloudProject_activeUser_selected_noProject_selected() {
    prepareOneTestUserOneTestProjectDialog(new CloudProject("", ""));

    assertThat(projectSelectionDialog.getAccountComboBox().getSelectedItem())
        .isEqualTo(mockTestUser);
    assertThat(projectSelectionDialog.getProjectListTableModel().getRowCount()).isEqualTo(1);
    assertThat(projectSelectionDialog.getProjectListTable().getSelectedRow()).isEqualTo(-1);
  }

  @Test
  public void setCloudProject_updatesUi() {
    prepareOneTestUserOneTestProjectDialog(testUiProject);

    assertThat(projectSelectionDialog.getAccountComboBox().getSelectedItem())
        .isEqualTo(mockTestUser);
    assertThat(projectSelectionDialog.getSelectedProjectName())
        .isEqualTo(testCloudProject.getName());
    assertThat(projectSelectionDialog.getProjectListTable().getSelectedRow()).isEqualTo(0);
  }

  @Test
  public void addActiveAccount_noProjects_clearsProjectList()
      throws InvocationTargetException, InterruptedException {
    prepareOneTestUserOneTestProjectDialog(testUiProject);
    String activeUserEmail = "active-test@google.com";
    CredentialedUser mockAnotherUser = mock(CredentialedUser.class);
    when(mockAnotherUser.getEmail()).thenReturn(activeUserEmail);
    mockUserList(Arrays.asList(mockAnotherUser /* active */, mockTestUser));

    SwingUtilities.invokeAndWait(() -> {
      projectSelectionDialog.loadUsersAndProjects();
    });

    assertThat(projectSelectionDialog.getAccountComboBox().getSelectedItem())
        .isEqualTo(mockAnotherUser);
    assertThat(projectSelectionDialog.getProjectListTableModel().getRowCount()).isEqualTo(0);
    assertThat(projectSelectionDialog.getProjectListTable().getSelectedRow()).isEqualTo(-1);
  }

  /**
   * Prepares common test case with test user (active) and its one test project.
   *
   * @param selectedProject Project to set as selected for the dialog, may be null/empty.
   */
  private void prepareOneTestUserOneTestProjectDialog(CloudProject selectedProject) {
    mockUserList(Collections.singletonList(mockTestUser));
    mockUserProjects(mockTestUser, Collections.singletonList(testCloudProject));

    // wait until UI events are processed.
    try {
      SwingUtilities.invokeAndWait(() -> {
        projectSelectionDialog.loadUsersAndProjects();
        projectSelectionDialog.setCloudProject(selectedProject);
      });
      SwingUtilities.invokeAndWait(() -> {});
    } catch (Exception ex) {
      // this should not happen in the test.
      throw new AssertionError(ex);
    }
  }

  /** Mocks list of currently signed in users returned by login service. First user is active. */
  private void mockUserList(List<CredentialedUser> userList) {
    Map<String, CredentialedUser> emailUserMap =
        userList
            .stream()
            .collect(Collectors.toMap(CredentialedUser::getEmail, Function.identity()));
    when(googleLoginService.getAllUsers()).thenReturn(emailUserMap);
    when(googleLoginService.getActiveUser()).thenReturn(userList.get(0));

    for (CredentialedUser user : userList) {
      when(googleLoginService.ensureLoggedIn(user.getEmail())).thenReturn(true);
      when(googleLoginService.getLoggedInUser(user.getEmail())).thenReturn(Optional.of(user));
    }
  }

  /** Mocks list of project returned for a user when selection dialog calls for it. */
  private void mockUserProjects(CredentialedUser user, List<Project> projectList) {
    doAnswer(
            invocation -> {
              ProjectLoaderResultCallback callback = invocation.getArgument(1);
              callback.projectListReady(projectList);
              return null;
            })
        .when(mockProjectLoader)
        .loadUserProjectsInBackground(ArgumentMatchers.eq(user), ArgumentMatchers.any());
  }
}
