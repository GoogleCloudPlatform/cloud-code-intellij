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

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ArtifactPointer;
import com.intellij.packaging.artifacts.ArtifactPointerManager;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider;
import com.intellij.remoteServer.configuration.deployment.ArtifactDeploymentSource;
import com.intellij.remoteServer.configuration.deployment.DeploymentSourceType;
import com.intellij.remoteServer.impl.configuration.deployment.DeployToServerRunConfiguration;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

import javax.swing.JComponent;

/**
 * A {@link DeploymentSourceType} loading and saving of a {@link AppEngineArtifactDeploymentSource}.
 */
public class AppEngineArtifactDeploymentSourceType
    extends DeploymentSourceType<ArtifactDeploymentSource> {

  private static final String SOURCE_TYPE_ID = "appengine-artifact-source";
  private static final String NAME_ATTRIBUTE = "name";
  private static final String PROJECT_ATTRIBUTE = "project";
  private static final String VERSION_ATTRIBUTE = "version";

  public AppEngineArtifactDeploymentSourceType() {
    super(SOURCE_TYPE_ID);
  }

  @NotNull
  @Override
  public AppEngineArtifactDeploymentSource load(@NotNull Element tag, @NotNull Project project) {
    final String artifactName = tag.getAttributeValue(NAME_ATTRIBUTE);
    Element settings = tag.getChild(DeployToServerRunConfiguration.SETTINGS_ELEMENT);

    if (settings != null) {
      Artifact[] artifacts = ArtifactManager.getInstance(project).getArtifacts();
      Optional<Artifact> artifact = Arrays.asList(artifacts).stream()
          .filter(candidate -> candidate.getName().equals(artifactName))
          .findFirst();

      String environment = settings.getAttributeValue(
          AppEngineDeploymentConfiguration.ENVIRONMENT_ATTRIBUTE);

      if (artifact.isPresent() && environment != null) {
        return createDeploymentSource(AppEngineEnvironment.valueOf(environment),
            ArtifactPointerManager.getInstance(project).createPointer(artifact.get()), tag);
      }
    }

    return createDeploymentSource(
        null /*environment */,
        ArtifactPointerManager.getInstance(project).createPointer(artifactName), tag);
  }

  @Override
  public void save(@NotNull ArtifactDeploymentSource deploymentSource,
      @NotNull Element tag) {
    tag.setAttribute(NAME_ATTRIBUTE, deploymentSource.getPresentableName());

    if (deploymentSource instanceof AppEngineDeployable) {
      AppEngineDeployable deployable = (AppEngineDeployable) deploymentSource;

      if (deployable.getProjectName() != null) {
        tag.setAttribute(PROJECT_ATTRIBUTE, deployable.getProjectName());
      }

      if (deployable.getVersion() != null) {
        tag.setAttribute(VERSION_ATTRIBUTE, deployable.getVersion());
      }

      if (deployable.getEnvironment() != null) {
        tag.setAttribute(AppEngineDeploymentConfiguration.ENVIRONMENT_ATTRIBUTE,
            deployable.getEnvironment().name());
      }
    }
  }

  @Override
  public void setBuildBeforeRunTask(@NotNull RunConfiguration configuration,
      @NotNull ArtifactDeploymentSource source) {
    Artifact artifact = source.getArtifact();
    if (artifact != null) {
      BuildArtifactsBeforeRunTaskProvider.setBuildArtifactBeforeRun(
          configuration.getProject(), configuration, artifact);
    }
  }

  @Override
  public void updateBuildBeforeRunOption(@NotNull JComponent runConfigurationEditorComponent,
      @NotNull Project project,
      @NotNull ArtifactDeploymentSource source, boolean select) {
    Artifact artifact = source.getArtifact();
    if (artifact != null) {
      BuildArtifactsBeforeRunTaskProvider.setBuildArtifactBeforeRunOption(
          runConfigurationEditorComponent, project, artifact, select);
    }
  }

  private AppEngineArtifactDeploymentSource createDeploymentSource(
      AppEngineEnvironment environment, ArtifactPointer artifactPointer, Element persistedData) {
    AppEngineArtifactDeploymentSource source
        = new AppEngineArtifactDeploymentSource(environment, artifactPointer);
    source.setProjectName(persistedData.getAttributeValue(PROJECT_ATTRIBUTE));
    source.setVersion(persistedData.getAttributeValue(VERSION_ATTRIBUTE));

    return source;
  }

}
