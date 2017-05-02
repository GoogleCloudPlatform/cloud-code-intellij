/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.server.run;

import com.google.cloud.tools.intellij.CloudToolsRunConfigurationAction;
import com.google.cloud.tools.intellij.appengine.facet.standard.AppEngineStandardFacetType;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;

import com.intellij.facet.FacetManager;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * Creates a shortcut to the App Engine standard local run configuration in the tools menu.
 */
public class AppEngineStandardLocalRunToolsMenuAction extends CloudToolsRunConfigurationAction {

  private static final String APP_ENGINE_STANDARD_DOCS_LINK
      = "https://cloud.google.com/appengine/docs/java/";

  public AppEngineStandardLocalRunToolsMenuAction() {
    super(AppEngineServerConfigurationType.getInstance(),
        GctBundle.message("appengine.tools.menu.run.server.text"),
        GctBundle.message("appengine.tools.menu.run.server.description"),
        GoogleCloudToolsIcons.APP_ENGINE);
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    if (event.getProject() != null && isAppEngineStandardProjectCheck(event.getProject())) {
      super.actionPerformed(event);
    }
  }

  /**
   * Determines if the project has at least one module with the App Engine Standard facet.
   * If it does not, then a notification balloon is shown.
   */
  @VisibleForTesting
  boolean isAppEngineStandardProjectCheck(@NotNull Project project) {
    boolean hasAppEngineStandardFacet =
        Stream.of(ModuleManager.getInstance(project).getModules())
            .anyMatch(module ->
                !FacetManager.getInstance(module)
                    .getFacetsByType(AppEngineStandardFacetType.ID).isEmpty()
            );

    if (!hasAppEngineStandardFacet) {
      NotificationGroup notification =
          new NotificationGroup(
              GctBundle.message("appengine.tools.menu.run.server.error.title"),
              NotificationDisplayType.BALLOON,
              true);

      notification.createNotification(
          GctBundle.message("appengine.tools.menu.run.server.error.title"),
          GctBundle.message("appengine.tools.menu.run.server.error.message"),
          NotificationType.ERROR,
          null /*listener*/).notify(project);
    }

    return hasAppEngineStandardFacet;
  }

}
