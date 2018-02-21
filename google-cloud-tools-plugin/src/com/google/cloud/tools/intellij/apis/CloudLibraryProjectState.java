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

package com.google.cloud.tools.intellij.apis;

import com.google.cloud.tools.libraries.CloudLibraries;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.fest.util.Maps;
import org.jetbrains.idea.maven.dom.MavenDomUtil;
import org.jetbrains.idea.maven.dom.model.MavenDomDependency;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.project.MavenProjectsManager.Listener;

public class CloudLibraryProjectState implements ProjectComponent {

  private static final Logger logger = Logger.getInstance(AddCloudLibrariesAction.class);

  private final Project project;
  private Map<Module, Set<CloudLibrary>> moduleLibraryMap = Maps.newHashMap();
  private List<CloudLibrary> allLibraries;

  private CloudLibraryProjectState(Project project) {
    this.project = project;

    try {
      allLibraries = ImmutableList.copyOf(CloudLibraries.getCloudLibraries());
    } catch (IOException e) {
      logger.error(e);
      allLibraries = ImmutableList.of();
    }
  }

  @Override
  public void projectOpened() {
    // todo figure out how to deal with eventual maven/gradle and ensure that double synching
    // doesn't occur if multiple build systems exist
    // todo what happens if this is not a maven project? (and later becomes one?)
    MavenProjectsManager.getInstance(project)
        .addManagerListener(
            new Listener() {
              @Override
              public void projectsScheduled() {
                syncManagedProjectLibraries();
              }
            });
  }

  Set<CloudLibrary> getManagedLibraries(Module module) {
    return moduleLibraryMap.get(module);
  }

  private void syncManagedProjectLibraries() {
    moduleLibraryMap =
        Stream.of(ModuleManager.getInstance(project).getModules())
            .collect(Collectors.toMap(Function.identity(), this::loadManagedLibraries));
  }

  private Set<CloudLibrary> loadManagedLibraries(Module module) {
    List<MavenDomDependency> moduleDependencies = getModuleDependencies(module);

    return allLibraries
        .stream()
        .filter(library -> isManagedDependencyInModule(library, moduleDependencies))
        .collect(Collectors.toSet());
  }

  private List<MavenDomDependency> getModuleDependencies(Module module) {
    MavenProject mavenProject = MavenProjectsManager.getInstance(project).findProject(module);

    return ApplicationManager.getApplication()
        .runReadAction(
            (Computable<List<MavenDomDependency>>)
                () -> {
                  MavenDomProjectModel model =
                      MavenDomUtil.getMavenDomProjectModel(project, mavenProject.getFile());

                  return model.getDependencies().getDependencies();
                });
  }

  private boolean isManagedDependencyInModule(
      CloudLibrary library, List<MavenDomDependency> moduleDependencies) {
    return ApplicationManager.getApplication()
        .runReadAction(
            (Computable<Boolean>)
                () ->
                    CloudLibraryUtils.getFirstJavaClientMavenCoordinates(library)
                        .map(
                            coordinates ->
                                CloudLibraryDependencyWriter.isMavenIdInDependencyList(
                                    CloudLibraryDependencyWriter.toMavenId(coordinates),
                                    moduleDependencies))
                        .orElse(false));
  }
}
