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

package com.google.cloud.tools.intellij.appengine.facet.flexible;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ChooseModulesDialog;
import com.intellij.openapi.ui.Messages;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ToolsMenuAction extends AnAction {

  // TODO: 1. opens the module selector
  // 2. if OK button is clicked, gets selected module
  // 3. opens the flex config panel wrapped in a dialog
  // 4. link ok button, to... AppEngineFlexibleSupportConfigurable#addAppEngineFlexibleSupport

  @Override
  public void actionPerformed(AnActionEvent event) {
    Project project = event.getProject(); // check null?
    if (project == null) {
      Messages.showErrorDialog("No project available", "Cannot add App Engine Flexible support");
      return;
    }

    // TODO: add parser for suitable modules - modules that don't have the flex facet or the standard facet
    // or all modules?
    List<Module> suitableModules = new ArrayList<Module>(
        Arrays.asList(ModuleManager.getInstance(project).getModules()));
    if (suitableModules.isEmpty()) {
      Messages.showErrorDialog(project, "No suitable modules for App Engine Flexible facet found.",
          "Cannot add App Engine Flexible support");
      return;
    }

    ChooseModulesDialog chooseModulesDialog = new ChooseModulesDialog(project, suitableModules,
        "Choose Module", "");
    chooseModulesDialog.setSingleSelectionMode();
    chooseModulesDialog.show();
    final List<Module> elements = chooseModulesDialog.getChosenElements();
    if (!chooseModulesDialog.isOK() || elements.size() != 1) {
      return;
    }

    final Module module = elements.get(0);
    //

  }

}
