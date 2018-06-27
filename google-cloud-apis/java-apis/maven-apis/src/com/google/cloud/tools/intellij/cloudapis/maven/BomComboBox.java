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

package com.google.cloud.tools.intellij.cloudapis.maven;

import com.google.common.collect.Lists;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ListCellRendererWrapper;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.swing.JComboBox;
import javax.swing.JList;

/**
 * Combobox that populates list of available BOM versions and selects currently active BOM version
 * from the given project/module.
 */
public class BomComboBox extends JComboBox<String> {
  private static final int NUM_BOM_VERSIONS_TO_SHOW = 5;

  public BomComboBox() {
    setRenderer(new BomVersionRenderer());
  }

  /** Sorts the list of BOM version in reverse order */
  private void sortBomList(List<String> boms) {
    boms.sort(Comparator.reverseOrder());
  }

  /**
   * Populates the BOM {@link JComboBox} with the fetched versions. If there is already a
   * preconfigured BOM in the corresponding module's pom.xml, then its version will be preselected.
   * If there are no versions returned and there is no preconfigured BOM, then the BOM UX is hidden.
   *
   * <p>Sorts the displayable versions in reverse order, and limits the number shown to some value
   * N.
   *
   * @param project Active IDE project
   * @param module Module selected for BOM/Cloud API dependency
   * @return true if BOM versions are available and selected, false if no BOM versions are shown.
   */
  // TODO (eshaul): make async with loader icons
  public boolean populateBomVersions(Project project, Module module) {
    List<String> availableBomVersions =
        Lists.newArrayList(CloudApiMavenService.getInstance().getAllBomVersions());

    Optional<String> configuredBomVersion =
        CloudLibraryProjectState.getInstance(project).getCloudLibraryBomVersion(module);

    if (availableBomVersions.isEmpty() && !configuredBomVersion.isPresent()) {
      return false;
    } else {
      sortBomList(availableBomVersions);

      if (availableBomVersions.size() > NUM_BOM_VERSIONS_TO_SHOW) {
        availableBomVersions = availableBomVersions.subList(0, NUM_BOM_VERSIONS_TO_SHOW);
      }

      // If there is a preconfigured BOM not already in the list, add it to the list, and re-sort
      if (configuredBomVersion.isPresent()
          && availableBomVersions.indexOf(configuredBomVersion.get()) == -1) {
        availableBomVersions.add(configuredBomVersion.get());
        sortBomList(availableBomVersions);
      }

      removeAllItems();
      availableBomVersions.forEach(this::addItem);

      // If there is a preconfigured BOM, select it by default
      if (configuredBomVersion.isPresent()) {
        setSelectedIndex(availableBomVersions.indexOf(configuredBomVersion.get()));
      }
    }

    return true;
  }

  private static class BomVersionRenderer extends ListCellRendererWrapper<String> {

    private static final String BOM_VERSION_DISPLAY_FORMAT = "(BOM) %s";

    @Override
    public void customize(JList list, String value, int index, boolean selected, boolean hasFocus) {
      setText(String.format(BOM_VERSION_DISPLAY_FORMAT, value));
    }
  }
}
