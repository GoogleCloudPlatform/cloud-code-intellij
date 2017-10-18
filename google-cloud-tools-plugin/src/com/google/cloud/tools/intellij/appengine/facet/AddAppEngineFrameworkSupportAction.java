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

package com.google.cloud.tools.intellij.appengine.facet;

import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable;
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
import org.jetbrains.annotations.Nullable;

/**
 * Created by nbashirbello on 10/18/17.
 */
public abstract class AddAppEngineFrameworkSupportAction extends AnAction {

  public AddAppEngineFrameworkSupportAction(@Nullable String text, @Nullable String description) {
    super(text, description, null /* icon */);
  }

  public abstract FrameworkSupportInModuleConfigurable getModuleConfigurable();

  @Override
  public void actionPerformed(AnActionEvent event) {
    Project project = event.getProject();
    if (project == null) {
      Messages.showErrorDialog("No project available", "Cannot add App Engine Flexible support");
      return;
    }

    // TODO: add parser for suitable modules - modules that don't have the flex facet or the standard facet
    // or all modules?
    List<Module> suitableModules = new ArrayList<>(
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
    if (!chooseModulesDialog.isOK()) {
      return;
    }

    if (elements.size() == 0) {
      Messages.showErrorDialog(project, "No module selected ", "Error");
      return;
    }

    Module module = elements.get(0);
    AddAppEngineFrameworkSupportDialog frameworkSupportDialog =
        new AddAppEngineFrameworkSupportDialog(project, module, getModuleConfigurable());
    frameworkSupportDialog.show();
  }

}
