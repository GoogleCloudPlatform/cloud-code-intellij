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

import com.google.cloud.tools.intellij.appengine.facet.standard.AppEngineStandardFacet;
import com.google.cloud.tools.intellij.appengine.facet.standard.AppEngineStandardFacetType;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbAwareAction;
import git4idea.DialogManager;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.JavaVersion;

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
    Boolean addLibrariesEnabled =
        Optional.ofNullable(e.getProject())
            .map(
                project ->
                    Stream.of(ModuleManager.getInstance(project).getModules())
                        .anyMatch(this::isValidModuleForAddCloudLibraries))
            .orElse(false);

    e.getPresentation().setEnabled(addLibrariesEnabled);
    if (!addLibrariesEnabled) {
      // update message to hint Maven/Java 8 project is required.
      e.getPresentation()
          .setDescription(GctBundle.message("cloud.libraries.menu.action.disabled.description"));
    } else {
      e.getPresentation()
          .setDescription(GctBundle.message("cloud.libraries.menu.action.description"));
    }
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

  private boolean isValidModuleForAddCloudLibraries(Module module) {
    // AppEngine Standard + Java 7 are not supported for GCP Libraries
    if (AppEngineProjectService.getInstance().hasAppEngineStandardFacet(module)) {
      AppEngineStandardFacet appEngineStandardFacet =
          FacetManager.getInstance(module).getFacetByType(AppEngineStandardFacetType.ID);
      if (!appEngineStandardFacet.getRuntimeJavaVersion().atLeast(JavaVersion.JAVA_1_8))
        return false;
    }

    return AppEngineProjectService.getInstance().isMavenModule(module);
  }
}
