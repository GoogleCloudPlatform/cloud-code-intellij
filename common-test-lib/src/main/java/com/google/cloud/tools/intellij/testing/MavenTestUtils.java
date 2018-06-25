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

package com.google.cloud.tools.intellij.testing;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import java.io.IOException;
import java.util.function.Consumer;
import org.jdom.JDOMException;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.server.MavenServerManager;
import org.jetbrains.idea.maven.wizards.MavenModuleBuilder;

/** Utilities for setting up and working with Maven components in tests. */
public class MavenTestUtils {

  private static final MavenTestUtils INSTANCE = new MavenTestUtils();

  private MavenTestUtils() {}

  public static MavenTestUtils getInstance() {
    return INSTANCE;
  }

  /** Runs the supplied {@link Consumer mavenModuleConsumer} with a newly created Maven module. */
  public void runWithMavenModule(Project project, Consumer<Module> mavenModuleConsumer) {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              try {
                Module mavenModule =
                    MavenTestUtils.getInstance()
                        .createNewMavenModule(initMavenModuleBuilder(project), project);

                mavenModuleConsumer.accept(mavenModule);
              } finally {
                MavenServerManager.getInstance().shutdown(true);
              }
            });
  }

  /** Creates a new Maven module for use in tests. */
  private Module createNewMavenModule(MavenModuleBuilder moduleBuilder, Project project) {
    return ApplicationManager.getApplication()
        .runWriteAction(
            (Computable<Module>)
                () -> {
                  ModifiableModuleModel model =
                      ModuleManager.getInstance(project).getModifiableModel();
                  Module module;
                  try {
                    module = moduleBuilder.createModule(model);
                  } catch (IOException
                      | ModuleWithNameAlreadyExists
                      | JDOMException
                      | ConfigurationException e) {
                    throw new AssertionError("Error creating Mavenized module");
                  }
                  model.commit();

                  resolveDependenciesAndImport(project);
                  return module;
                });
  }

  /** Creates a new {@link MavenModuleBuilder} that can be used to create Maven modules in tests. */
  private MavenModuleBuilder initMavenModuleBuilder(Project project) {
    MavenModuleBuilder moduleBuilder = new MavenModuleBuilder();
    MavenId mavenId = new MavenId("org.foo", "module", "1.0");

    String root = project.getBasePath();
    moduleBuilder.setName("module");
    moduleBuilder.setModuleFilePath(root + "/module.iml");
    moduleBuilder.setContentEntryPath(root);
    moduleBuilder.setProjectId(mavenId);

    return moduleBuilder;
  }

  private void resolveDependenciesAndImport(Project project) {
    MavenProjectsManager myProjectsManager = MavenProjectsManager.getInstance(project);
    myProjectsManager.waitForResolvingCompletion();
    myProjectsManager.performScheduledImportInTests();
  }
}
