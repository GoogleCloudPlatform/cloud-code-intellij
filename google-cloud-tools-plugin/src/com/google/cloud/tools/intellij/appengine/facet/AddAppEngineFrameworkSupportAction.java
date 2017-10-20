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

import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.facet.impl.DefaultFacetsProvider;
import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ChooseModulesDialog;
import com.intellij.openapi.ui.Messages;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Action to add either App Engine Flexible or Standard framework support to a module which
 * includes creating appropriate facet and run configurations.
 */
public abstract class AddAppEngineFrameworkSupportAction extends AnAction {

  public AddAppEngineFrameworkSupportAction(@Nullable String text, @Nullable String description) {
    super(text, description, null /* icon */);
  }

  @NotNull
  public abstract FrameworkSupportInModuleConfigurable getModuleConfigurable(Module module);

  @NotNull
  public abstract FrameworkSupportInModuleProvider getModuleProvider();

  /**
   * Opens Choose Module dialog for user to select a module, then opens the appropriate App Engine
   * Framework Support dialog to add App Engine support.
   */
  @Override
  public void actionPerformed(AnActionEvent event) {
    Project project = event.getProject();
    if (project == null) {
      return;
    }

    List<Module> suitableModules = new ArrayList<>(
        Arrays.asList(ModuleManager.getInstance(project).getModules()));
    DefaultFacetsProvider facetsProvider = new DefaultFacetsProvider();
    FrameworkSupportInModuleProvider provider = getModuleProvider();
    suitableModules.removeIf(module -> !provider.isEnabledForModuleType(ModuleType.get(module)) ||
        provider.isSupportAlreadyAdded(module, facetsProvider));

    String frameworkName = getTemplatePresentation().getText();
    if (suitableModules.isEmpty()) {
      Messages.showErrorDialog(project,
          GctBundle.message("appengine.add.framework.support.no.modules.message", frameworkName),
          GctBundle.message("appengine.add.framework.support.no.modules.title", frameworkName));
      return;
    }

    ChooseModulesDialog chooseModulesDialog = new ChooseModulesDialog(project, suitableModules,
        GctBundle.message("appengine.add.framework.support.choose.module.dialog.title"),
        GctBundle.message("appengine.add.framework.support.choose.module.dialog.description",
            frameworkName));
    chooseModulesDialog.setSingleSelectionMode();
    chooseModulesDialog.show();
    final List<Module> elements = chooseModulesDialog.getChosenElements();
    if (!chooseModulesDialog.isOK() || elements.size() != 1) {
      return;
    }

    Module module = elements.get(0);
    AddAppEngineFrameworkSupportDialog frameworkSupportDialog =
        new AddAppEngineFrameworkSupportDialog(
            GctBundle.message("appengine.add.framework.support.dialog.title", frameworkName),
            project,
            module,
            getModuleConfigurable(module));
    frameworkSupportDialog.show();
  }

}
