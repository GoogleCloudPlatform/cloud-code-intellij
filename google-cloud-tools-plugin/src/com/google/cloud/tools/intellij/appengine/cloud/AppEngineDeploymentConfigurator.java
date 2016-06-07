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

import com.google.cloud.tools.intellij.appengine.util.AppEngineUtil;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfigurator;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Sets up the configuration elements for an AppEngine Cloud deployment.
 */
class AppEngineDeploymentConfigurator extends
    DeploymentConfigurator<AppEngineDeploymentConfiguration, AppEngineServerConfiguration> {

  private static final Logger logger = Logger.getInstance(AppEngineDeploymentConfigurator.class);

  private final Project project;

  public AppEngineDeploymentConfigurator(Project project) {
    this.project = project;
  }

  @NotNull
  @Override
  public List<DeploymentSource> getAvailableDeploymentSources() {
    List<DeploymentSource> deploymentSources = new ArrayList<>();

    deploymentSources.addAll(AppEngineUtil.createArtifactDeploymentSources(project));
    deploymentSources.addAll(AppEngineUtil.createModuleDeploymentSources(project));

    return deploymentSources;
  }

  @NotNull
  @Override
  public AppEngineDeploymentConfiguration createDefaultConfiguration(
      @NotNull DeploymentSource source) {
    return new AppEngineDeploymentConfiguration();
  }

  @Nullable
  @Override
  public SettingsEditor<AppEngineDeploymentConfiguration> createEditor(
      @NotNull DeploymentSource source,
      @NotNull RemoteServer<AppEngineServerConfiguration> server) {
    if (!(source instanceof AppEngineDeployable)) {
      logger.error(
          String.format("Deployment source with name %s is not deployable to App Engine.",
              source.getPresentableName()));
      return null;
    }

    return new AppEngineDeploymentRunConfigurationEditor(
        project,
        source,
        ((AppEngineDeployable) source).getEnvironment(),
        new CloudSdkAppEngineHelper(
            project,
            new File(server.getConfiguration().getCloudSdkHomePath()))
    );
  }
}
