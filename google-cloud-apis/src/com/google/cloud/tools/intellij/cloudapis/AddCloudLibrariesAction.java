/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.cloudapis;

import com.google.cloud.tools.intellij.GoogleCloudCoreIcons;
import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardFacet;
import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardFacetType;
import com.google.cloud.tools.intellij.appengine.java.maven.project.MavenProjectService;
import com.google.cloud.tools.intellij.appengine.java.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.cloudapis.maven.CloudLibraryDependencyWriter;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbAwareAction;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.JavaVersion;

/**
 * The action in the Google Cloud Tools menu group that opens the wizard to add client libraries to
 * the user's project and manage cloud APIs.
 */
public final class AddCloudLibrariesAction extends DumbAwareAction {

  public AddCloudLibrariesAction() {
    super(
        GoogleCloudApisMessageBundle.message("cloud.libraries.menu.action.active.text"),
        GoogleCloudApisMessageBundle.message("cloud.libraries.menu.action.description"),
        GoogleCloudCoreIcons.CLOUD);
  }

  @Override
  @SuppressWarnings("MissingCasesInEnumSwitch")
  public void update(AnActionEvent e) {
    boolean addLibrariesEnabled = false;
    if (e.getProject() != null) {
      Set<CloudLibrariesModuleSupportType> moduleSupportTypes =
          Stream.of(ModuleManager.getInstance(e.getProject()).getModules())
              .map(this::checkModuleForAddCloudLibraries)
              .collect(Collectors.toSet());

      addLibrariesEnabled = moduleSupportTypes.contains(CloudLibrariesModuleSupportType.SUPPORTED);
      if (!addLibrariesEnabled) {
        // update message to hint what is missing.
        for (CloudLibrariesModuleSupportType supportType : moduleSupportTypes) {
          switch (supportType) {
            case MAVEN_REQUIRED:
              e.getPresentation()
                  .setDescription(
                      GoogleCloudApisMessageBundle.message(
                          "cloud.libraries.menu.action.maven.required.description"));
              e.getPresentation()
                  .setText(
                      GoogleCloudApisMessageBundle.message(
                          "cloud.libraries.menu.action.disabled.maven.text"));
              break;
            case APPENGINE_JAVA8_REQUIRED:
              e.getPresentation()
                  .setDescription(
                      GoogleCloudApisMessageBundle.message(
                          "cloud.libraries.menu.action.gae.java8.required.description"));
              e.getPresentation()
                  .setText(
                      GoogleCloudApisMessageBundle.message(
                          "cloud.libraries.menu.action.disabled.java8.text"));
              break;
          }
        }
      } else {
        // standard message for a supported module action.
        e.getPresentation()
            .setDescription(
                GoogleCloudApisMessageBundle.message("cloud.libraries.menu.action.description"));
        e.getPresentation()
            .setText(
                GoogleCloudApisMessageBundle.message("cloud.libraries.menu.action.active.text"));
      }
    }

    e.getPresentation().setEnabled(addLibrariesEnabled);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    if (e.getProject() != null) {
      AddCloudLibrariesDialog librariesDialog = new AddCloudLibrariesDialog(e.getProject());
      librariesDialog.show();

      if (librariesDialog.isOK()) {
        CloudLibraryDependencyWriter.addLibraries(
            librariesDialog.getSelectedLibraries(),
            librariesDialog.getSelectedModule(),
            librariesDialog.getSelectedBomVersion().orElse(null));
      }
    }
  }

  private enum CloudLibrariesModuleSupportType {
    SUPPORTED,
    MAVEN_REQUIRED,
    APPENGINE_JAVA8_REQUIRED
  }

  private CloudLibrariesModuleSupportType checkModuleForAddCloudLibraries(Module module) {
    // AppEngine Standard + Java 7 are not supported for GCP Libraries
    if (AppEngineProjectService.getInstance().hasAppEngineStandardFacet(module)) {
      AppEngineStandardFacet appEngineStandardFacet =
          FacetManager.getInstance(module).getFacetByType(AppEngineStandardFacetType.ID);
      if (!appEngineStandardFacet.getRuntimeJavaVersion().atLeast(JavaVersion.JAVA_1_8))
        return CloudLibrariesModuleSupportType.APPENGINE_JAVA8_REQUIRED;
    }

    return MavenProjectService.getInstance().isMavenModule(module)
        ? CloudLibrariesModuleSupportType.SUPPORTED
        : CloudLibrariesModuleSupportType.MAVEN_REQUIRED;
  }
}
