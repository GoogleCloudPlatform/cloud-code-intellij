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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.IntegratedGoogleLoginService;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestService;
import java.util.Optional;
import javax.swing.SwingUtilities;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/** Tests {@link ProjectSelectionDialog} data model and UI interactions. */
public class ProjectSelectionDialogTest {
 @Rule
 public CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

 private static final String TEST_USER_EMAIL = "test@google.com";
 private final CloudProject TEST_PROJECT = new CloudProject("test-1", TEST_USER_EMAIL);

 @TestService @Mock
 private IntegratedGoogleLoginService googleLoginService;
 @Mock private CredentialedUser mockTestUser;

 private ProjectSelectionDialog projectSelectionDialog;

 @Before
 public void setUp() throws Exception {

  SwingUtilities.invokeAndWait(() -> {
   projectSelectionDialog = spy(new ProjectSelectionDialog());
   doNothing().when(projectSelectionDialog).init();
   projectSelectionDialog.createUIComponents();
  });
  when(mockTestUser.getEmail()).thenReturn(TEST_USER_EMAIL);

  when(googleLoginService.ensureLoggedIn(TEST_USER_EMAIL)).thenReturn(true);
  when(googleLoginService.getLoggedInUser(TEST_USER_EMAIL)).thenReturn(Optional.of(mockTestUser));
 }

 @Test
 public void starts_empty_noSelection() {
  assertThat(projectSelectionDialog.getAccountComboBox().getSelectedItem()).isNull();
  assertThat(projectSelectionDialog.getProjectListTable().getSelectedRow()).isLessThan(0);
  assertThat(projectSelectionDialog.getProjectListTableModel().getRowCount()).isEqualTo(0);
 }

 @Test
 public void setProject_updatesUi() {
   projectSelectionDialog.setCloudProject(TEST_PROJECT);
 }

}