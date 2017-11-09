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

import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import git4idea.DialogManager;
import java.awt.Dimension;
import javax.swing.JComponent;
import org.jetbrains.annotations.Nullable;

/** The action in the Google Cloud Tools menu group that opens the dialog to add Cloud libraries. */
public final class AddCloudLibrariesAction extends DumbAwareAction {

  public AddCloudLibrariesAction() {
    super(
        GctBundle.message("cloud.libraries.menu.action.text"),
        GctBundle.message("cloud.libraries.menu.action.description"),
        GoogleCloudToolsIcons.CLOUD);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    if (e.getProject() != null) {
      AddCloudLibrariesDialog dialog = new AddCloudLibrariesDialog(e.getProject());
      DialogManager.show(dialog);
    }
  }

  /** The dialog for the "Add Cloud Libraries" menu action. */
  private static final class AddCloudLibrariesDialog extends DialogWrapper {

    AddCloudLibrariesDialog(Project project) {
      super(project);
      init();
      setTitle(GctBundle.message("cloud.libraries.dialog.title"));
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
      GoogleCloudApiSelectorPanel panel = new GoogleCloudApiSelectorPanel();
      panel.getPanel().setPreferredSize(new Dimension(800, 600));
      return panel.getPanel();
    }
  }
}
