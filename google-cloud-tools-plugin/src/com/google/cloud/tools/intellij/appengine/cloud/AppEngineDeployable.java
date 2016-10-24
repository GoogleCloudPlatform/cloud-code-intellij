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
   * Sets the name of this deployable. Used for display in the Application Server deployment window
   * and for disambiguation of multiple deployables. This field is mutable so that, at deploy time,
   * we can append additional information to the name to ensure uniqueness (e.g. version).
   */
  void setName(String name);

  /**
   * Returns the default name for this deployable. The default name represents the name of the
   * deployment source prior to deploy time. For example, this would not include version information
   * since it is not yet known.
   */
  String getDefaultName();

}
