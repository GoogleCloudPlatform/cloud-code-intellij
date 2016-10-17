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
import com.google.common.collect.Sets;

import org.jetbrains.annotations.NotNull;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 * UI panel for configuring App Engine standard environment libraries.
 */
public class AppEngineStandardLibraryPanel {

  private JPanel mainPanel;
  private JPanel libraryPanel;

  private boolean enabled = true;

  public AppEngineStandardLibraryPanel(boolean enabled) {
    this.enabled = enabled;
  }

  public Set<AppEngineStandardMavenLibrary> getSelectedLibraries() {
    return Sets.newHashSet(Collections2.filter(
        Collections2.transform(Arrays.asList(libraryPanel.getComponents()),
            new Function<Component, AppEngineStandardMavenLibrary>() {
              @Nullable
              @Override
              public AppEngineStandardMavenLibrary apply(Component libraryCheckbox) {
                return libraryCheckbox instanceof JCheckBox
                    && ((JCheckBox) libraryCheckbox).isSelected()
                    ? AppEngineStandardMavenLibrary
                    .getLibraryByDisplayName(((JCheckBox) libraryCheckbox).getText())
                    : null;
              }
            }),
        Predicates.notNull()
    ));
  }

  public void setSelectedLibraries(Set<AppEngineStandardMavenLibrary> libraries) {
    Collection<String> availableLibraryNames = Collections2.transform(libraries,
        new Function<AppEngineStandardMavenLibrary, String>() {
          @Nullable
          @Override
          public String apply(AppEngineStandardMavenLibrary library) {
            return library.getDisplayName();
          }
        });

    for (Component libraryCheckbox : libraryPanel.getComponents()) {
      ((JCheckBox) libraryCheckbox)
          .setSelected(availableLibraryNames.contains(((JCheckBox) libraryCheckbox).getText()));
    }
  }

  public void selectLibraryByName(String name) {
    for (Component libraryCheckbox : libraryPanel.getComponents()) {
      if (name.equals(((JCheckBox) libraryCheckbox).getText())) {
        ((JCheckBox) libraryCheckbox).setSelected(true);
      }
    }
  }

  @VisibleForTesting
  JCheckBox getLibraryCheckbox(String name) {
    for (Component libraryCheckbox : libraryPanel.getComponents()) {
      if (name.equals(((JCheckBox) libraryCheckbox).getText())) {
        return (JCheckBox) libraryCheckbox;
      }
    }

    return null;
  }

  public void toggleLibrary(AppEngineStandardMavenLibrary library, boolean select) {
    for (Component libraryCheckbox : libraryPanel.getComponents()) {
      if (libraryCheckbox instanceof JCheckBox
          && ((JCheckBox) libraryCheckbox).getText().equals(library.getDisplayName())) {
        ((JCheckBox) libraryCheckbox).setSelected(select);
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

  @SuppressWarnings("checkstyle:abbreviationaswordinname")
  private void createUIComponents() {
    libraryPanel = new JPanel(new GridLayout(AppEngineStandardMavenLibrary.values().length, 1));
    if (enabled) {
      libraryPanel.setBorder(
          BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Libraries"));

      for (AppEngineStandardMavenLibrary library : AppEngineStandardMavenLibrary.values()) {
        final JCheckBox libraryCheckbox = new JCheckBox(library.getDisplayName());
        libraryPanel.add(libraryCheckbox);

        if (library == AppEngineStandardMavenLibrary.SERVLET_API) {
          // The Servlet API is provided by the AE standard runtime. So we are enabling it by
          // default.
          libraryCheckbox.setSelected(true);
        } else if (library == AppEngineStandardMavenLibrary.OBJECTIFY) {
          // If the user selects Objectify, then auto select the App Engine API since it is
          // required.
          libraryCheckbox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
              if (((JCheckBox) event.getItem()).isSelected()) {
                selectLibraryByName(AppEngineStandardMavenLibrary.APP_ENGINE_API.getDisplayName());
              }
            }
          });
        }
      }
    }
  }
}
