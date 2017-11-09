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

import com.google.cloud.tools.libraries.CloudLibraries;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBScrollPane;
import java.io.IOException;
import java.util.Comparator;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

/** The form-bound class for the Cloud API selector panel. */
final class GoogleCloudApiSelectorPanel {

  private JPanel panel;
  private JBScrollPane leftScrollPane;
  private JBScrollPane rightScrollPane;
  private JPanel checkboxPanel;
  private JBSplitter splitter;
  private GoogleCloudApiDetailsPanel detailsForm;

  /** Returns the {@link JPanel} that holds the UI elements in this panel. */
  public JPanel getPanel() {
    return panel;
  }

  /**
   * Initializes some UI components in this panel that require special set-up.
   *
   * <p>This is automatically called by the IDEA SDK and should not be directly invoked.
   */
  private void createUIComponents() {
    splitter = new JBSplitter();
    splitter.setFirstComponent(leftScrollPane);
    splitter.setSecondComponent(rightScrollPane);

    checkboxPanel = new JPanel();
    checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.PAGE_AXIS));
    try {
      CloudLibraries.getCloudLibraries()
          .stream()
          .sorted(Comparator.comparing(CloudLibrary::getName))
          .forEach(this::addLibraryCheckbox);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Adds a new checkbox for the given {@link CloudLibrary} to the {@link #checkboxPanel}.
   *
   * @param library the {@link CloudLibrary} to add the checkbox for
   */
  private void addLibraryCheckbox(CloudLibrary library) {
    JBCheckBox checkBox = new JBCheckBox(library.getName());
    checkBox.addActionListener(event -> detailsForm.setCloudLibrary(library));
    checkboxPanel.add(checkBox);
  }
}
