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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.IntegratedGoogleLoginService;
import com.google.cloud.tools.intellij.project.ProjectSelectionDialog.ProjectSelectionDialogWrapper;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
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
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/** Tests {@link ProjectSelectionDialog} data model and UI interactions. */
public class ProjectSelectionDialogTest {

  @Rule public CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  private static final String TEST_USER_EMAIL = "test@google.com";
  private static final String TEST_PROJECT_NAME = "test-1";
  private static final String TEST_PROJECT_ID = "test-1-id";

  private CloudProject testUiProject;
  private Project testGoogleProject;

  @TestService @Mock private IntegratedGoogleLoginService googleLoginService;
  @Mock private CredentialedUser mockTestUser;
  @Mock private ProjectLoader mockProjectLoader;

  @Mock private JButton mockDialogButton;
  @Mock private ProjectSelectionDialogWrapper dialogWrapper;

  @Spy private ProjectSelectionDialog projectSelectionDialog;

  @Before
  public void setUp() {
    projectSelectionDialog.setProjectLoader(mockProjectLoader);
    projectSelectionDialog.setDialogWrapper(dialogWrapper);
    doReturn(mockDialogButton).when(projectSelectionDialog).getDialogButton(any());
    doNothing().when(projectSelectionDialog).installTableSpeedSearch(any());
    doNothing().when(projectSelectionDialog).setLoading(anyBoolean());

    projectSelectionDialog.createUIComponents();
    projectSelectionDialog.loadAllProjects();

    testUiProject = CloudProject.create(TEST_PROJECT_NAME, TEST_PROJECT_ID, TEST_USER_EMAIL);
    testGoogleProject = new Project();
    testGoogleProject.setName(TEST_PROJECT_NAME);
    testGoogleProject.setProjectId(TEST_PROJECT_NAME + "-id");

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
    prepareOneTestUserOneTestProjectDialog(testUiProject);

    cleanLoadUsersAndProjects();

    assertThat(projectSelectionDialog.getCenterPanelWrapper().getComponent(0))
        .isNotInstanceOf(ProjectSelectorSignInPanel.class);
  }

  @Test
  public void nullCloudProject_activeUser_noUiSelection() {
    prepareOneTestUserOneTestProjectDialog(null);

    assertThat(projectSelectionDialog.getAccountComboBox().getSelectedItem())
        .isEqualTo(mockTestUser);
    assertThat(projectSelectionDialog.getProjectListTableModel().getRowCount()).isEqualTo(1);
    assertThat(projectSelectionDialog.getProjectListTable().getSelectedRow()).isEqualTo(-1);
  }

  @Test
  public void emptyCloudProject_activeUser_noUiSelection() {
    prepareOneTestUserOneTestProjectDialog(CloudProject.create("", "", ""));

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
        .isEqualTo(testGoogleProject.getName());
    assertThat(projectSelectionDialog.getSelectedProjectId())
        .isEqualTo(testGoogleProject.getProjectId());
    assertThat(projectSelectionDialog.getProjectListTable().getSelectedRow()).isEqualTo(0);
  }

  @Test
  public void getCloudProject_returns_selectedProject() {
    prepareOneTestUserOneTestProjectDialog(testUiProject);
    Project secondProject = new Project();
    secondProject.setName("project-2");
    secondProject.setProjectId("project-2-id");
    mockUserProjects(mockTestUser, Arrays.asList(testGoogleProject, secondProject));

    cleanLoadUsersAndProjects();
    projectSelectionDialog.showProjectInList(secondProject.getName());
    CloudProject selectedProject = projectSelectionDialog.getSelectedProject();

    CloudProject expected =
        CloudProject.create(
            secondProject.getName(), secondProject.getProjectId(), mockTestUser.getEmail());
    assertThat(selectedProject).isEqualTo(expected);
  }

  @Test
  public void addActiveAccount_withNoProjects_clearsProjectList() {
    prepareOneTestUserOneTestProjectDialog(testUiProject);
    String activeUserEmail = "active-test@google.com";
    CredentialedUser mockAnotherUser = mock(CredentialedUser.class);
    when(mockAnotherUser.getEmail()).thenReturn(activeUserEmail);
    mockUserList(Arrays.asList(mockAnotherUser /* active */, mockTestUser));
    mockUserProjects(mockAnotherUser, Collections.emptyList());

    cleanLoadUsersAndProjects();

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
    mockUserProjects(mockTestUser, Collections.singletonList(testGoogleProject));
    cleanLoadUsersAndProjects();
    projectSelectionDialog.setSelectedProject(selectedProject);
  }

  /** Loads users and projects and ensures UI events are processed before returning. */
  private void cleanLoadUsersAndProjects() {
    // wait until UI events are processed.
    try {
      SwingUtilities.invokeAndWait(() -> projectSelectionDialog.loadAllProjects());
      // second call to wait until project list is updated via invokeLater().
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
    @SuppressWarnings("unchecked")
    ListenableFuture<List<Project>> mockFuture =
        (ListenableFuture<List<Project>>) mock(ListenableFuture.class);
    when(mockProjectLoader.loadUserProjectsInBackground(user)).thenReturn(mockFuture);
    doAnswer(
            new Answer() {
              @Override
              @SuppressWarnings("unchecked")
              public Object answer(InvocationOnMock invocation) {
                ((FutureCallback<List<Project>>) invocation.getArgument(1)).onSuccess(projectList);
                return null;
              }
            })
        .when(projectSelectionDialog)
        .addProjectListFutureCallback(any(), any());
  }
}
