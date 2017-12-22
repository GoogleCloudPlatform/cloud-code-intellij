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
package com.google.cloud.tools.intellij.debugger.ui;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.clouddebugger.v2.model.Debuggee;
import com.google.cloud.tools.intellij.debugger.CloudDebugProcessState;
import com.google.cloud.tools.intellij.debugger.ProjectRepositoryValidator;
import com.google.cloud.tools.intellij.debugger.SyncResult;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.IntegratedGoogleLoginService;
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.project.ProjectSelector;
import com.google.cloud.tools.intellij.testing.TestUtils;
import com.google.gdt.eclipse.login.common.GoogleLoginState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.ui.components.JBCheckBox;
import java.util.LinkedHashMap;
import javax.swing.JComboBox;
import javax.swing.JLabel;

public class CloudAttachDialogTest extends PlatformTestCase {
  private static final String NO_LOGIN_WARNING = "You must be logged in to perform this action.";
  private static final String NO_PROJECT_ID_WARNING = "Please enter a Project ID.";
  private static final String NO_MODULES_FOUND_WARNING =
      "No debuggable modules found. Please ensure that your application has live instances.";

  private static final String USER = "test@user.com";
  private static final String PASSWORD = "123";
  private CredentialedUser user;

  private ProjectSelector projectSelector;
  private ProjectDebuggeeBinding binding;
  private JComboBox targetSelector;
  private JBCheckBox syncStashCheckbox;
  private JLabel warningHeader;
  private JLabel warningMessage;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    mockCredentials();
  }

  public void testErrorWhenUserIsLoggedOut() {
    CloudAttachDialog dialog = initDialog();
    mockLoggedOutUser();
    ValidationInfo error = dialog.doValidate();

    assertNotNull(error);
    assertEquals(NO_LOGIN_WARNING, error.message);

    dialog.close(0);
  }

  public void testNoProjectSelected() {
    CloudAttachDialog dialog = initDialog();
    mockLoggedInUser();
    ValidationInfo error = dialog.doValidate();

    assertNotNull(error);
    assertEquals(NO_PROJECT_ID_WARNING, error.message);

    dialog.close(0);
  }

  public void testNoModulesFound() {
    CloudAttachDialog dialog = initDialog();
    mockLoggedInUser();
    selectEmptyProject();
    ValidationInfo error = dialog.doValidate();

    assertNotNull(error);
    assertEquals(NO_MODULES_FOUND_WARNING, error.message);
    assertFalse(warningMessage.isVisible());
    assertFalse(warningHeader.isVisible());

    assertFalse(targetSelector.isEnabled());

    dialog.close(0);
  }

  public void testDebuggableModuleSelected() {
    mockLoggedInUser();

    binding = mock(ProjectDebuggeeBinding.class);
    when(binding.buildResult(any(Project.class))).thenReturn(new CloudDebugProcessState());

    ProjectRepositoryValidator repositoryValidator = mock(ProjectRepositoryValidator.class);
    SyncResult syncResult = mockSyncResult(false, true);
    when(repositoryValidator.checkSyncStashState()).thenReturn(syncResult);

    CloudAttachDialog dialog = initDialog();
    dialog.setProjectRepositoryValidator(repositoryValidator);

    selectProjectWithDebuggableModules();
    ValidationInfo error = dialog.doValidate();

    assertNull(error);

    assertFalse(warningMessage.isVisible());
    assertFalse(warningHeader.isVisible());

    assertFalse(syncStashCheckbox.isVisible());

    assertTrue(targetSelector.isEnabled());

    dialog.close(0);
  }

  /**
   * If there is no module loaded (including no default module) then this indicates that the async
   * module loading is still in progress. We do not want to display an error to the user until the
   * thread completes to avoid flashing error messages
   */
  public void testUnknownProjectSelected() {
    CloudAttachDialog dialog = initDialog();
    mockLoggedInUser();
    selectInProgressProject();

    ValidationInfo error = dialog.doValidate();

    assertNull(error);
    assertFalse(targetSelector.isEnabled());
    assertNull(targetSelector.getSelectedItem());
    // We want to make sure that the OK button is disabled, though.
    assertFalse(dialog.isOKActionEnabled());

    dialog.close(0);
  }

  /**
   * After selecting a module that requires sync/stash, any subsequent module that is selected that
   * has no remote repository is shown the sync/stash checkbox regardless of its state. The
   * visibility of this option needs to be properly reset.
   */
  public void testSyncStashReset() {
    mockLoggedInUser();

    binding = mock(ProjectDebuggeeBinding.class);
    when(binding.buildResult(any(Project.class))).thenReturn(new CloudDebugProcessState());

    // Step 1 - select a debuggable module that requires stashing
    // The stash checkbox should be visible to the user

    boolean needsStash = true;
    boolean hasRemoteRepository = true;
    ProjectRepositoryValidator repositoryValidator = mock(ProjectRepositoryValidator.class);
    SyncResult syncResult = mockSyncResult(needsStash, hasRemoteRepository);
    when(repositoryValidator.checkSyncStashState()).thenReturn(syncResult);

    CloudAttachDialog dialog = initDialog();
    dialog.setProjectRepositoryValidator(repositoryValidator);
    selectProjectWithDebuggableModules();

    assertTrue(syncStashCheckbox.isVisible());

    // Step 2 - select a project with no remote repo that does NOT require stashing
    // The stash checkbox should now be hidden from the user

    needsStash = false;
    hasRemoteRepository = false;
    syncResult = mockSyncResult(needsStash, hasRemoteRepository);
    when(repositoryValidator.checkSyncStashState()).thenReturn(syncResult);
    selectEmptyProject();

    assertFalse(syncStashCheckbox.isVisible());

    dialog.close(0);
  }

  private CloudAttachDialog initDialog() {
    CloudAttachDialog dialog = new CloudAttachDialog(this.getProject(), binding);
    projectSelector = dialog.getProjectSelector();
    targetSelector = dialog.getTargetSelector();
    warningHeader = dialog.getWarningHeader();
    warningMessage = dialog.getWarningMessage();
    syncStashCheckbox = dialog.getSyncStashCheckbox();

    return dialog;
  }

  @SuppressWarnings("unchecked")
  private void selectEmptyProject() {
    String projectName = "emptyProject";
    projectSelector.setSelectedProject(
        CloudProject.create(projectName, projectName, null, "some-user-id"));

    targetSelector.removeAllItems();
    targetSelector.setEnabled(false);
    targetSelector.addItem(NO_MODULES_FOUND_WARNING);
  }

  @SuppressWarnings("unchecked")
  private void selectProjectWithDebuggableModules() {
    String projectName = "projectWithDebuggableModules";
    projectSelector.setSelectedProject(
        CloudProject.create(projectName, projectName, null, "some-user-id"));
    targetSelector.removeAllItems();
    targetSelector.setEnabled(true);

    DebugTarget debugTarget = new DebugTarget(new Debuggee(), projectName);
    targetSelector.addItem(debugTarget);
  }

  private void selectInProgressProject() {
    String projectName = "unknownProject";
    projectSelector.setSelectedProject(
        CloudProject.create(projectName, projectName, null, "some-user-id"));
  }

  private void mockCredentials() throws Exception {
    IntegratedGoogleLoginService mockLoginService =
        TestUtils.installMockService(IntegratedGoogleLoginService.class);

    GoogleLoginState googleLoginState = mock(GoogleLoginState.class);
    Credential credential = mock(Credential.class);
    this.user = mock(CredentialedUser.class);
    LinkedHashMap<String, CredentialedUser> allusers =
        new LinkedHashMap<String, CredentialedUser>();

    when(this.user.getCredential()).thenReturn(credential);
    when(this.user.getEmail()).thenReturn(USER);
    when(this.user.getGoogleLoginState()).thenReturn(googleLoginState);
    when(googleLoginState.fetchAccessToken()).thenReturn(PASSWORD);
    when(mockLoginService.getAllUsers()).thenReturn(allusers);
    allusers.put(USER, this.user);
  }

  private void mockLoggedOutUser() {
    when(Services.getLoginService().isLoggedIn()).thenReturn(false);
  }

  private void mockLoggedInUser() {
    when(Services.getLoginService().isLoggedIn()).thenReturn(true);
  }

  /**
   * Creates a mock sync result representing a debuggable module selection that doesn't need stash
   * or sync
   */
  private SyncResult mockSyncResult(boolean needsStash, boolean hasRemoteRepository) {
    SyncResult syncResult = mock(SyncResult.class);
    when(syncResult.needsStash()).thenReturn(needsStash);
    when(syncResult.needsSync()).thenReturn(false);
    when(syncResult.getTargetSyncSha()).thenReturn(null);
    when(syncResult.hasRemoteRepository()).thenReturn(hasRemoteRepository);

    return syncResult;
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }
}
