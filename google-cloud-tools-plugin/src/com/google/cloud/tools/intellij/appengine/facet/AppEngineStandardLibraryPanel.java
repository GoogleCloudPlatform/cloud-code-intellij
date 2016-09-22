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

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 * UI panel for configuring App Engine standard environment libraries.
 */
public class AppEngineStandardLibraryPanel {

  private JPanel libraryPanel;
  private JCheckBox servletApi;
  private JCheckBox jstl;
  private JCheckBox appEngineApi;
  private JCheckBox endpoints;
  private JCheckBox objectify;
  private List<JCheckBox> libraryGroup = Arrays.asList(servletApi, jstl, appEngineApi, endpoints,
      objectify);

  public Set<AppEngineStandardMavenLibrary> getSelectedLibraries() {
    return Sets.newHashSet(Collections2.filter(
        Collections2.transform(libraryGroup, new Function<JCheckBox, AppEngineStandardMavenLibrary>() {
          @Nullable
          @Override
          public AppEngineStandardMavenLibrary apply(JCheckBox libraryCheckbox) {
            return libraryCheckbox.isSelected()
                ? AppEngineStandardMavenLibrary.getLibraryByDisplayName(libraryCheckbox.getText())
                : null;
          }
        }),
        Predicates.notNull()
    ));
  }

  public void selectLibraries(Set<AppEngineStandardMavenLibrary> libraries) {
    Collection<String> availableLibraryNames = Collections2.transform(libraries,
        new Function<AppEngineStandardMavenLibrary, String>() {
          @Nullable
          @Override
          public String apply(AppEngineStandardMavenLibrary library) {
            return library.getDisplayName();
          }
        });

    for (JCheckBox libraryCheckbox : libraryGroup) {
      libraryCheckbox.setSelected(availableLibraryNames.contains(libraryCheckbox.getText()));
    }
  }

  public void toggleLibrary(AppEngineStandardMavenLibrary library, boolean select) {
    for (JCheckBox libraryCheckbox : libraryGroup) {
      if (libraryCheckbox.getText().equals(library.getDisplayName())) {
        libraryCheckbox.setSelected(select);
      }
    }
  }

  @NotNull
  public JPanel getComponent() {
    return libraryPanel;
  }
}
