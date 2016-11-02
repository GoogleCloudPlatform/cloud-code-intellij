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

package com.google.cloud.tools.intellij.appengine.cloud;

import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.remoteServer.configuration.RemoteServersManager;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfigurationManager;
import com.intellij.remoteServer.impl.configuration.deployment.DeployToServerRunConfiguration;
import com.intellij.remoteServer.impl.configuration.deployment.DeployToServerSettingsEditor;
import com.intellij.util.containers.ContainerUtil;

import java.util.List;

/**
 * Creates a shortcut to App Engine flex cloud configuration in the tools menu.
 */
public class AppEngineDeployToolsMenuAction extends AnAction {

  private static final Logger logger = Logger.getInstance(AppEngineDeployToolsMenuAction.class);

  /**
   * Initialize the tools menu shortcut.
   */
  public AppEngineDeployToolsMenuAction() {
    super(GctBundle.message("appengine.tools.menu.item.label"),
        GctBundle.message("appengine.tools.menu.item.label"),
        GoogleCloudToolsIcons.APP_ENGINE);
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    Project project = event.getProject();

    if (project != null) {
      AppEngineCloudType serverType = ServerType.EP_NAME.findExtension(AppEngineCloudType.class);
      List<RemoteServer<AppEngineServerConfiguration>> servers =
          RemoteServersManager.getInstance().getServers(serverType);

      try {
        DeploymentConfigurationManager.getInstance(project)
            .createAndRunConfiguration(serverType, ContainerUtil.getFirstItem(servers));
      } catch (NullPointerException npe) {
        /**
         * Handles the case where the configuration is executed with a null deployment source.
         * See {@link DeployToServerSettingsEditor#applyEditorTo(DeployToServerRunConfiguration)}
         * The deployment configuration is set to null causing the following execution to fail:
         * {@link DeployToServerRunConfiguration#checkConfiguration()}
         */
        logger.warn("Error encountered executing App Engine deployment run configuration.", npe);
      }
    }
  }
}
