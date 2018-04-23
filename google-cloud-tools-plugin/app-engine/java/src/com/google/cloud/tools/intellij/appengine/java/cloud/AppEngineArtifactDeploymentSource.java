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

import com.intellij.packaging.artifacts.ArtifactPointer;
import com.intellij.remoteServer.configuration.deployment.DeploymentSourceType;
import com.intellij.remoteServer.impl.configuration.deployment.ArtifactDeploymentSourceImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An App Engine implementation of {@link ArtifactDeploymentSourceImpl} that provides its targeted
 * App Engine environment.
 */
public class AppEngineArtifactDeploymentSource extends ArtifactDeploymentSourceImpl
    implements AppEngineDeployable {

  private AppEngineEnvironment environment;

  private String projectName;
  private String version;

  /**
   * Initialize the artifact deployment source given a target App Engine environment, and an
   * artifact pointer.
   */
  public AppEngineArtifactDeploymentSource(
      @Nullable AppEngineEnvironment environment, @NotNull ArtifactPointer pointer) {
    super(pointer);
    this.environment = environment;
  }

  @Override
  public AppEngineEnvironment getEnvironment() {
    return environment;
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

  @NotNull
  @Override
  public AppEngineArtifactDeploymentSourceType getType() {
    return DeploymentSourceType.EP_NAME.findExtension(AppEngineArtifactDeploymentSourceType.class);
  }
}
