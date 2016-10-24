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

import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.icons.AllIcons.FileTypes;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.remoteServer.configuration.deployment.DeploymentSourceType;
import com.intellij.remoteServer.impl.configuration.deployment.ModuleDeploymentSourceImpl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;

import java.io.File;

import javax.swing.Icon;

/**
 * A deployment source used as a placeholder to allow user selection of a jar or war file from the
 * filesystem.
 */
public class UserSpecifiedPathDeploymentSource extends ModuleDeploymentSourceImpl
    implements AppEngineDeployable {

  private String name;
  private String userSpecifiedFilePath;

  public UserSpecifiedPathDeploymentSource(@NotNull ModulePointer pointer) {
    super(pointer);
    setName(getDefaultName());
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
    return String.format("[%s] ", DateTime.now().toString("yyyy-MM-dd HH:mm:ss")) + name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getDefaultName() {
    return GctBundle.message("appengine.flex.user.specified.deploymentsource.name");
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
    return DeploymentSourceType.EP_NAME.findExtension(
        UserSpecifiedPathDeploymentSourceType.class);
  }

  @Override
  public AppEngineEnvironment getEnvironment() {
    return AppEngineEnvironment.APP_ENGINE_FLEX;
  }
}
