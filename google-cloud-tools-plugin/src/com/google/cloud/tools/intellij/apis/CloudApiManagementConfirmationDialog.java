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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import java.util.Set;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import org.jetbrains.annotations.Nullable;

/**
 * Dialog confirming GCP API management actions such as API enablement. Allows selection of a {@link
 * CloudProject CloudProject} on which to perform the actions.
 */
public class CloudApiManagementConfirmationDialog extends DialogWrapper {

  private JPanel panel;
  private JList<String> apisToEnableList;

  /**
   * Initializes the Cloud API management confirmation dialog.
   *
   * @param project the current {@link Project}
   * @param apisToEnable the set of APIs to be enabled on GCP
   */
  CloudApiManagementConfirmationDialog(@Nullable Project project, Set<CloudLibrary> apisToEnable) {
    super(project);
    init();
    setTitle(GctBundle.message("cloud.apis.management.dialog.title"));

    DefaultListModel<String> apiListModel = new DefaultListModel<>();
    apisToEnable.forEach(library -> apiListModel.addElement(library.getName()));
    apisToEnableList.setModel(apiListModel);
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return panel;
  }
}