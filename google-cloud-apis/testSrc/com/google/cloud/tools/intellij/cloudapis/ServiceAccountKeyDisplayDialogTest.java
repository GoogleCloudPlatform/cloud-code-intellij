/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.cloudapis;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JTable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

public class ServiceAccountKeyDisplayDialogTest {
  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @TestFixture private IdeaProjectTestFixture testFixture;
  @Mock private CloudProject mockCloudProject;
  @Mock private RunnerAndConfigurationSettings mockRunnerAndConfigurationSettings;

  private ServiceAccountKeyDisplayDialog dialog;

  @Before
  public void setUp() {
    when(mockRunnerAndConfigurationSettings.getName()).thenReturn("name");
  }

  @Test
  public void runConfigurationTable_whenConfigurationsDoNotExist_Hidden() {
    launchDialog(new ArrayList<>());
    assertThat(dialog.getRunConfigurationTable().isVisible()).isFalse();
  }

  @Test
  public void runConfigurationTable_whenConfigurationsExist_Visible() {
    launchDialog(Collections.singletonList(mockRunnerAndConfigurationSettings));
    assertThat(dialog.getRunConfigurationTable().isVisible()).isTrue();
  }

  @Test
  public void runConfigurationTable_verifyValues() {
    launchDialog(Collections.singletonList(mockRunnerAndConfigurationSettings));

    JTable runConfigurationTable = dialog.getRunConfigurationTable();
    assertThat(runConfigurationTable.getRowCount()).isEqualTo(1);
    assertThat(runConfigurationTable.getColumnCount()).isEqualTo(2);
    assertThat(runConfigurationTable.getModel().getValueAt(0, 0))
        .isEqualTo(mockRunnerAndConfigurationSettings);
    assertThat((Boolean) runConfigurationTable.getModel().getValueAt(0, 1)).isTrue();
  }

  private void launchDialog(List<RunnerAndConfigurationSettings> configurationSettingsList) {
    when(mockCloudProject.projectId()).thenReturn("gcpProjectId");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () ->
                dialog =
                    new ServiceAccountKeyDisplayDialog(
                        testFixture.getProject(), mockCloudProject, "downloadPath"));
    dialog.initTableModel(configurationSettingsList);
  }
}
