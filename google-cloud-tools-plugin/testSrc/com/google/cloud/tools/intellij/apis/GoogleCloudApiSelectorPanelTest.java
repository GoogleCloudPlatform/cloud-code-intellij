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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.GctFeature;
import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.project.ProjectSelector;
import com.google.cloud.tools.intellij.service.PluginInfoService;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.google.cloud.tools.intellij.testing.TestModule;
import com.google.cloud.tools.intellij.testing.TestService;
import com.google.cloud.tools.intellij.testing.apis.TestCloudLibrary;
import com.google.cloud.tools.intellij.testing.apis.TestCloudLibrary.TestCloudLibraryClient;
import com.google.cloud.tools.intellij.testing.apis.TestCloudLibrary.TestCloudLibraryClientMavenCoordinates;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableModel;
import org.eclipse.aether.version.Version;
import org.fest.util.Lists;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;

/** Unit tests for {@link GoogleCloudApiSelectorPanel}. */
@RunWith(JUnit4.class)
public final class GoogleCloudApiSelectorPanelTest {

  private static final String BOM_VERSION = "1.2.3-alpha";
  private static final TestCloudLibraryClientMavenCoordinates JAVA_CLIENT_MAVEN_COORDS_1 =
      TestCloudLibraryClientMavenCoordinates.create("java", "client-1", "1.0.0");
  private static final TestCloudLibraryClientMavenCoordinates JAVA_CLIENT_MAVEN_COORDS_2 =
      TestCloudLibraryClientMavenCoordinates.create("java", "client-2", "2.0.0");
  private static final TestCloudLibraryClient JAVA_CLIENT_1 =
      TestCloudLibraryClient.create(
          "Client 1",
          "java",
          "API Ref 1",
          "alpha",
          "Source 1",
          "Lang Level 1",
          JAVA_CLIENT_MAVEN_COORDS_1);
  private static final TestCloudLibraryClient JAVA_CLIENT_2 =
      TestCloudLibraryClient.create(
          "Client 2",
          "java",
          "API Ref 2",
          "beta",
          "Source 2",
          "Lang Level 2",
          JAVA_CLIENT_MAVEN_COORDS_2);

  private static final TestCloudLibrary LIBRARY_1 =
      TestCloudLibrary.create(
          "Library 1",
          "ID 1",
          "service_1",
          "Docs Link 1",
          "Description 1",
          "Icon Link 1",
          ImmutableList.of(JAVA_CLIENT_1));
  private static final TestCloudLibrary LIBRARY_2 =
      TestCloudLibrary.create(
          "Library 2",
          "ID 2",
          "service_2",
          "Docs Link 2",
          "Description 2",
          "Icon Link 2",
          ImmutableList.of(JAVA_CLIENT_2));

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @TestModule private Module module1;
  @TestModule private Module module2;

  @TestFixture private IdeaProjectTestFixture testFixture;

  @Mock @TestService private PluginInfoService pluginInfoService;
  @Mock @TestService CloudApiMavenService mavenService;

  @Test
  public void getPanel_withOneLibrary_noSelection_hasCheckboxAndEmptyDetails() {
    CloudLibrary library = LIBRARY_1.toCloudLibrary();

    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(ImmutableList.of(library), testFixture.getProject());
    JTable table = panel.getCloudLibrariesTable();

    assertThat(table.getSelectionModel().isSelectionEmpty()).isTrue();

    TableModel model = table.getModel();
    assertThat(model.getRowCount()).isEqualTo(1);
    assertThat(model.getValueAt(0, 0)).isEqualTo(library);
    assertThat((Boolean) model.getValueAt(0, 1)).isFalse();

    assertDetailsEmpty(panel.getDetailsPanel());
  }

  @Test
  public void getPanel_withOneLibrary_selectedLibrary_hasCheckboxAndPopulatedDetails() {
    CloudLibrary library = LIBRARY_1.toCloudLibrary();

    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(ImmutableList.of(library), testFixture.getProject());
    JTable table = panel.getCloudLibrariesTable();
    checkAddLibraryCheckbox(table, 0);

    assertThat(table.getSelectionModel().isSelectedIndex(0)).isTrue();

    TableModel model = table.getModel();
    assertThat(model.getRowCount()).isEqualTo(1);
    assertThat(model.getValueAt(0, 0)).isEqualTo(library);
    assertThat((Boolean) model.getValueAt(0, 1)).isTrue();

    assertDetailsShownForLibrary(panel.getDetailsPanel(), LIBRARY_1, JAVA_CLIENT_1);
  }

  @Test
  public void getPanel_withOneLibrary_focusedLibrary_hasUncheckedCheckboxAndPopulatedDetails() {
    CloudLibrary library = LIBRARY_1.toCloudLibrary();

    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(ImmutableList.of(library), testFixture.getProject());
    JTable table = panel.getCloudLibrariesTable();
    table.setRowSelectionInterval(0, 0);

    assertThat(table.getSelectionModel().isSelectedIndex(0)).isTrue();

    TableModel model = table.getModel();
    assertThat(model.getRowCount()).isEqualTo(1);
    assertThat(model.getValueAt(0, 0)).isEqualTo(library);
    assertThat((Boolean) model.getValueAt(0, 1)).isFalse();

    assertDetailsShownForLibrary(panel.getDetailsPanel(), LIBRARY_1, JAVA_CLIENT_1);
  }

  @Test
  public void getPanel_withMultipleLibraries_noSelection_hasCheckboxesAndEmptyDetails() {
    CloudLibrary library1 = LIBRARY_1.toCloudLibrary();
    CloudLibrary library2 = LIBRARY_2.toCloudLibrary();

    // The given list should be reordered by the name's natural order, placing library1 first.
    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(
            ImmutableList.of(library2, library1), testFixture.getProject());
    JTable table = panel.getCloudLibrariesTable();

    assertThat(table.getSelectionModel().isSelectionEmpty()).isTrue();

    TableModel model = table.getModel();
    assertThat(model.getRowCount()).isEqualTo(2);
    assertThat(model.getValueAt(0, 0)).isEqualTo(library1);
    assertThat(model.getValueAt(1, 0)).isEqualTo(library2);
    assertThat((Boolean) model.getValueAt(0, 1)).isFalse();
    assertThat((Boolean) model.getValueAt(1, 1)).isFalse();

    assertDetailsEmpty(panel.getDetailsPanel());
  }

  @Test
  public void getPanel_withMultipleLibraries_selectedLibrary_hasCheckboxesAndPopulatedDetails() {
    CloudLibrary library1 = LIBRARY_1.toCloudLibrary();
    CloudLibrary library2 = LIBRARY_2.toCloudLibrary();

    // The given list should be reordered by the name's natural order, placing library1 first.
    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(
            ImmutableList.of(library2, library1), testFixture.getProject());
    JTable table = panel.getCloudLibrariesTable();

    // Checks the second row's checkbox, which should be library2.
    checkAddLibraryCheckbox(table, 1);

    assertThat(table.getSelectionModel().isSelectedIndex(0)).isFalse();
    assertThat(table.getSelectionModel().isSelectedIndex(1)).isTrue();

    TableModel model = table.getModel();
    assertThat(model.getRowCount()).isEqualTo(2);
    assertThat(model.getValueAt(0, 0)).isEqualTo(library1);
    assertThat(model.getValueAt(1, 0)).isEqualTo(library2);
    assertThat((Boolean) model.getValueAt(0, 1)).isFalse();
    assertThat((Boolean) model.getValueAt(1, 1)).isTrue();

    assertDetailsShownForLibrary(panel.getDetailsPanel(), LIBRARY_2, JAVA_CLIENT_2);
  }

  @Test
  public void getPanel_withMultipleLibraries_multipleSelected_hasCheckboxesAndPopulatedDetails() {
    CloudLibrary library1 = LIBRARY_1.toCloudLibrary();
    CloudLibrary library2 = LIBRARY_2.toCloudLibrary();

    // The given list should be reordered by the name's natural order, placing library1 first.
    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(
            ImmutableList.of(library2, library1), testFixture.getProject());
    JTable table = panel.getCloudLibrariesTable();

    checkAddLibraryCheckbox(table, 1);
    checkAddLibraryCheckbox(table, 0);

    assertThat(table.getSelectionModel().isSelectedIndex(0)).isTrue();
    assertThat(table.getSelectionModel().isSelectedIndex(1)).isFalse();

    TableModel model = table.getModel();
    assertThat(model.getRowCount()).isEqualTo(2);
    assertThat(model.getValueAt(0, 0)).isEqualTo(library1);
    assertThat(model.getValueAt(1, 0)).isEqualTo(library2);
    assertThat((Boolean) model.getValueAt(0, 1)).isTrue();
    assertThat((Boolean) model.getValueAt(1, 1)).isTrue();

    assertDetailsShownForLibrary(panel.getDetailsPanel(), LIBRARY_1, JAVA_CLIENT_1);
  }

  @Test
  public void getPanel_withAvailableBomVersions_populatesBomVersionsInReverseOrder() {
    // TODO (eshaul): remove once feature is released
    when(pluginInfoService.shouldEnable(GctFeature.BOM)).thenReturn(true);

    when(mavenService.getBomVersions())
        .thenReturn(
            ImmutableList.of(newTestVersion("v0"), newTestVersion("v1"), newTestVersion("v2")));

    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(ImmutableList.of(), testFixture.getProject());

    JComboBox<Version> bomComboBox = panel.getBomComboBox();

    assertThat(panel.getBomSelectorLabel().isVisible()).isTrue();
    assertThat(panel.getBomComboBox().isVisible()).isTrue();
    assertThat(bomComboBox.getItemCount()).isEqualTo(3);

    assertThat(bomComboBox.getItemAt(0).toString()).isEqualTo("v2");
    assertThat(bomComboBox.getItemAt(1).toString()).isEqualTo("v1");
    assertThat(bomComboBox.getItemAt(2).toString()).isEqualTo("v0");
  }

  @Test
  public void getPanel_withManyAvailableBomVersions_limitsNumBomVersions() {
    // TODO (eshaul): remove once feature is released
    when(pluginInfoService.shouldEnable(GctFeature.BOM)).thenReturn(true);

    List<Version> versions = Lists.newArrayList();
    for (int i = 0; i < 20; i++) {
      versions.add(newTestVersion("v" + i));
    }

    when(mavenService.getBomVersions()).thenReturn(versions);

    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(ImmutableList.of(), testFixture.getProject());

    assertThat(panel.getBomComboBox().getItemCount()).isEqualTo(5);
  }

  @Test
  public void getPanel_withNoAvailableBomVersions_hidesBomUi() {
    // TODO (eshaul): remove once feature is released
    when(pluginInfoService.shouldEnable(GctFeature.BOM)).thenReturn(true);

    when(mavenService.getBomVersions()).thenReturn(ImmutableList.of());

    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(ImmutableList.of(), testFixture.getProject());

    assertThat(panel.getBomSelectorLabel().isVisible()).isFalse();
    assertThat(panel.getBomComboBox().isVisible()).isFalse();
  }

  @Test
  public void getPanel_withVersionReturnedFromBomQuery_displaysVersionFromBom() {
    // TODO (eshaul): remove once feature is released
    when(pluginInfoService.shouldEnable(GctFeature.BOM)).thenReturn(true);

    String libVersion = "9.9-alpha";
    when(mavenService.getManagedDependencyVersion(any(), anyString()))
        .thenReturn(Optional.of(libVersion));

    CloudLibrary cloudLibrary = LIBRARY_1.toCloudLibrary();
    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(ImmutableList.of(cloudLibrary), testFixture.getProject());

    panel
        .getDetailsPanel()
        .setCloudLibrary(cloudLibrary, BOM_VERSION, panel.getApiManagementMap().get(cloudLibrary));

    assertThat(panel.getDetailsPanel().getVersionLabel().getText())
        .isEqualTo("Version: " + libVersion);
  }

  @Test
  public void getPanel_withNoVersionReturnedFromBomQuery_fallsBackToAndDisplaysStaticVersion() {
    // TODO (eshaul): remove once feature is released
    when(pluginInfoService.shouldEnable(GctFeature.BOM)).thenReturn(true);

    CloudLibrary cloudLibrary = LIBRARY_1.toCloudLibrary();
    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(ImmutableList.of(cloudLibrary), testFixture.getProject());

    panel
        .getDetailsPanel()
        .setCloudLibrary(
            cloudLibrary, null /*bomVersion*/, panel.getApiManagementMap().get(cloudLibrary));

    assertThat(panel.getDetailsPanel().getVersionLabel().getText())
        .isEqualTo("Version: " + JAVA_CLIENT_MAVEN_COORDS_1.version());
  }

  @Test
  public void getPanel_withBomAndNoDependencyVersion_displaysEmptyVersion() {
    // TODO (eshaul): remove once feature is released
    when(pluginInfoService.shouldEnable(GctFeature.BOM)).thenReturn(true);

    when(mavenService.getManagedDependencyVersion(any(), anyString())).thenReturn(Optional.empty());

    CloudLibrary cloudLibrary = LIBRARY_1.toCloudLibrary();
    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(ImmutableList.of(cloudLibrary), testFixture.getProject());

    panel
        .getDetailsPanel()
        .setCloudLibrary(cloudLibrary, BOM_VERSION, panel.getApiManagementMap().get(cloudLibrary));

    assertThat(panel.getDetailsPanel().getVersionLabel().getText()).isEqualTo("");
    assertThat(panel.getDetailsPanel().getVersionLabel().getIcon()).isNull();
  }

  @Test
  public void getSelectedModule_withNoneSelected_returnsDefaultModule() {
    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(ImmutableList.of(), testFixture.getProject());

    // The order is determined by the call to Project.getSortedModules(), which returns module2
    // before module1. It is deterministic, though, so there is no issue testing for direct equality
    // here.
    assertThat(panel.getSelectedModule()).isEqualTo(module2);
  }

  @Test
  public void getSelectedModule_withModuleSelected_returnsSelectedModule() {
    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(ImmutableList.of(), testFixture.getProject());

    panel.getModulesComboBox().setSelectedModule(module1);

    assertThat(panel.getSelectedModule()).isEqualTo(module1);
  }

  @Test
  public void getSelectedModule_withNoModulesInProject_returnsNull() {
    // Disposes the modules created by the CloudToolsRule.
    ModuleManager moduleManager = ModuleManager.getInstance(testFixture.getProject());
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              moduleManager.disposeModule(module1);
              moduleManager.disposeModule(module2);
            });

    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(ImmutableList.of(), testFixture.getProject());

    assertThat(panel.getSelectedModule()).isNull();
  }

  @Test
  public void getSelectedLibraries_withNoneSelected_returnsEmptySet() {
    CloudLibrary library1 = LIBRARY_1.toCloudLibrary();
    CloudLibrary library2 = LIBRARY_2.toCloudLibrary();

    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(
            ImmutableList.of(library1, library2), testFixture.getProject());

    assertThat(panel.getSelectedLibraries()).isEmpty();
  }

  @Test
  public void getSelectedLibraries_withOneFocused_returnsEmptySet() {
    CloudLibrary library1 = LIBRARY_1.toCloudLibrary();
    CloudLibrary library2 = LIBRARY_2.toCloudLibrary();

    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(
            ImmutableList.of(library1, library2), testFixture.getProject());
    JTable table = panel.getCloudLibrariesTable();

    table.setRowSelectionInterval(0, 0);

    assertThat(panel.getSelectedLibraries()).isEmpty();
  }

  @Test
  public void getSelectedLibraries_withOneSelected_returnsLibrary() {
    CloudLibrary library1 = LIBRARY_1.toCloudLibrary();
    CloudLibrary library2 = LIBRARY_2.toCloudLibrary();

    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(
            ImmutableList.of(library1, library2), testFixture.getProject());
    JTable table = panel.getCloudLibrariesTable();

    checkAddLibraryCheckbox(table, 0);

    assertThat(panel.getSelectedLibraries()).containsExactly(library1);
  }

  @Test
  public void getSelectedLibraries_withSomeSelected_returnsLibraries() {
    CloudLibrary library1 = LIBRARY_1.toCloudLibrary();
    CloudLibrary library2 = LIBRARY_2.toCloudLibrary();

    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(
            ImmutableList.of(library1, library2), testFixture.getProject());
    JTable table = panel.getCloudLibrariesTable();

    checkAddLibraryCheckbox(table, 0);
    checkAddLibraryCheckbox(table, 1);

    assertThat(panel.getSelectedLibraries()).containsExactly(library1, library2);
  }

  @Test
  public void getApisToEnable_shouldEnableSelectedLibraryByDefault() {
    CloudLibrary library = LIBRARY_1.toCloudLibrary();

    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(ImmutableList.of(library), testFixture.getProject());
    JTable table = panel.getCloudLibrariesTable();
    checkAddLibraryCheckbox(table, 0);

    Set<CloudLibrary> apisToEnable = panel.getApisToEnable();
    assertThat(apisToEnable).containsExactly(library);
  }

  @Test
  public void
      getApisToEnable_withAllLibrariesChecked_AndNoShouldEnableChecked_returnsNoneEnabled() {
    CloudLibrary library1 = LIBRARY_1.toCloudLibrary();
    CloudLibrary library2 = LIBRARY_2.toCloudLibrary();

    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(
            ImmutableList.of(library1, library2), testFixture.getProject());
    JTable table = panel.getCloudLibrariesTable();

    checkAddLibraryCheckbox(table, 0);
    checkAddLibraryCheckbox(table, 1);

    Map<CloudLibrary, CloudApiManagementSpec> apiManagementMap = panel.getApiManagementMap();

    panel.getDetailsPanel().setCloudLibrary(library1, BOM_VERSION, apiManagementMap.get(library1));
    checkEnableCheckbox(panel.getDetailsPanel().getEnableApiCheckbox(), false);

    panel.getDetailsPanel().setCloudLibrary(library2, BOM_VERSION, apiManagementMap.get(library2));
    checkEnableCheckbox(panel.getDetailsPanel().getEnableApiCheckbox(), false);

    assertThat(panel.getApisToEnable()).isEmpty();
  }

  @Test
  public void
      getApisToEnable_withAllLibrariesChecked_AndSomeShouldEnableChecked_returnsSomeEnabled() {
    CloudLibrary library1 = LIBRARY_1.toCloudLibrary();
    CloudLibrary library2 = LIBRARY_2.toCloudLibrary();

    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(
            ImmutableList.of(library1, library2), testFixture.getProject());
    JTable table = panel.getCloudLibrariesTable();

    checkAddLibraryCheckbox(table, 0);
    checkAddLibraryCheckbox(table, 1);

    Map<CloudLibrary, CloudApiManagementSpec> apiManagementMap = panel.getApiManagementMap();

    panel.getDetailsPanel().setCloudLibrary(library1, BOM_VERSION, apiManagementMap.get(library1));
    checkEnableCheckbox(panel.getDetailsPanel().getEnableApiCheckbox(), false);

    panel.getDetailsPanel().setCloudLibrary(library2, BOM_VERSION, apiManagementMap.get(library2));
    checkEnableCheckbox(panel.getDetailsPanel().getEnableApiCheckbox(), true);

    assertThat(panel.getApisToEnable()).containsExactly(library2);
  }

  @Test
  public void
      getApisToEnable_withNoLibrariesChecked_AndAllShouldEnableChecked_returnsNoneEnabled() {
    CloudLibrary library1 = LIBRARY_1.toCloudLibrary();
    CloudLibrary library2 = LIBRARY_2.toCloudLibrary();

    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(
            ImmutableList.of(library1, library2), testFixture.getProject());

    Map<CloudLibrary, CloudApiManagementSpec> apiManagementMap = panel.getApiManagementMap();

    panel.getDetailsPanel().setCloudLibrary(library1, BOM_VERSION, apiManagementMap.get(library1));
    checkEnableCheckbox(panel.getDetailsPanel().getEnableApiCheckbox(), true);

    panel.getDetailsPanel().setCloudLibrary(library2, BOM_VERSION, apiManagementMap.get(library2));
    checkEnableCheckbox(panel.getDetailsPanel().getEnableApiCheckbox(), true);

    assertThat(panel.getApisToEnable()).isEmpty();
  }

  @Test
  public void getManagementUI_withLibraryAndProjectUnselected_isDisabled() {
    CloudLibrary library = LIBRARY_1.toCloudLibrary();

    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(ImmutableList.of(library), testFixture.getProject());

    panel
        .getDetailsPanel()
        .setCloudLibrary(library, BOM_VERSION, panel.getApiManagementMap().get(library));

    assertThat(panel.getDetailsPanel().getEnableApiCheckbox().isEnabled()).isFalse();
    assertThat(panel.getDetailsPanel().getManagementInfoPanel().isVisible()).isTrue();
  }

  @Test
  public void getManagementUI_withLibraryAndProjectSelected_isEnabled() {
    CloudLibrary library = LIBRARY_1.toCloudLibrary();

    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(ImmutableList.of(library), testFixture.getProject());
    JTable table = panel.getCloudLibrariesTable();

    checkAddLibraryCheckbox(table, 0);
    panel
        .getDetailsPanel()
        .setCloudLibrary(library, BOM_VERSION, panel.getApiManagementMap().get(library));

    CloudProject cloudProject = CloudProject.create("name", "id", "user");
    ProjectSelector projectSelector = panel.getProjectSelector();
    projectSelector.setSelectedProject(cloudProject);
    projectSelector
        .getProjectSelectionListeners()
        .forEach(listener -> listener.projectSelected(cloudProject));

    assertThat(panel.getDetailsPanel().getEnableApiCheckbox().isEnabled()).isTrue();
    assertThat(panel.getDetailsPanel().getManagementInfoPanel().isVisible()).isFalse();
  }

  @Test
  public void getEnableCheckbox_withNoLibrarySelected_isNotSelected() {
    CloudLibrary library = LIBRARY_1.toCloudLibrary();

    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(ImmutableList.of(library), testFixture.getProject());

    Map<CloudLibrary, CloudApiManagementSpec> apiManagementMap = panel.getApiManagementMap();
    panel.getDetailsPanel().setCloudLibrary(library, BOM_VERSION, apiManagementMap.get(library));

    assertThat(panel.getDetailsPanel().getEnableApiCheckbox().isSelected()).isFalse();
  }

  /**
   * Forcibly checks the checkbox in the given {@link JTable} at the given row number.
   *
   * @param table the {@link JTable} to check the checkbox in
   * @param row the index of the row to check the checkbox in
   */
  private static void checkAddLibraryCheckbox(JTable table, int row) {
    table.setRowSelectionInterval(row, row);
    table.setValueAt(true, row, 1);
  }

  /**
   * Checks or unchecks the given checkbox. {@link ActionListener} does not respond to {@link
   * JCheckBox#setSelected(boolean)} so the attached listeners are manually invoked.
   *
   * @param checkbox the checkbox to check or uncheck
   * @param select whether to check or uncheck it
   */
  private static void checkEnableCheckbox(JCheckBox checkbox, boolean select) {
    checkbox.setSelected(select);
    ActionEvent e = new ActionEvent(checkbox, 1, "");
    Arrays.stream(checkbox.getListeners(ActionListener.class))
        .forEach(listener -> listener.actionPerformed(e));
  }

  /**
   * Asserts the given library details {@link JPanel} is empty.
   *
   * @param panel the {@link JPanel} for the details view to assert on
   */
  private static void assertDetailsEmpty(GoogleCloudApiDetailsPanel panel) {
    TestCloudLibrary emptyLibrary = TestCloudLibrary.createEmpty();
    assertDetailsShownForLibrary(panel, emptyLibrary, emptyLibrary.clients().get(0));
  }

  /**
   * Asserts the given {@link GoogleCloudApiDetailsPanel} shows the proper details for the given
   * {@link TestCloudLibrary} and {@link TestCloudLibraryClient}.
   *
   * @param panel the {@link GoogleCloudApiDetailsPanel} for the details view to assert on
   * @param library the {@link TestCloudLibrary} whose details should be shown. If using {@link
   *     TestCloudLibrary#createEmpty()}, this method will assert the corresponding fields are
   *     empty.
   * @param client the {@link TestCloudLibraryClient} whose details should be shown. If using {@link
   *     TestCloudLibraryClient#createEmpty()}, this method will assert the corresponding fields are
   *     empty.
   */
  private static void assertDetailsShownForLibrary(
      GoogleCloudApiDetailsPanel panel, TestCloudLibrary library, TestCloudLibraryClient client) {
    assertThat(panel.getNameLabel().getText()).isEqualTo(library.name());
    assertThat(panel.getDescriptionTextPane().getText()).isEqualTo(library.description());

    String expectedVersion =
        client.mavenCoordinates().version().isEmpty()
            ? ""
            : "Version: " + client.mavenCoordinates().version();
    assertThat(panel.getVersionLabel().getText()).isEqualTo(expectedVersion);

    Map<String, String> actualUrlMap = buildActualUrlMap(panel.getLinksTextPane().getText());
    Map<String, String> expectedUrlMap = buildExpectedUrlMap(library, client);
    assertThat(actualUrlMap).containsExactlyEntriesIn(expectedUrlMap);

    // TODO(nkibler): Consider refactoring the details panel to allow unit tests to inject fake
    // icons. Until then, this will always be null.
    assertThat(panel.getIcon().getIcon()).isNull();
  }

  /**
   * Builds the map of expected URLs in the links text pane for the given {@link TestCloudLibrary}
   * and {@link TestCloudLibraryClient}.
   *
   * @param library the {@link TestCloudLibrary} to build the expected links map for
   * @param client the {@link TestCloudLibraryClient} to build the expected links map for
   */
  private static Map<String, String> buildExpectedUrlMap(
      TestCloudLibrary library, TestCloudLibraryClient client) {
    ImmutableMap.Builder<String, String> mapBuilder = ImmutableMap.builder();
    if (!library.documentation().isEmpty()) {
      mapBuilder.put("Documentation", library.documentation());
    }
    if (!client.source().isEmpty()) {
      mapBuilder.put("Source", client.source());
    }
    if (!client.apireference().isEmpty()) {
      mapBuilder.put("API Reference", client.apireference());
    }
    return mapBuilder.build();
  }

  /**
   * Builds the map of actual URLs contained in the given text, keyed by the displayed link text and
   * whose values are the link URLs.
   *
   * <p>The given text is assumed to be an HTML-formatted string that uses {@literal <a>} tags to
   * format URLs.
   *
   * @param text the HTML-formatted string to build the URL map for
   */
  private static Map<String, String> buildActualUrlMap(String text) {
    Pattern pattern = Pattern.compile("<a href=\"([^\"]*)\">([a-zA-Z\\p{Space}]*)</a>");
    Matcher matcher = pattern.matcher(text);
    ImmutableMap.Builder<String, String> mapBuilder = ImmutableMap.builder();
    while (matcher.find()) {
      mapBuilder.put(matcher.group(2), matcher.group(1));
    }
    return mapBuilder.build();
  }

  private static TestVersion newTestVersion(String name) {
    return new TestVersion(name);
  }

  private static class TestVersion implements Version {
    private final String name;

    TestVersion(String name) {
      this.name = name;
    }

    @Override
    public int compareTo(@NotNull Version that) {
      return this.toString().compareTo(that.toString());
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
