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

package com.google.cloud.tools.intellij.appengine.java.facet;

import com.google.cloud.tools.intellij.appengine.java.AppEngineMessageBundle;
import com.google.cloud.tools.intellij.appengine.java.util.AppEngineUtil;
import com.google.common.annotations.VisibleForTesting;
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
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import org.jetbrains.annotations.NotNull;

/**
 * Action to add either App Engine Flexible or Standard framework support to a module which includes
 * creating appropriate facet and run configurations.
 */
public abstract class AddAppEngineFrameworkSupportAction extends AnAction {
  private final String FRAMEWORK_NAME;

  public AddAppEngineFrameworkSupportAction(@NotNull String nameInTitle, @NotNull String name) {
    super(
        nameInTitle,
        AppEngineMessageBundle.message(
            "appengine.add.framework.support.tools.menu.description", name),
        null /* icon */);
    FRAMEWORK_NAME = name;
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

    List<Module> suitableModules = getModulesWithoutAppEngineSupport(project);

    String frameworkNameInTitle = getTemplatePresentation().getText();
    if (suitableModules.isEmpty()) {
      Messages.showErrorDialog(
          project,
          AppEngineMessageBundle.message(
              "appengine.add.framework.support.no.modules.message", FRAMEWORK_NAME),
          AppEngineMessageBundle.message(
              "appengine.add.framework.support.no.modules.title", frameworkNameInTitle));
      return;
    }

    ChooseModulesDialog chooseModulesDialog =
        new ChooseModulesDialog(
            project,
            suitableModules,
            AppEngineMessageBundle.message(
                "appengine.add.framework.support.choose.module.dialog.title"),
            AppEngineMessageBundle.message(
                "appengine.add.framework.support.choose.module.dialog.description",
                FRAMEWORK_NAME));
    chooseModulesDialog.setSingleSelectionMode();
    chooseModulesDialog.setOKActionEnabled(false);

    if (chooseModulesDialog.getPreferredFocusedComponent() instanceof JTable) {
      JTable chooseModuleTable = (JTable) chooseModulesDialog.getPreferredFocusedComponent();

      ListSelectionModel selectionModel = chooseModuleTable.getSelectionModel();
      if (selectionModel != null) {
        selectionModel.addListSelectionListener(
            e -> chooseModulesDialog.setOKActionEnabled(chooseModuleTable.getSelectedRow() != -1));
      }
    }

    chooseModulesDialog.show();

    final List<Module> elements = chooseModulesDialog.getChosenElements();
    if (!chooseModulesDialog.isOK() || elements.size() != 1) {
      return;
    }

    Module module = elements.get(0);
    AddAppEngineFrameworkSupportDialog frameworkSupportDialog =
        new AddAppEngineFrameworkSupportDialog(
            AppEngineMessageBundle.message(
                "appengine.add.framework.support.dialog.title", frameworkNameInTitle),
            project,
            module,
            getModuleConfigurable(module));
    frameworkSupportDialog.show();
  }

  @VisibleForTesting
  List<Module> getModulesWithoutAppEngineSupport(Project project) {
    List<Module> suitableModules =
        new ArrayList<>(Arrays.asList(ModuleManager.getInstance(project).getModules()));
    FrameworkSupportInModuleProvider provider = getModuleProvider();
    // checks if this module type supports out facets at all
    suitableModules.removeIf(module -> !provider.isEnabledForModuleType(ModuleType.get(module)));
    // filter out compatible modules where support has been already added.
    suitableModules.removeIf(AppEngineUtil::isAnyAppEngineFacetAlreadyAdded);

    return suitableModules;
  }
}
