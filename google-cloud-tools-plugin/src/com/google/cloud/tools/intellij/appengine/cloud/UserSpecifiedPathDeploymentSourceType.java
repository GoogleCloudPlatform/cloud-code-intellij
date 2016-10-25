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

import com.intellij.openapi.module.ModulePointerManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.remoteServer.configuration.deployment.DeploymentSourceType;
import com.intellij.remoteServer.configuration.deployment.ModuleDeploymentSource;
import com.intellij.remoteServer.impl.configuration.deployment.DeployToServerRunConfiguration;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link DeploymentSourceType} that supports serialization for a {@link
 * UserSpecifiedPathDeploymentSource}.
 */
public class UserSpecifiedPathDeploymentSourceType extends
    DeploymentSourceType<ModuleDeploymentSource> {

  private static final String SOURCE_TYPE_ID = "filesystem-war-jar-module";
  private static final String PROJECT_ATTRIBUTE = "project";
  private static final String VERSION_ATTRIBUTE = "version";

  public UserSpecifiedPathDeploymentSourceType() {
    super(SOURCE_TYPE_ID);
  }

  /**
   * Restore presentable name (e.g., to be "Filesystem JAR or WAR file - [file path]") of
   * UserSpecifiedPathDeploymentSource.
   */
  @NotNull
  @Override
  public ModuleDeploymentSource load(@NotNull Element tag, @NotNull Project project) {
    UserSpecifiedPathDeploymentSource userSpecifiedSource =
        new UserSpecifiedPathDeploymentSource(ModulePointerManager
            .getInstance(project).create(UserSpecifiedPathDeploymentSource.moduleName));

    userSpecifiedSource.setProjectName(tag.getAttributeValue(PROJECT_ATTRIBUTE));
    userSpecifiedSource.setVersion(tag.getAttributeValue(VERSION_ATTRIBUTE));

    Element settings = tag.getChild(DeployToServerRunConfiguration.SETTINGS_ELEMENT);
    if (settings != null) {
      String filePath = settings.getAttributeValue(
          AppEngineDeploymentConfiguration.USER_SPECIFIED_ARTIFACT_PATH_ATTRIBUTE);

      if (!StringUtil.isEmpty(filePath)) {
        userSpecifiedSource.setFilePath(filePath);

        return userSpecifiedSource;
      }
    }

    return userSpecifiedSource;
  }

  @Override
  public void save(@NotNull ModuleDeploymentSource deploymentSource, @NotNull Element tag) {
    if (deploymentSource instanceof AppEngineDeployable) {
      AppEngineDeployable deployable = (AppEngineDeployable) deploymentSource;

      if (deployable.getProjectName() != null) {
        tag.setAttribute(PROJECT_ATTRIBUTE,
            ((AppEngineDeployable) deploymentSource).getProjectName());
      }

      if (deployable.getVersion() != null) {
        tag.setAttribute(VERSION_ATTRIBUTE, ((AppEngineDeployable) deploymentSource).getVersion());
      }
    }
  }
}
