/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.google.gct.idea.appengine.cloud;

import com.google.gct.idea.util.GctBundle;

import com.intellij.remoteServer.util.CloudDeploymentNameConfiguration;
import com.intellij.util.xmlb.annotations.Attribute;

import org.jetbrains.annotations.NotNull;

/**
 * The model for a Managed VM based deployment configuration.  This state is specific to the
 * artifact that's being deployed, as such there can be multiple per project.
 */
public class ManagedVmDeploymentConfiguration extends
    CloudDeploymentNameConfiguration<ManagedVmDeploymentConfiguration> {

  public enum ConfigType {
    AUTO("appengine.managedvm.configtype.auto.label"),
    CUSTOM("appengine.managedvm.configtype.custom.label");

    private String label;

    ConfigType(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return GctBundle.message(label);
    }
  }

  private String dockerFilePath;
  private String appYamlPath;
  private ConfigType configType;

  @Attribute("dockerFilePath")
  public String getDockerFilePath() {
    return dockerFilePath;
  }

  @Attribute("appYamlPath")
  public String getAppYamlPath() {
    return appYamlPath;
  }

  @Attribute("configType")
  public ConfigType getConfigType() {
    return configType == null ? ConfigType.AUTO : configType;
  }

  public void setConfigType(@NotNull ConfigType configType) {
    this.configType = configType;
  }

  public void setDockerFilePath(String dockerFilePath) {
    this.dockerFilePath = dockerFilePath;
  }

  public void setAppYamlPath(String appYamlPath) {
    this.appYamlPath = appYamlPath;
  }
}
