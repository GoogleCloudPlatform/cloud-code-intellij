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

package com.google.cloud.tools.intellij.appengine.java.ultimate.server.run;

import com.google.cloud.tools.intellij.CloudToolsRunConfigurationAction;
import com.google.cloud.tools.intellij.appengine.java.AppEngineIcons;
import com.google.cloud.tools.intellij.appengine.java.AppEngineMessageBundle;
import com.google.cloud.tools.intellij.appengine.java.project.AppEngineProjectService;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

/** Creates a shortcut to the App Engine standard local run configuration in the tools menu. */
public class AppEngineStandardLocalRunToolsMenuAction extends CloudToolsRunConfigurationAction {

  public AppEngineStandardLocalRunToolsMenuAction() {
    super(
        AppEngineServerConfigurationType.getInstance(),
        AppEngineMessageBundle.message("appengine.tools.menu.run.server.text"),
        AppEngineMessageBundle.message("appengine.tools.menu.run.server.description"),
        AppEngineIcons.APP_ENGINE);
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    Project project = event.getProject();
    if (project != null) {
      if (isAppEngineStandardProject(project)) {
        super.actionPerformed(event);
      } else {
        notifyNotAppEngineStandardProject(project);
      }
    }
  }

  /** Determines if the project has at least one module with the App Engine standard facet. */
  @VisibleForTesting
  boolean isAppEngineStandardProject(@NotNull Project project) {
    return Stream.of(ModuleManager.getInstance(project).getModules())
        .anyMatch(
            module -> AppEngineProjectService.getInstance().hasAppEngineStandardFacet(module));
  }

  private void notifyNotAppEngineStandardProject(@NotNull Project project) {
    NotificationGroup notification =
        new NotificationGroup(
            AppEngineMessageBundle.message("appengine.tools.menu.run.server.error.title"),
            NotificationDisplayType.BALLOON,
            true);

    notification
        .createNotification(
            AppEngineMessageBundle.message("appengine.tools.menu.run.server.error.title"),
            AppEngineMessageBundle.message("appengine.tools.menu.run.server.error.message"),
            NotificationType.ERROR,
            null /*listener*/)
        .notify(project);
  }
}
