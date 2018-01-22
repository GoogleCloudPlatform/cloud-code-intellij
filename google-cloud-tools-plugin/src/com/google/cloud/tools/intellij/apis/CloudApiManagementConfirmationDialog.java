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

import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.DialogWrapper;
import java.util.Set;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import org.jetbrains.annotations.Nullable;

/**
 * Dialog confirming GCP API management actions such as API enablement. Allows selection of a {@link
 * CloudProject CloudProject} on which to perform the actions.
 */
public class CloudApiManagementConfirmationDialog extends DialogWrapper {

  private JPanel panel;
  private JPanel apisToEnablePanel;
  private JPanel apisNotSelectedToEnablePanel;
  private JList<String> apisToEnableList;
  private JList<String> apisNotSelectedToEnableList;
  private JLabel enableConfirmationLabel;
  private JLabel wontEnableConfirmationLabel;

  /**
   * Initializes the Cloud API management confirmation dialog.
   *
   * @param module the {@link Module} the client libraries are added to
   * @param apisToEnable the set of APIs to be enabled on GCP
   */
  CloudApiManagementConfirmationDialog(
      Module module,
      CloudProject cloudProject,
      Set<CloudLibrary> apisToEnable,
      Set<CloudLibrary> apisNotToEnable) {
    super(module.getProject());
    init();
    setTitle(GctBundle.message("cloud.apis.management.dialog.title"));
    enableConfirmationLabel.setText(
        GctBundle.message(
            "cloud.apis.management.dialog.apistoenable.header", cloudProject.projectName()));
    wontEnableConfirmationLabel.setText(
        GctBundle.message("cloud.apis.management.dialog.apisnottoenable.header", module.getName()));

    apisToEnablePanel.setVisible(!apisToEnable.isEmpty());
    apisNotSelectedToEnablePanel.setVisible(!apisNotToEnable.isEmpty());

    populateLibraryList(apisToEnableList, apisToEnable);
    populateLibraryList(apisNotSelectedToEnableList, apisNotToEnable);
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return panel;
  }

  private void populateLibraryList(JList<String> list, Set<CloudLibrary> libraries) {
    DefaultListModel<String> listModel = new DefaultListModel<>();
    libraries.forEach(library -> listModel.addElement(library.getName()));
    list.setModel(listModel);
  }
}
