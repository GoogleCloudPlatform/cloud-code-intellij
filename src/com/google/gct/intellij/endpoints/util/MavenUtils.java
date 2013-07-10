/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.google.gct.intellij.endpoints.util;

import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.Semaphore;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType;
import org.jetbrains.idea.maven.execution.MavenRunnerParameters;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.util.List;

/**
 * Utilities for working with maven
 */
public class MavenUtils {


  /** interface for a callback when a maven build completes */
  public static interface MavenBuildCallback {
    void onBuildCompleted(int resultCode);
  }

  /**
   * NOTE : requires readAction if not coming from the dispatch thread
   * Retreive the maven project given a module
   *
   * TODO: This function might have outlived its original use, both calls to it know the module,
   * TODO: so just send that in, instead of the module name
   * @param p
   * @param moduleName
   * @return the module, <code>null</code> otherwise
   */
  @Nullable
  public static MavenProject getMavenProjectForModule(Project p, String moduleName) {
    Module module = ModuleManager.getInstance(p).findModuleByName(moduleName);
    if (module == null) {
      return null;
    }

    return MavenProjectsManager.getInstance(p).findProject(module);
  }

  /**
   * NOTE : must be invoked on the dispatch thread
   * Check if the Maven Project has the Maven App Engine plugin
   * @param p
   * @param m
   * @return
   */
  public static boolean isMavenProjectWithAppEnginePlugin(Project p, Module m) {

    MavenProject mavenProject = MavenProjectsManager.getInstance(p).findProject(m);
    if (mavenProject == null) {
      return false;
    }

    if (mavenProject.findPlugin("com.google.appengine", "appengine-maven-plugin") == null) {
      return false;
    }

    return true;
  }

  /**
   * NOTE : Needs to be executed on the dispatch thread
   * Asynchronous call to run the maven builder; callback is invoked once build is complete.
   * @param project
   * @param rootDir
   * @param goalsToRun
   * @param callback
   */
  public static void runMavenBuilder(final Project project,
                                     final VirtualFile rootDir,
                                     List<String> goalsToRun,
                                     @Nullable final MavenBuildCallback callback) {

    MavenProject mavenProject = MavenProjectsManager.getInstance(project).findProject(rootDir.findChild("pom.xml"));
    assert (mavenProject != null);

    final MavenRunnerParameters params = new MavenRunnerParameters(true, mavenProject.getDirectory(), goalsToRun,
                                                                   MavenProjectsManager.getInstance(project).getExplicitProfiles());

    final Semaphore targetDone = new Semaphore();
    final int[] result = new int[1];

    targetDone.down();

    MavenRunConfigurationType.runConfiguration(project, params, new ProgramRunner.Callback() {
      @Override
      public void processStarted(RunContentDescriptor descriptor) {
        ProcessHandler processHandler = descriptor.getProcessHandler();
        if (processHandler != null) {
          processHandler.addProcessListener(new ProcessAdapter() {
            @Override
            public void processTerminated(ProcessEvent event) {
              result[0] = event.getExitCode();
              targetDone.up();
            }
          });
        }
      }
    });

    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
      @Override
      public void run() {
        // FIXME: Don't wait forever.
        targetDone.waitFor();

        if (callback != null) {
          ApplicationManager.getApplication().invokeLater(new Runnable() {

            @Override
            public void run() {
              callback.onBuildCompleted(result[0]);
            }
          });
        }
      }
    });
  }
}
