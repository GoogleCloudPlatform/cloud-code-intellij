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

package com.google.cloud.tools.intellij.appengine.java.cloud.apis;

import com.google.cloud.tools.intellij.appengine.java.AppEngineMessageBundle;
import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardFacet;
import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardFacetType;
import com.google.cloud.tools.intellij.appengine.java.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.cloudapis.CloudApiActionDecoratorExtension;
import com.google.cloud.tools.intellij.cloudapis.GoogleCloudApisMessageBundle;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import java.util.stream.Stream;
import org.apache.commons.lang3.JavaVersion;

/**
 * {@link CloudApiActionDecoratorExtension} extension point that checks for Java projects version if
 * App Engine is used. Otherwise, action is disabled and help text is shown.
 */
public class AppEngineCloudApiActionDecorator implements CloudApiActionDecoratorExtension {

  /**
   * Checks if current project module(s) are App Engine + Java 7 combination which does not support
   * cloud APIs and disables the Cloud API action if so.
   *
   * @return true if action must be disabled and prevented for this project and other decorators do
   *     not apply anymore.
   */
  @Override
  public boolean decorate(AnActionEvent e) {
    boolean cloudApiLibrariesSupported = true;
    if (e.getProject() != null) {
      cloudApiLibrariesSupported =
          Stream.of(ModuleManager.getInstance(e.getProject()).getModules())
              .anyMatch(this::checkAddCloudLibrariesSupport);
    }

    if (!cloudApiLibrariesSupported) {
      // update message to hint what is missing.
      e.getPresentation()
          .setDescription(
              AppEngineMessageBundle.message(
                  "cloud.libraries.menu.action.gae.java8.required.description"));
      e.getPresentation()
          .setText(
              AppEngineMessageBundle.message("cloud.libraries.menu.action.disabled.java8.text"));
    } else {
      // standard message for a supported module action.
      e.getPresentation()
          .setDescription(
              GoogleCloudApisMessageBundle.message("cloud.libraries.menu.action.description"));
      e.getPresentation()
          .setText(GoogleCloudApisMessageBundle.message("cloud.libraries.menu.action.active.text"));
    }

    e.getPresentation().setEnabled(cloudApiLibrariesSupported);

    // if we decided to disable the action due to library/java version conflict, stop the decoration
    // and show the helper message to a user.
    return !cloudApiLibrariesSupported;
  }

  private boolean checkAddCloudLibrariesSupport(Module module) {
    // AppEngine Standard + Java 7 are not supported for GCP Libraries
    if (AppEngineProjectService.getInstance().hasAppEngineStandardFacet(module)) {
      AppEngineStandardFacet appEngineStandardFacet =
          FacetManager.getInstance(module).getFacetByType(AppEngineStandardFacetType.ID);
      if (!appEngineStandardFacet.getRuntimeJavaVersion().atLeast(JavaVersion.JAVA_1_8)) {
        return false;
      }
    }

    return true;
  }
}
