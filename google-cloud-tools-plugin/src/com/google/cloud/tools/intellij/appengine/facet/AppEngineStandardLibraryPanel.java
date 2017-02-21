/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.facet;

import static java.util.stream.Collectors.toSet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import org.jetbrains.annotations.NotNull;

import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 * UI panel for configuring App Engine standard environment libraries.
 */
public class AppEngineStandardLibraryPanel {

  private JPanel mainPanel;
  private JPanel libraryPanel;

  private JCheckBox servletApiCheckbox;
  private JCheckBox jstlCheckbox;
  private JCheckBox appEngineApiCheckBox;
  private JCheckBox endpointsCheckBox;
  private JCheckBox objectifyCheckBox;

  private List<JCheckBox> libraries = ImmutableList.of(servletApiCheckbox, jstlCheckbox,
      appEngineApiCheckBox, endpointsCheckBox, objectifyCheckBox);

  private boolean enabled = true;

  public AppEngineStandardLibraryPanel(boolean enabled) {
    this.enabled = enabled;
    libraryPanel.setVisible(enabled);

    // The Servlet API is provided by the AE standard runtime. So we are enabling it by
    // default.
    servletApiCheckbox.setSelected(true);

    // Objectify and Endpoints are dependencies of the App Engine API.
    objectifyCheckBox.setSelected(false);
    endpointsCheckBox.setSelected(false);
    appEngineApiCheckBox.addItemListener(event -> {
      JCheckBox checkbox = (JCheckBox) event.getItem();

      if (!checkbox.isSelected()) {
        objectifyCheckBox.setSelected(false);
        endpointsCheckBox.setSelected(false);
      }
    });
    objectifyCheckBox.addItemListener(this::listenAppEngineApiDependencyCheckbox);
    endpointsCheckBox.addItemListener(this::listenAppEngineApiDependencyCheckbox);
  }

  public Set<AppEngineStandardMavenLibrary> getSelectedLibraries() {
    return libraries.stream()
        .filter(JCheckBox::isSelected)
        .map(
            library ->
                AppEngineStandardMavenLibrary.getLibraryByDisplayName(library.getText()).get())
        .collect(toSet());
  }

  public void setSelectedLibraries(Set<AppEngineStandardMavenLibrary> mavenLibraries) {
    Set<String> availableLibraryNames = mavenLibraries.stream()
        .map(AppEngineStandardMavenLibrary::getDisplayName)
        .collect(toSet());

    libraries.stream()
        .filter(library -> availableLibraryNames.contains(library.getText()))
        .forEach(checkbox -> checkbox.setSelected(true));
  }

  public void selectLibraryByName(String name) {
    libraries.stream()
        .filter(library -> library.getText().equals(name))
        .forEach(checkbox -> checkbox.setSelected(true));
  }

  public void toggleLibrary(AppEngineStandardMavenLibrary library, boolean select) {
    libraries.stream()
        .filter(checkbox -> checkbox.getText().equals(library.getDisplayName()))
        .forEach(checkbox -> checkbox.setSelected(select));
  }

  @NotNull
  public JPanel getComponent() {
    return libraryPanel;
  }

  public boolean isEnabled() {
    return enabled;
  }

  @VisibleForTesting
  JCheckBox getLibraryCheckbox(String name) {
    return libraries.stream()
        .filter(library -> library.getText().equals(name))
        .findAny()
        .orElse(null);
  }

  @VisibleForTesting
  List<JCheckBox> getLibraries() {
    return libraries;
  }

  private void listenAppEngineApiDependencyCheckbox(ItemEvent event) {
    JCheckBox appEngineApiDependencyCheckbox = (JCheckBox) event.getSource();

    if (appEngineApiDependencyCheckbox.isSelected()) {
      appEngineApiCheckBox.setSelected(true);
    }
  }
}
