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

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.apis.TestCloudLibrary;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Set;
import javax.swing.table.TableModel;
import org.junit.Rule;
import org.junit.Test;

/** Tests for {@link GoogleCloudApiManagementPanel}. */
public class GoogleCloudApiManagementPanelTest {

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  private static final TestCloudLibrary LIB_1 = TestCloudLibrary.createWithName("Client 1");
  private static final TestCloudLibrary LIB_2 = TestCloudLibrary.createWithName("Client 2");
  private static final TestCloudLibrary LIB_3 = TestCloudLibrary.createWithName("Client 3");
  private static final TestCloudLibrary LIB_4 = TestCloudLibrary.createWithName("Client 4");

  @Test
  public void getPanel_withNoCloudProject_hidesManagementUi() {
    GoogleCloudApiManagementPanel panel = createPanel();

    assertFalse(panel.getApisScrollPane().isVisible());
    assertTrue(panel.getSelectProjectLabel().isVisible());
  }

  @Test
  public void getPanel_withCloudProject_showsManagementUi() {
    GoogleCloudApiManagementPanel panel = createPanel();

    panel
        .getProjectSelector()
        .setSelectedProject(CloudProject.create("testName", "testId", "testUserName"));
    panel.getProjectSelector().notifyProjectSelectionListeners();

    assertTrue(panel.getApisScrollPane().isVisible());
    assertFalse(panel.getSelectProjectLabel().isVisible());
  }

  @Test
  public void selectedApiTable_matchesSelectedClientLibraries() {
    GoogleCloudApiManagementPanel panel = createPanel();

    panel
        .getProjectSelector()
        .setSelectedProject(CloudProject.create("testName", "testId", "testUserName"));
    panel.getProjectSelector().notifyProjectSelectionListeners();

    List<CloudLibrary> libs = collectCloudLibraries(panel.getSelectedApisTable().getModel());
    assertEquals(2, libs.size());
    assertThat(
        libs.stream().map(CloudLibrary::getName).collect(toList()),
        hasItems("Client 2", "Client 4"));
  }

  @Test
  public void allApiTable_showsDifferenceofAllLibrariesAndSelectedLibraries() {
    GoogleCloudApiManagementPanel panel = createPanel();

    panel
        .getProjectSelector()
        .setSelectedProject(CloudProject.create("testName", "testId", "testUserName"));
    panel.getProjectSelector().notifyProjectSelectionListeners();

    List<CloudLibrary> libs = collectCloudLibraries(panel.getAllApisTable().getModel());
    assertEquals(2, libs.size());
    assertThat(
        libs.stream().map(CloudLibrary::getName).collect(toList()),
        hasItems("Client 1", "Client 3"));
  }

  private static GoogleCloudApiManagementPanel createPanel() {
    GoogleCloudApiManagementPanel panel = new GoogleCloudApiManagementPanel();

    CloudLibrary lib1 = LIB_1.toCloudLibrary();
    CloudLibrary lib2 = LIB_2.toCloudLibrary();
    CloudLibrary lib3 = LIB_3.toCloudLibrary();
    CloudLibrary lib4 = LIB_4.toCloudLibrary();

    List<CloudLibrary> allApis = ImmutableList.of(lib1, lib2, lib3, lib4);

    Set<CloudLibrary> selectedApis = ImmutableSet.of(lib2, lib4);

    panel.init(allApis, selectedApis);

    return panel;
  }

  private static List<CloudLibrary> collectCloudLibraries(TableModel model) {
    int cloudLibraryColNum = 0;
    List<CloudLibrary> libs = Lists.newArrayList();

    for (int i = 0; i < model.getRowCount(); i++) {
      libs.add((CloudLibrary) model.getValueAt(i, cloudLibraryColNum));
    }

    return libs;
  }
}
