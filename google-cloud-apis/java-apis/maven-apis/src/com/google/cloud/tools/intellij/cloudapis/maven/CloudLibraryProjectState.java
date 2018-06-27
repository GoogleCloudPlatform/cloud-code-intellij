/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.cloudapis.maven;

import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.util.xml.GenericValue;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.idea.maven.dom.MavenDomUtil;
import org.jetbrains.idea.maven.dom.model.MavenDomDependencies;
import org.jetbrains.idea.maven.dom.model.MavenDomDependency;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.project.MavenProjectsManager.Listener;

/**
 * A {@link ProjectComponent} that maintains, in memory, the current set of {@link CloudLibrary}
 * that are configured on the user's project.
 */
public class CloudLibraryProjectState implements ProjectComponent {

  private final Project project;

  private Map<Module, Set<CloudLibrary>> moduleLibraryMap = Maps.newHashMap();
  // Map of project module to BOM version in the module's pom.xml
  private Map<Module, Optional<String>> moduleBomVersionMap = Maps.newHashMap();

  private List<CloudLibrary> allLibraries;

  private CloudLibraryProjectState(Project project) {
    this.project = project;
  }

  public static CloudLibraryProjectState getInstance(Project project) {
    return project.getComponent(CloudLibraryProjectState.class);
  }

  /**
   * Attaches a listener to listen for changes to a module's pom.xml to request a resync of the
   * project's managed dependencies.
   *
   * <p>If the project is not a maven project, then the listener simply isn't fired. If the project
   * is not a maven project, but later becomes one, then the listener will be fired as expected and
   * the managed libraries will be synchronized.
   */
  @Override
  public void projectOpened() {
    // todo (eshaul) handle Gradle & native projects and ensure that double syncing won't occur
    MavenProjectsManager.getInstance(project)
        .addManagerListener(
            new Listener() {
              @Override
              public void projectsScheduled() {
                syncManagedProjectLibraries();
                syncCloudLibrariesBom();
              }
            });
  }

  /** Returns the set of {@link CloudLibrary} currently configured on the given {@link Module}. */
  Set<CloudLibrary> getCloudLibraries(Module module) {
    return moduleLibraryMap.getOrDefault(module, ImmutableSet.of());
  }

  public Optional<String> getCloudLibraryBomVersion(Module module) {
    return moduleBomVersionMap.getOrDefault(module, Optional.empty());
  }

  /**
   * Updates the project's mapping of {@link Module} to {@link CloudLibrary} with the currently
   * configured set of managed client libraries.
   */
  @VisibleForTesting
  void syncManagedProjectLibraries() {
    moduleLibraryMap =
        Stream.of(ModuleManager.getInstance(project).getModules())
            .collect(Collectors.toMap(Function.identity(), this::loadManagedLibraries));
  }

  public void syncCloudLibrariesBom() {
    moduleBomVersionMap =
        Stream.of(ModuleManager.getInstance(project).getModules())
            .collect(Collectors.toMap(Function.identity(), this::loadCloudLibraryBomVersion));
  }

  /**
   * Loads the set of managed {@link CloudLibrary} configured in the current {@link Module}.
   *
   * <p>Does so by fetching the dependencies configured in the user's build and comparing them to
   * the set of all available GCP client libraries and filtering out the non-managed ones.
   */
  private Set<CloudLibrary> loadManagedLibraries(Module module) {
    List<MavenDomDependency> moduleDependencies = getModuleDependencies(module);

    if (moduleDependencies == null || moduleDependencies.isEmpty()) {
      return ImmutableSet.of();
    }

    // Only fetch the libraries once
    if (allLibraries == null) {
      allLibraries = CloudLibrariesService.getInstance().getCloudLibraries();
    }

    return allLibraries
        .stream()
        .filter(library -> isManagedDependencyInModule(library, moduleDependencies))
        .collect(Collectors.toSet());
  }

  private Optional<String> loadCloudLibraryBomVersion(Module module) {
    return ApplicationManager.getApplication()
        .runReadAction(
            (Computable<Optional<String>>)
                () ->
                    loadCloudLibraryBom(module)
                        .map(MavenDomDependency::getVersion)
                        .map(GenericValue::getStringValue));
  }

  /**
   * Fetches the {@link MavenDomDependency} DOM element corresponding to the google-cloud-java BOM
   * in the {@link Module} pom.xml.
   *
   * @param module the {@link Module} from which to load the BOM DOM element
   * @return the optional {@link MavenDomDependency} corresponding the the BOM element in the
   *     pom.xml
   */
  Optional<MavenDomDependency> loadCloudLibraryBom(Module module) {
    return ApplicationManager.getApplication()
        .runReadAction(
            (Computable<Optional<MavenDomDependency>>)
                () -> {
                  MavenProject mavenProject =
                      MavenProjectsManager.getInstance(module.getProject()).findProject(module);

                  if (mavenProject == null) {
                    return Optional.empty();
                  }

                  MavenDomProjectModel model =
                      MavenDomUtil.getMavenDomProjectModel(project, mavenProject.getFile());

                  if (model == null) {
                    return Optional.empty();
                  }

                  MavenDomDependencies mavenDomDependencies =
                      model.getDependencyManagement().getDependencies();

                  return mavenDomDependencies
                      .getDependencies()
                      .stream()
                      .filter(
                          managedDependency ->
                              CloudApiMavenService.GOOGLE_CLOUD_JAVA_BOM_ARTIFACT.equalsIgnoreCase(
                                      managedDependency.getArtifactId().getStringValue())
                                  && CloudApiMavenService.GOOGLE_CLOUD_JAVA_BOM_GROUP
                                      .equalsIgnoreCase(
                                          managedDependency.getGroupId().getStringValue()))
                      .findFirst();
                });
  }

  /**
   * Returns a list of all {@link MavenDomDependency} currently configured in the module's pom.xml.
   */
  private List<MavenDomDependency> getModuleDependencies(Module module) {
    MavenProject mavenProject = MavenProjectsManager.getInstance(project).findProject(module);
    if (mavenProject == null) {
      return ImmutableList.of();
    }

    return ApplicationManager.getApplication()
        .runReadAction(
            (Computable<List<MavenDomDependency>>)
                () -> {
                  MavenDomProjectModel model =
                      MavenDomUtil.getMavenDomProjectModel(project, mavenProject.getFile());

                  return model != null
                      ? model
                          .getDependencies()
                          .getDependencies()
                          .stream()
                          .map(dependency -> (MavenDomDependency) dependency.createStableCopy())
                          .collect(Collectors.toList())
                      : ImmutableList.of();
                });
  }

  /**
   * Checks to see if the given {@link CloudLibrary} is present in the list of {@link
   * MavenDomDependency}.
   */
  private boolean isManagedDependencyInModule(
      CloudLibrary library, List<MavenDomDependency> moduleDependencies) {
    return ApplicationManager.getApplication()
        .runReadAction(
            (Computable<Boolean>)
                () ->
                    CloudLibraryUtils.getFirstJavaClientMavenCoordinates(library)
                        .map(
                            coordinates ->
                                MavenUtils.isMavenIdInDependencyList(
                                    MavenUtils.toMavenId(coordinates), moduleDependencies))
                        .orElse(false));
  }
}
