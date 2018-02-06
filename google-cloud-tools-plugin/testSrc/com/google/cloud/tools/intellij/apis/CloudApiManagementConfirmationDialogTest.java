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

package com.google.cloud.tools.intellij.apis;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.services.iam.v1.model.Role;
import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.google.cloud.tools.intellij.testing.TestModule;
import com.google.cloud.tools.intellij.testing.apis.TestCloudLibrary;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.common.collect.ImmutableSet;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import java.util.Set;
import javax.swing.table.TableModel;
import org.junit.Rule;
import org.junit.Test;

/** Tests for {@link CloudApiManagementConfirmationDialog}. */
public class CloudApiManagementConfirmationDialogTest {

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
  @TestFixture private IdeaProjectTestFixture testFixture;
  @TestModule Module module;

  private static final CloudProject cloudProject = CloudProject.create("name", "id", "user");

  @Test
  public void enablementUiState_whenAllApisAreEnabled() {
    Set<CloudLibrary> libraries = ImmutableSet.of(TestCloudLibrary.createEmpty().toCloudLibrary());

    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              CloudApiManagementConfirmationDialog dialog =
                  new CloudApiManagementConfirmationDialog(
                      module, cloudProject, libraries, ImmutableSet.of(), ImmutableSet.of());

              assertThat(dialog.getApisToEnablePanel().isVisible()).isTrue();
              assertThat(dialog.getApisNotSelectedToEnablePanel().isVisible()).isFalse();
            });
  }

  @Test
  public void enablementUiState_whenSomeApisAreEnabled() {
    Set<CloudLibrary> librariesToEnable =
        ImmutableSet.of(TestCloudLibrary.createEmpty().toCloudLibrary());
    Set<CloudLibrary> librariesNotToEnable =
        ImmutableSet.of(TestCloudLibrary.createEmpty().toCloudLibrary());

    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              CloudApiManagementConfirmationDialog dialog =
                  new CloudApiManagementConfirmationDialog(
                      module,
                      cloudProject,
                      librariesToEnable,
                      librariesNotToEnable,
                      ImmutableSet.of());

              assertThat(dialog.getApisToEnablePanel().isVisible()).isTrue();
              assertThat(dialog.getApisNotSelectedToEnablePanel().isVisible()).isTrue();
            });
  }

  @Test
  public void enablementUiState_whenNoApisAreEnabled() {
    Set<CloudLibrary> librariesNotToEnable =
        ImmutableSet.of(TestCloudLibrary.createEmpty().toCloudLibrary());

    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              CloudApiManagementConfirmationDialog dialog =
                  new CloudApiManagementConfirmationDialog(
                      module,
                      cloudProject,
                      ImmutableSet.of(),
                      librariesNotToEnable,
                      ImmutableSet.of());

              assertThat(dialog.getApisToEnablePanel().isVisible()).isFalse();
              assertThat(dialog.getApisNotSelectedToEnablePanel().isVisible()).isTrue();
            });
  }

  @Test
  public void serviceAccountUi_whenSomeRolesExist_isVisible() {
    Set<CloudLibrary> libraries = ImmutableSet.of(TestCloudLibrary.createEmpty().toCloudLibrary());
    Role role = new Role();
    role.setName("name");
    role.setTitle("title");
    Set<Role> roles = ImmutableSet.of(role);

    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              CloudApiManagementConfirmationDialog dialog =
                  new CloudApiManagementConfirmationDialog(
                      module, cloudProject, libraries, ImmutableSet.of(), roles);

              assertThat(dialog.getRolePanel().isVisible()).isTrue();
            });
  }

  @Test
  public void serviceAccountUi_whenNoRolesExist_isHidden() {
    Set<CloudLibrary> libraries = ImmutableSet.of(TestCloudLibrary.createEmpty().toCloudLibrary());

    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              CloudApiManagementConfirmationDialog dialog =
                  new CloudApiManagementConfirmationDialog(
                      module, cloudProject, libraries, ImmutableSet.of(), ImmutableSet.of());

              assertThat(dialog.getRolePanel().isVisible()).isFalse();
            });
  }

  @Test
  public void roleTable_whenRolesExist_isPopulated_andAllSelectedByDefault() {
    Set<CloudLibrary> librariesNotToEnable =
        ImmutableSet.of(TestCloudLibrary.createEmpty().toCloudLibrary());

    Role role1 = new Role();
    role1.setName("my_role");
    role1.setTitle("My Role");
    Role role2 = new Role();
    role2.setName("my_role_2");
    role2.setTitle("My Role 2");
    Set<Role> roles = ImmutableSet.of(role1, role2);

    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              CloudApiManagementConfirmationDialog dialog =
                  new CloudApiManagementConfirmationDialog(
                      module, cloudProject, ImmutableSet.of(), librariesNotToEnable, roles);

              TableModel model = dialog.getRoleTable().getModel();
              assertThat(model.getRowCount()).isEqualTo(2);

              Set<Role> allValues =
                  ImmutableSet.of((Role) model.getValueAt(0, 0), (Role) model.getValueAt(1, 0));
              assertThat(allValues).containsExactlyElementsIn(roles);
              assertThat(dialog.getSelectedRoles()).containsExactlyElementsIn(roles);
            });
  }
}
