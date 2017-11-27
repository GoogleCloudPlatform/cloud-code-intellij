/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.apis;

import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.cloud.tools.libraries.json.CloudLibraryClientMavenCoordinates;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.util.xml.DomUtil;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.dom.MavenDomUtil;
import org.jetbrains.idea.maven.dom.model.MavenDomDependency;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

/** A helper class that writes dependencies on Cloud libraries to a given module. */
final class CloudLibraryDependencyWriter {

  /** Prevents instantiation. */
  private CloudLibraryDependencyWriter() {}

  /**
   * Adds the given set of {@link CloudLibrary CloudLibraries} to the given {@link Module}.
   *
   * <p>For a module whose project manages its dependencies through Maven, the list of libraries
   * will be added as dependencies to the {@code pom.xml}. For all other dependency management
   * systems, the libraries will be downloaded and added directly to the module's classpath.
   *
   * @param libraries the set of {@link CloudLibrary CloudLibraries} to add
   * @param module the {@link Module} to add the libraries to
   */
  static void addLibraries(Set<CloudLibrary> libraries, Module module) {
    Project project = module.getProject();
    MavenProjectsManager projectsManager = MavenProjectsManager.getInstance(module.getProject());
    MavenProject mavenProject = projectsManager.findProject(module);
    if (mavenProject == null) {
      // TODO(nkibler): Handle non-Maven projects.
      return;
    }

    MavenDomProjectModel model =
        MavenDomUtil.getMavenDomProjectModel(project, mavenProject.getFile());
    if (model == null) {
      // TODO(nkibler): Handle this error-state, maybe a warning?
      return;
    }

    new WriteCommandAction(project, DomUtil.getFile(model)) {
      @Override
      protected void run(@NotNull Result result) throws Throwable {
        List<MavenDomDependency> dependencies = model.getDependencies().getDependencies();
        // TODO(nkibler): Inform the user when libraries were ignored because of existing
        // dependencies, maybe a warning?
        libraries
            .stream()
            .map(CloudLibraryUtils::getJavaClientMavenCoordinates)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(CloudLibraryDependencyWriter::toMavenId)
            .filter(mavenId -> isMavenIdNotInDependencyList(mavenId, dependencies))
            .forEach(mavenId -> MavenDomUtil.createDomDependency(model, null, mavenId));
      }
    }.execute();
  }

  /**
   * Returns {@code true} if the given {@link MavenId} is not in the given list of {@link
   * MavenDomDependency dependencies}.
   *
   * <p>Note that equality is tested via matching group IDs and artifact IDs. Equality of versions
   * is not required. This prevents adding duplicate dependencies for the same artifact, but with a
   * different version.
   *
   * @param mavenId the {@link MavenId} to check for existence in the given list of dependencies
   * @param dependencies the list of {@link MavenDomDependency} objects that currently exist in the
   *     DOM model
   */
  private static boolean isMavenIdNotInDependencyList(
      MavenId mavenId, List<MavenDomDependency> dependencies) {
    return dependencies
        .stream()
        .noneMatch(
            dependency ->
                mavenId.equals(
                    dependency.getGroupId().getStringValue(),
                    dependency.getArtifactId().getStringValue()));
  }

  /**
   * Returns a new {@link MavenId} whose values are based on the given {@link
   * CloudLibraryClientMavenCoordinates}.
   *
   * @param mavenCoordinates the {@link CloudLibraryClientMavenCoordinates} to convert to a {@link
   *     MavenId}
   */
  private static MavenId toMavenId(CloudLibraryClientMavenCoordinates mavenCoordinates) {
    return new MavenId(
        mavenCoordinates.getGroupId(),
        mavenCoordinates.getArtifactId(),
        mavenCoordinates.getVersion());
  }
}
