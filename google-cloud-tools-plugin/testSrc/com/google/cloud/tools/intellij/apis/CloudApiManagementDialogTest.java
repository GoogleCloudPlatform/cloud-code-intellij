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

import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import org.junit.Rule;
import org.junit.Test;

/** Unit tests for {@link CloudApiManagementDialog}. */
public class CloudApiManagementDialogTest {

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
  @TestFixture private IdeaProjectTestFixture testFixture;

  @Test
  public void getDialog_withNoCloudProject_hasOkButtonDisabled() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              CloudApiManagementDialog dialog =
                  new CloudApiManagementDialog(testFixture.getProject());
              dialog.getProjectSelector().setSelectedProject(null);

              assertThat(dialog.isOKActionEnabled()).isFalse();
            });
  }

  @Test
  public void getDialog_withCloudProject_hasOkButtonEnabled() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              CloudApiManagementDialog dialog =
                  new CloudApiManagementDialog(testFixture.getProject());
              dialog
                  .getProjectSelector()
                  .getProjectSelectionListeners()
                  .forEach(
                      listener ->
                          listener.projectSelected(CloudProject.create("id", "name", "user")));

              assertThat(dialog.isOKActionEnabled()).isTrue();
            });
  }
}
