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

import com.google.auto.value.AutoValue;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.apis.TestCloudLibrary;
import com.google.cloud.tools.intellij.testing.apis.TestCloudLibrary.TestCloudLibraryClient;
import com.google.cloud.tools.intellij.testing.apis.TestCloudLibrary.TestCloudLibraryClientMavenCoordinates;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Correspondence;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBScrollPane;
import java.awt.Component;
import java.awt.event.FocusEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link GoogleCloudApiSelectorPanel}. */
@RunWith(JUnit4.class)
public final class GoogleCloudApiSelectorPanelTest {

  private static final TestCloudLibraryClientMavenCoordinates JAVA_CLIENT_MAVEN_COORDS_1 =
      TestCloudLibraryClientMavenCoordinates.create("java", "client-1", "1.0.0");
  private static final TestCloudLibraryClientMavenCoordinates JAVA_CLIENT_MAVEN_COORDS_2 =
      TestCloudLibraryClientMavenCoordinates.create("java", "client-2", "2.0.0");
  private static final TestCloudLibraryClient JAVA_CLIENT_1 =
      TestCloudLibraryClient.create(
          "java", "API Ref 1", "alpha", "Source 1", "Lang Level 1", JAVA_CLIENT_MAVEN_COORDS_1);
  private static final TestCloudLibraryClient JAVA_CLIENT_2 =
      TestCloudLibraryClient.create(
          "java", "API Ref 2", "beta", "Source 2", "Lang Level 2", JAVA_CLIENT_MAVEN_COORDS_2);

  private static final TestCloudLibrary LIBRARY_1 =
      TestCloudLibrary.create(
          "Library 1",
          "ID 1",
          "Docs Link 1",
          "Description 1",
          "Icon Link 1",
          ImmutableList.of(JAVA_CLIENT_1));
  private static final TestCloudLibrary LIBRARY_2 =
      TestCloudLibrary.create(
          "Library 2",
          "ID 2",
          "Docs Link 2",
          "Description 2",
          "Icon Link 2",
          ImmutableList.of(JAVA_CLIENT_2));

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Test
  public void getPanel_withOneLibrary_noSelection_hasCheckboxAndEmptyDetails() {
    CloudLibrary library = LIBRARY_1.toCloudLibrary();

    GoogleCloudApiSelectorPanel panel = new GoogleCloudApiSelectorPanel(ImmutableList.of(library));

    List<JPanel> checkboxPanels = getCheckboxPanels(panel);
    ExpectedCheckboxAttributes expected =
        ExpectedCheckboxAttributes.create(
            LIBRARY_1.name(), /* isChecked= */ false, /* hasBorder= */ false);
    assertThat(checkboxPanels)
        .comparingElementsUsing(new CheckboxPanelCorrespondence())
        .containsExactlyElementsIn(ImmutableList.of(expected));

    JPanel detailsPanel = getDetailsPanel(panel);
    assertDetailsEmpty(detailsPanel);
  }

  @Test
  public void getPanel_withOneLibrary_selectedLibrary_hasCheckboxAndPopulatedDetails() {
    CloudLibrary library = LIBRARY_1.toCloudLibrary();

    GoogleCloudApiSelectorPanel panel = new GoogleCloudApiSelectorPanel(ImmutableList.of(library));

    List<JPanel> checkboxPanels = getCheckboxPanels(panel);
    Optional<JBCheckBox> checkbox = findCheckbox(checkboxPanels, library);

    clickCheckbox(checkbox.get(), checkboxPanels);

    ExpectedCheckboxAttributes expected =
        ExpectedCheckboxAttributes.create(
            LIBRARY_1.name(), /* isChecked= */ true, /* hasBorder= */ true);
    assertThat(checkboxPanels)
        .comparingElementsUsing(new CheckboxPanelCorrespondence())
        .containsExactlyElementsIn(ImmutableList.of(expected));

    JPanel detailsPanel = getDetailsPanel(panel);
    assertDetailsShownForLibrary(detailsPanel, LIBRARY_1, JAVA_CLIENT_1);
  }

  @Test
  public void getPanel_withOneLibrary_focusedLibrary_hasUncheckedCheckboxAndPopulatedDetails() {
    CloudLibrary library = LIBRARY_1.toCloudLibrary();

    GoogleCloudApiSelectorPanel panel = new GoogleCloudApiSelectorPanel(ImmutableList.of(library));

    List<JPanel> checkboxPanels = getCheckboxPanels(panel);
    Optional<JBCheckBox> checkbox = findCheckbox(checkboxPanels, library);

    forceFocus(checkbox.get(), checkboxPanels);

    ExpectedCheckboxAttributes expected =
        ExpectedCheckboxAttributes.create(
            LIBRARY_1.name(), /* isChecked= */ false, /* hasBorder= */ true);
    assertThat(checkboxPanels)
        .comparingElementsUsing(new CheckboxPanelCorrespondence())
        .containsExactlyElementsIn(ImmutableList.of(expected));

    JPanel detailsPanel = getDetailsPanel(panel);
    assertDetailsShownForLibrary(detailsPanel, LIBRARY_1, JAVA_CLIENT_1);
  }

  @Test
  public void getPanel_withMultipleLibraries_noSelection_hasCheckboxesAndEmptyDetails() {
    CloudLibrary library1 = LIBRARY_1.toCloudLibrary();
    CloudLibrary library2 = LIBRARY_2.toCloudLibrary();

    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(ImmutableList.of(library1, library2));

    List<JPanel> checkboxPanels = getCheckboxPanels(panel);
    ExpectedCheckboxAttributes expected1 =
        ExpectedCheckboxAttributes.create(
            LIBRARY_1.name(), /* isChecked= */ false, /* hasBorder= */ false);
    ExpectedCheckboxAttributes expected2 =
        ExpectedCheckboxAttributes.create(
            LIBRARY_2.name(), /* isChecked= */ false, /* hasBorder= */ false);
    assertThat(checkboxPanels)
        .comparingElementsUsing(new CheckboxPanelCorrespondence())
        .containsExactlyElementsIn(ImmutableList.of(expected1, expected2));

    JPanel detailsPanel = getDetailsPanel(panel);
    assertDetailsEmpty(detailsPanel);
  }

  @Test
  public void getPanel_withMultipleLibraries_selectedLibrary_hasCheckboxesAndPopulatedDetails() {
    CloudLibrary library1 = LIBRARY_1.toCloudLibrary();
    CloudLibrary library2 = LIBRARY_2.toCloudLibrary();

    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(ImmutableList.of(library1, library2));

    List<JPanel> checkboxPanels = getCheckboxPanels(panel);
    Optional<JBCheckBox> checkbox = findCheckbox(checkboxPanels, library2);

    clickCheckbox(checkbox.get(), checkboxPanels);

    ExpectedCheckboxAttributes expected1 =
        ExpectedCheckboxAttributes.create(
            LIBRARY_1.name(), /* isChecked= */ false, /* hasBorder= */ false);
    ExpectedCheckboxAttributes expected2 =
        ExpectedCheckboxAttributes.create(
            LIBRARY_2.name(), /* isChecked= */ true, /* hasBorder= */ true);
    assertThat(checkboxPanels)
        .comparingElementsUsing(new CheckboxPanelCorrespondence())
        .containsExactlyElementsIn(ImmutableList.of(expected1, expected2));

    JPanel detailsPanel = getDetailsPanel(panel);
    assertDetailsShownForLibrary(detailsPanel, LIBRARY_2, JAVA_CLIENT_2);
  }

  @Test
  public void getPanel_withMultipleLibraries_multipleSelected_hasCheckboxesAndPopulatedDetails() {
    CloudLibrary library1 = LIBRARY_1.toCloudLibrary();
    CloudLibrary library2 = LIBRARY_2.toCloudLibrary();

    GoogleCloudApiSelectorPanel panel =
        new GoogleCloudApiSelectorPanel(ImmutableList.of(library1, library2));

    List<JPanel> checkboxPanels = getCheckboxPanels(panel);
    Optional<JBCheckBox> checkbox1 = findCheckbox(checkboxPanels, library1);
    Optional<JBCheckBox> checkbox2 = findCheckbox(checkboxPanels, library2);

    clickCheckbox(checkbox2.get(), checkboxPanels);
    clickCheckbox(checkbox1.get(), checkboxPanels);

    ExpectedCheckboxAttributes expected1 =
        ExpectedCheckboxAttributes.create(
            LIBRARY_1.name(), /* isChecked= */ true, /* hasBorder= */ true);
    ExpectedCheckboxAttributes expected2 =
        ExpectedCheckboxAttributes.create(
            LIBRARY_2.name(), /* isChecked= */ true, /* hasBorder= */ false);
    assertThat(checkboxPanels)
        .comparingElementsUsing(new CheckboxPanelCorrespondence())
        .containsExactlyElementsIn(ImmutableList.of(expected1, expected2));

    JPanel detailsPanel = getDetailsPanel(panel);
    assertDetailsShownForLibrary(detailsPanel, LIBRARY_1, JAVA_CLIENT_1);
  }

  /**
   * Clicks the given {@link JBCheckBox} and forces a {@link FocusEvent} to be triggered.
   *
   * @param checkbox the {@link JBCheckBox} to click
   * @param otherPanels the list of other checkbox {@link JPanel JPanels}. These are required to
   *     force a {@link FocusEvent#FOCUS_LOST} event to properly simulate how focus works.
   */
  private static void clickCheckbox(JBCheckBox checkbox, List<JPanel> otherPanels) {
    checkbox.doClick();

    // Force the focus events since they won't be triggered outside the context of a window.
    forceFocus(checkbox, otherPanels);
  }

  /**
   * Forces a {@link FocusEvent} for the given {@link JBCheckBox}'s parent component.
   *
   * @param checkbox the {@link JBCheckBox} to force a focus event for
   * @param otherPanels the list of other checkbox {@link JPanel JPanels}. These are required to
   *     force a {@link FocusEvent#FOCUS_LOST} event to properly simulate how focus works.
   */
  private static void forceFocus(JBCheckBox checkbox, List<JPanel> otherPanels) {
    otherPanels.forEach(
        panel -> {
          FocusEvent lostEvent = new FocusEvent(panel, FocusEvent.FOCUS_LOST);
          Arrays.stream(panel.getFocusListeners())
              .forEach(listener -> listener.focusLost(lostEvent));
        });

    Component parent = checkbox.getParent();
    FocusEvent gainedEvent = new FocusEvent(parent, FocusEvent.FOCUS_GAINED);
    Arrays.stream(parent.getFocusListeners())
        .forEach(listener -> listener.focusGained(gainedEvent));
  }

  /**
   * Optionally returns the {@link JBCheckBox} in the given list of {@link JPanel JPanels} whose
   * label matches the given {@link CloudLibrary}'s name.
   *
   * @param panels the list of {@link JPanel JPanels} to search
   * @param library the {@link CloudLibrary} to find the matching {@link JBCheckBox} for
   */
  private static Optional<JBCheckBox> findCheckbox(List<JPanel> panels, CloudLibrary library) {
    return panels
        .stream()
        .filter(
            panel -> {
              Component[] components = panel.getComponents();
              JLabel label = (JLabel) components[1];
              return Objects.equals(label.getText(), library.getName());
            })
        .findFirst()
        .map(panel -> (JBCheckBox) panel.getComponents()[0]);
  }

  /**
   * Asserts the given library details {@link JPanel} is empty.
   *
   * @param panel the {@link JPanel} for the details view to assert on
   */
  private static void assertDetailsEmpty(JPanel panel) {
    TestCloudLibrary emptyLibrary = TestCloudLibrary.createEmpty();
    assertDetailsShownForLibrary(panel, emptyLibrary, emptyLibrary.clients().get(0));
  }

  /**
   * Asserts the given {@link JPanel} shows the proper details for the given {@link
   * TestCloudLibrary} and {@link TestCloudLibraryClient}.
   *
   * @param panel the {@link JPanel} for the details view to assert on
   * @param library the {@link TestCloudLibrary} whose details should be shown. If using {@link
   *     TestCloudLibrary#createEmpty()}, this method will assert the corresponding fields are
   *     empty.
   * @param client the {@link TestCloudLibraryClient} whose details should be shown. If using {@link
   *     TestCloudLibraryClient#createEmpty()}, this method will assert the corresponding fields are
   *     empty.
   */
  private static void assertDetailsShownForLibrary(
      JPanel panel, TestCloudLibrary library, TestCloudLibraryClient client) {
    Component[] components = panel.getComponents();
    JLabel icon = (JLabel) components[0];
    JLabel nameLabel = (JLabel) components[1];
    JLabel versionLabel = (JLabel) components[2];
    JLabel statusLabel = (JLabel) components[3];
    JTextPane descriptionTextPane = (JTextPane) ((JPanel) components[4]).getComponents()[0];
    JTextPane linksTextPane = (JTextPane) ((JPanel) components[5]).getComponents()[0];

    assertThat(nameLabel.getText()).isEqualTo(library.name());
    assertThat(descriptionTextPane.getText()).isEqualTo(library.description());

    String expectedVersion =
        client.mavenCoordinates().version().isEmpty()
            ? ""
            : "Version: " + client.mavenCoordinates().version();
    assertThat(versionLabel.getText()).isEqualTo(expectedVersion);

    String expectedStatus = client.launchStage().isEmpty() ? "" : "Status: " + client.launchStage();
    assertThat(statusLabel.getText()).isEqualTo(expectedStatus);

    Map<String, String> actualUrlMap = buildActualUrlMap(linksTextPane.getText());
    Map<String, String> expectedUrlMap = buildExpectedUrlMap(library, client);
    assertThat(actualUrlMap).containsExactlyEntriesIn(expectedUrlMap);

    // TODO(nkibler): Consider refactoring the details panel to allow unit tests to inject fake
    // icons. Until then, this will always be null.
    assertThat(icon.getIcon()).isNull();
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

  /**
   * Returns the list of {@link JPanel JPanels} that represent the API library checkboxes in the
   * given {@link GoogleCloudApiSelectorPanel}.
   *
   * @param panel the {@link GoogleCloudApiSelectorPanel} to return the checkbox panels for
   */
  private static List<JPanel> getCheckboxPanels(GoogleCloudApiSelectorPanel panel) {
    Component[] panelChildren = panel.getPanel().getComponents();
    assertThat(panelChildren).hasLength(1);

    JBSplitter splitter = (JBSplitter) panelChildren[0];
    JBScrollPane leftScrollPane = (JBScrollPane) splitter.getFirstComponent();
    JPanel checkboxPanel = (JPanel) leftScrollPane.getViewport().getView();
    return Arrays.stream(checkboxPanel.getComponents())
        .map(component -> (JPanel) component)
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Returns the {@link JPanel} that represents the API library details pane in the given {@link
   * GoogleCloudApiSelectorPanel}.
   *
   * @param panel the {@link GoogleCloudApiSelectorPanel} to return the details panel for
   */
  private static JPanel getDetailsPanel(GoogleCloudApiSelectorPanel panel) {
    Component[] panelChildren = panel.getPanel().getComponents();
    assertThat(panelChildren).hasLength(1);

    JBSplitter splitter = (JBSplitter) panelChildren[0];
    JBScrollPane rightScrollPane = (JBScrollPane) splitter.getSecondComponent();
    return (JPanel) rightScrollPane.getViewport().getView();
  }

  /**
   * The set of expected attributes for an API library checkbox.
   *
   * <p>This is designed to make comparison methods (using the {@link com.google.common.truth.Truth
   * Truth} library) easy-to-write, by leveraging the usage of the {@link
   * CheckboxPanelCorrespondence} class. All expected parameters of the checkbox panels should be
   * added to this holder class; the comparison logic should be added to the {@link
   * CheckboxPanelCorrespondence#compare(JPanel, ExpectedCheckboxAttributes)} method.
   */
  @AutoValue
  abstract static class ExpectedCheckboxAttributes {

    /** Returns a newly created instance for the given parameters. */
    static ExpectedCheckboxAttributes create(
        String libraryName, boolean isChecked, boolean hasBorder) {
      return new AutoValue_GoogleCloudApiSelectorPanelTest_ExpectedCheckboxAttributes(
          libraryName, isChecked, hasBorder);
    }

    /** The name of the expected library. */
    abstract String libraryName();

    /** {@code true} if the checkbox is expected to be checked, {@code false} otherwise. */
    abstract boolean isChecked();

    /**
     * {@code true} if the checkbox panel is expected to have a highlighted border visible, {@code
     * false} otherwise.
     */
    abstract boolean hasBorder();
  }

  /**
   * A custom {@link Correspondence} for checkbox {@link JPanel JPanels} and {@link
   * ExpectedCheckboxAttributes}.
   */
  private static final class CheckboxPanelCorrespondence
      extends Correspondence<JPanel, ExpectedCheckboxAttributes> {

    @Override
    public boolean compare(JPanel actual, ExpectedCheckboxAttributes expected) {
      try {
        Component[] components = actual.getComponents();
        JBCheckBox checkbox = (JBCheckBox) components[0];
        JLabel label = (JLabel) components[1];

        if (!Objects.equals(label.getText(), expected.libraryName())) {
          return false;
        }
        if (checkbox.isSelected() != expected.isChecked()) {
          return false;
        }
        if (expected.hasBorder()
            && (actual.getBorder() == null || !actual.getBorder().isBorderOpaque())) {
          return false;
        }
      } catch (Throwable throwable) {
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      return "has UI elements that match the parameters in";
    }
  }
}
