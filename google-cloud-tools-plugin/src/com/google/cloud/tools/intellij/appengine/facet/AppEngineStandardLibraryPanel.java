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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;
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
    appEngineApiCheckBox.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent event) {
        JCheckBox checkbox = (JCheckBox) event.getItem();

        if (!checkbox.isSelected()) {
          objectifyCheckBox.setSelected(false);
          endpointsCheckBox.setSelected(false);
        }
      }
    });
    objectifyCheckBox.addItemListener(new AppEngineApiDependencyCheckboxListener());
    endpointsCheckBox.addItemListener(new AppEngineApiDependencyCheckboxListener());
  }

  public Set<AppEngineStandardMavenLibrary> getSelectedLibraries() {
    return Sets.newHashSet(Collections2.filter(
        Collections2.transform(libraries,
            new Function<JCheckBox, AppEngineStandardMavenLibrary>() {
              @Nullable
              @Override
              public AppEngineStandardMavenLibrary apply(JCheckBox libraryCheckbox) {
                return libraryCheckbox.isSelected()
                    ? AppEngineStandardMavenLibrary
                    .getLibraryByDisplayName(libraryCheckbox.getText())
                    : null;
              }
            }),
        Predicates.notNull()
    ));
  }

  public void setSelectedLibraries(Set<AppEngineStandardMavenLibrary> mavenLibraries) {
    Collection<String> availableLibraryNames = Collections2.transform(mavenLibraries,
        new Function<AppEngineStandardMavenLibrary, String>() {
          @Nullable
          @Override
          public String apply(AppEngineStandardMavenLibrary mavenLibrary) {
            return mavenLibrary.getDisplayName();
          }
        });

    for (JCheckBox libraryCheckbox : libraries) {
      libraryCheckbox.setSelected(availableLibraryNames.contains(libraryCheckbox.getText()));
    }
  }

  public void selectLibraryByName(String name) {
    for (JCheckBox libraryCheckbox : libraries) {
      if (name.equals(libraryCheckbox.getText())) {
        libraryCheckbox.setSelected(true);
      }
    }
  }

  public void toggleLibrary(AppEngineStandardMavenLibrary library, boolean select) {
    for (JCheckBox libraryCheckbox : libraries) {
      if (libraryCheckbox.getText().equals(library.getDisplayName())) {
        libraryCheckbox.setSelected(select);
      }
    }
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
    for (JCheckBox libraryCheckbox : libraries) {
      if (name.equals(libraryCheckbox.getText())) {
        return libraryCheckbox;
      }
    }

    return null;
  }

  @VisibleForTesting
  List<JCheckBox> getLibraries() {
    return libraries;
  }

  private class AppEngineApiDependencyCheckboxListener implements ItemListener {

    @Override
    public void itemStateChanged(ItemEvent event) {
      JCheckBox appEngineApiDependencyCheckbox = (JCheckBox) event.getSource();

      if (appEngineApiDependencyCheckbox.isSelected()) {
        appEngineApiCheckBox.setSelected(true);
      }
    }
  }
}
