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

package com.google.cloud.tools.intellij.compiler;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.jps.model.impl.StackdriverProperties;
import com.google.cloud.tools.intellij.stackdriver.facet.StackdriverFacetType;

import com.intellij.compiler.server.BuildManagerListener;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;

import java.util.UUID;

/**
 * Copies the latest specified Cloud SDK location into the Stackdriver facet, to be used in the
 * source context generation build process.
 */
public class StackdriverBuildManagerListener implements BuildManagerListener {

  @Override
  public void beforeBuildProcessStarted(Project project, UUID sessionId) {
    for (Module module : ModuleManager.getInstance(project).getModules()) {
      StackdriverProperties configuration =
          FacetManager.getInstance(module).getFacetByType(StackdriverFacetType.ID)
              .getConfiguration().getState();
      configuration.setCloudSdkPath(CloudSdkService.getInstance().getSdkHomePath().toString());
    }
  }

  @Override
  public void buildStarted(Project project, UUID sessionId, boolean isAutomake) {
    // Do nothing.
  }

  @Override
  public void buildFinished(Project project, UUID sessionId, boolean isAutomake) {
    // Do nothing.
  }
}
