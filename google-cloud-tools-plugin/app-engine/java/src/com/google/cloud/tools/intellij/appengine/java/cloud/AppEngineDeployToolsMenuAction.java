/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.cloud.tools.intellij.appengine.java.cloud;

import com.google.cloud.tools.intellij.appengine.java.AppEngineIcons;
import com.google.cloud.tools.intellij.appengine.java.AppEngineMessageBundle;
import com.google.cloud.tools.intellij.appengine.java.project.AppEngineProjectService;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.remoteServer.configuration.RemoteServersManager;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfigurationManager;
import com.intellij.util.containers.ContainerUtil;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

/** Creates a shortcut to App Engine deployment configuration in the tools menu. */
public class AppEngineDeployToolsMenuAction extends AnAction {

  private static final Logger logger = Logger.getInstance(AppEngineDeployToolsMenuAction.class);

  public AppEngineDeployToolsMenuAction() {
    super(
        AppEngineMessageBundle.message("appengine.tools.menu.deploy.text"),
        AppEngineMessageBundle.message("appengine.tools.menu.deploy.description"),
        AppEngineIcons.APP_ENGINE);
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    Project project = event.getProject();

    if (project != null) {
      if (isAppEngineProject(project)) {
        openRunConfiguration(project);
      } else {
        notifyNotAppEngineProject(project);
      }
    }
  }

  private void openRunConfiguration(@NotNull Project project) {
    AppEngineCloudType serverType = ServerType.EP_NAME.findExtension(AppEngineCloudType.class);
    List<RemoteServer<AppEngineServerConfiguration>> servers =
        RemoteServersManager.getInstance().getServers(serverType);

    try {
      DeploymentConfigurationManager.getInstance(project)
          .createAndRunConfiguration(serverType, ContainerUtil.getFirstItem(servers));
    } catch (NullPointerException npe) {
      /**
       * Handles the case where the configuration is executed with a null deployment source. See
       * {@link DeployToServerSettingsEditor#applyEditorTo(DeployToServerRunConfiguration)} The
       * deployment configuration is set to null causing the following execution to fail: {@link
       * DeployToServerRunConfiguration#checkConfiguration()}
       */
      logger.warn("Error encountered executing App Engine deployment run configuration.", npe);
    }
  }

  private void notifyNotAppEngineProject(@NotNull Project project) {
    NotificationGroup notification =
        new NotificationGroup(
            AppEngineMessageBundle.message("appengine.tools.menu.deploy.error.title"),
            NotificationDisplayType.BALLOON,
            true);

    notification
        .createNotification(
            AppEngineMessageBundle.message("appengine.tools.menu.deploy.error.title"),
            AppEngineMessageBundle.message("appengine.tools.menu.deploy.error.message"),
            NotificationType.ERROR,
            null /*listener*/)
        .notify(project);
  }

  /**
   * Determines if the project has at least one module with an App Engine standard or flexible
   * facet.
   */
  @VisibleForTesting
  boolean isAppEngineProject(@NotNull Project project) {
    AppEngineProjectService projectService = AppEngineProjectService.getInstance();

    return Stream.of(ModuleManager.getInstance(project).getModules())
        .anyMatch(
            module ->
                projectService.hasAppEngineStandardFacet(module)
                    || projectService.hasAppEngineFlexFacet(module));
  }
}
