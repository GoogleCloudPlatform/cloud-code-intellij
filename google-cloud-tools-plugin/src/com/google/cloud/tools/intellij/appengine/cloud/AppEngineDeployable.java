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

import com.intellij.remoteServer.configuration.deployment.DeploymentSource;

/**
 * An {@link DeploymentSource} that is deployable to Google App engine that specifies its target
 * App Engine environment.
 */
public interface AppEngineDeployable extends DeploymentSource {

  /**
   * Returns the targeted App Engine environment.
   */
  AppEngineEnvironment getEnvironment();


  /**
   * Returns the targeted cloud project name.
   */
  String getProjectName();

  /**
   * Sets the targeted cloud project. It is mutable because the cloud project is not known until
   * deploy time where it is then set.
   */
  void setProjectName(String projectName);

  /**
   * Returns the targeted cloud project version.
   */
  String getVersion();

  /**
   * Sets the targeted cloud project version. It is mutable because the cloud project version is
   * not known until deploy time where it is then set.
   */
  void setVersion(String version);
}
