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

package com.google.cloud.tools.intellij.appengine.java.cloud.flexible;

import com.google.cloud.tools.intellij.appengine.java.AppEngineMessageBundle;
import com.google.cloud.tools.intellij.appengine.java.cloud.AppEngineDeployable;
import com.google.cloud.tools.intellij.appengine.java.cloud.AppEngineEnvironment;
import com.intellij.icons.AllIcons.FileTypes;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.remoteServer.configuration.deployment.DeploymentSourceType;
import com.intellij.remoteServer.impl.configuration.deployment.ModuleDeploymentSourceImpl;
import java.io.File;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A deployment source used as a placeholder to allow user selection of a jar or war file from the
 * filesystem.
 */
public class UserSpecifiedPathDeploymentSource extends ModuleDeploymentSourceImpl
    implements AppEngineDeployable {

  public static final String moduleName = "userSpecifiedSource";
  private String userSpecifiedFilePath;
  private String projectName;
  private String version;

  public UserSpecifiedPathDeploymentSource(@NotNull ModulePointer pointer) {
    super(pointer);
  }

  @Override
  public boolean isValid() {
    return false;
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return FileTypes.Any_type;
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return AppEngineMessageBundle.message("appengine.flex.user.specified.deploymentsource.name");
  }

  @Override
  public String getProjectName() {
    return projectName;
  }

  @Override
  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public void setVersion(String version) {
    this.version = version;
  }

  @Nullable
  @Override
  public File getFile() {
    return userSpecifiedFilePath != null ? new File(userSpecifiedFilePath) : null;
  }

  public void setFilePath(@NotNull String userSpecifiedFilePath) {
    this.userSpecifiedFilePath = userSpecifiedFilePath;
  }

  @NotNull
  @Override
  public DeploymentSourceType<?> getType() {
    return DeploymentSourceType.EP_NAME.findExtension(UserSpecifiedPathDeploymentSourceType.class);
  }

  @Override
  public AppEngineEnvironment getEnvironment() {
    return AppEngineEnvironment.APP_ENGINE_FLEX;
  }
}
