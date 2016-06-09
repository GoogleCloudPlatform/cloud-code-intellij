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

import com.intellij.remoteServer.util.CloudDeploymentNameConfiguration;
import com.intellij.util.xmlb.annotations.Attribute;

import org.jetbrains.annotations.NotNull;

/**
 * The model for a App Engine based deployment configuration.  This state is specific to the
 * artifact that's being deployed, as such there can be multiple per project.
 */
public class AppEngineDeploymentConfiguration extends
    CloudDeploymentNameConfiguration<AppEngineDeploymentConfiguration> {

  public enum ConfigType {
    AUTO("appengine.flex.configtype.auto.label"),
    CUSTOM("appengine.flex.configtype.custom.label");

    private String label;

    ConfigType(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return GctBundle.message(label);
    }
  }

  public static final String USER_SPECIFIED_ARTIFACT_PATH_ATTRIBUTE = "userSpecifiedArtifactPath";

  private String cloudProjectName;
  private String googleUsername;
  private String dockerFilePath;
  private String appYamlPath;
  private boolean userSpecifiedArtifact;
  private String userSpecifiedArtifactPath;
  private ConfigType configType;
  private String version;

  @Attribute("cloudProjectName")
  public String getCloudProjectName() {
    return cloudProjectName;
  }

  @Attribute("googleUsername")
  public String getGoogleUsername() {
    return googleUsername;
  }

  @Attribute("userSpecifiedArtifact")
  public boolean isUserSpecifiedArtifact() {
    return userSpecifiedArtifact;
  }

  @Attribute(USER_SPECIFIED_ARTIFACT_PATH_ATTRIBUTE)
  public String getUserSpecifiedArtifactPath() {
    return userSpecifiedArtifactPath;
  }

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

  @Attribute("version")
  public String getVersion() {
    return version;
  }

  public void setConfigType(@NotNull ConfigType configType) {
    this.configType = configType;
  }

  public void setCloudProjectName(String cloudProjectName) {
    this.cloudProjectName = cloudProjectName;
  }

  public void setGoogleUsername(String googleUsername) {
    this.googleUsername = googleUsername;
  }

  public void setUserSpecifiedArtifact(boolean userSpecifiedArtifact) {
    this.userSpecifiedArtifact = userSpecifiedArtifact;
  }

  public void setUserSpecifiedArtifactPath(String userSpecifiedArtifactPath) {
    this.userSpecifiedArtifactPath = userSpecifiedArtifactPath;
  }

  public void setDockerFilePath(String dockerFilePath) {
    this.dockerFilePath = dockerFilePath;
  }

  public void setAppYamlPath(String appYamlPath) {
    this.appYamlPath = appYamlPath;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public boolean isCustom() {
    return getConfigType() == ConfigType.CUSTOM;
  }
}
