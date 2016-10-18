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

package com.google.cloud.tools.intellij.appengine.maven;

import com.google.cloud.tools.intellij.appengine.facet.AppEngineFacetConfiguration;
import com.google.cloud.tools.intellij.appengine.facet.AppEngineStandardFacet;
import com.google.cloud.tools.intellij.appengine.facet.AppEngineStandardFacetType;
import com.google.cloud.tools.intellij.appengine.facet.AppEngineWebIntegration;

import com.intellij.facet.FacetType;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.module.Module;
import com.intellij.packaging.artifacts.Artifact;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.importing.FacetImporter;
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenPlugin;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectChanges;
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask;
import org.jetbrains.idea.maven.project.MavenProjectsTree;

import java.util.List;
import java.util.Map;

/**
 * @author nik
 */
// TODO update this to use the new maven plugin and sdk
public class AppEngineFacetImporter extends
    FacetImporter<AppEngineStandardFacet, AppEngineFacetConfiguration, AppEngineStandardFacetType> {

  public AppEngineFacetImporter() {
    super("com.google.appengine", "appengine-maven-plugin",
        FacetType.findInstance(AppEngineStandardFacetType.class));
  }

  @Nullable
  private String getVersion(MavenProject project) {
    for (MavenArtifact artifact : project
        .findDependencies("com.google.appengine", "appengine-api-1.0-sdk")) {
      String artifactVersion = artifact.getVersion();
      if (artifactVersion != null) {
        return artifactVersion;
      }
    }
    MavenPlugin plugin = project.findPlugin(myPluginGroupID, myPluginArtifactID);
    return plugin != null ? plugin.getVersion() : null;
  }

  @Override
  protected void setupFacet(AppEngineStandardFacet facet, MavenProject mavenProject) {

  }

  @Override
  protected void reimportFacet(IdeModifiableModelsProvider modelsProvider,
      Module module,
      MavenRootModelAdapter rootModel,
      AppEngineStandardFacet facet,
      MavenProjectsTree mavenTree,
      MavenProject mavenProject,
      MavenProjectChanges changes,
      Map<MavenProject, String> mavenProjectToModuleName,
      List<MavenProjectsProcessorTask> postTasks) {
    String version = getVersion(mavenProject);
    if (version != null) {
      AppEngineWebIntegration.getInstance().setupDevServer();
      final String artifactName = module.getName() + ":war exploded";
      final Artifact webArtifact = modelsProvider.getModifiableArtifactModel()
          .findArtifact(artifactName);
      AppEngineWebIntegration.getInstance().setupRunConfiguration(webArtifact, module.getProject(),
          null /* existingConfiguration */);
    }
  }
}
