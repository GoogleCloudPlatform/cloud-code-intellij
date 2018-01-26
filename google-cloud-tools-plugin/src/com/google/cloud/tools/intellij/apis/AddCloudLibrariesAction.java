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

import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import git4idea.DialogManager;

/**
 * The action in the Google Cloud Tools menu group that opens the wizard to add client libraries to
 * the user's project and manage cloud APIs.
 */
public final class AddCloudLibrariesAction extends DumbAwareAction {

  public AddCloudLibrariesAction() {
    super(
        GctBundle.message("cloud.libraries.menu.action.text"),
        GctBundle.message("cloud.libraries.menu.action.description"),
        GoogleCloudToolsIcons.CLOUD);
  }

  @Override
  public void update(AnActionEvent e) {
    boolean enableAddLibraries = false;
    Project project = e.getProject();
    if (project != null) {
      // check if this project has any 'maven-style' modules.
      for (Module module : ModuleManager.getInstance(project).getModules()) {
        if (AppEngineProjectService.getInstance().isMavenModule(module)) {
          enableAddLibraries = true;
          break;
        }
      }
    }

    e.getPresentation().setVisible(enableAddLibraries);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    if (e.getProject() != null) {
      AddCloudLibrariesDialog librariesDialog = new AddCloudLibrariesDialog(e.getProject());
      DialogManager.show(librariesDialog);

      if (librariesDialog.isOK()) {
        CloudLibraryDependencyWriter.addLibraries(
            librariesDialog.getSelectedLibraries(), librariesDialog.getSelectedModule());
      }
    }
  }
}
