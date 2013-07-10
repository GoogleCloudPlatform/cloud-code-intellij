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

package com.google.gct.intellij.endpoints.actions;

import com.google.gct.intellij.endpoints.EndpointsConstants;
import com.google.gct.intellij.endpoints.generator.GradleGcmGeneratorHelper;
import com.google.gct.intellij.endpoints.generator.MavenEndpointGeneratorHelper;
import com.google.gct.intellij.endpoints.util.MavenUtils;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;

import java.io.File;
import java.util.Collections;

/**
 * Action to generate client libraries for an AppEngine endpoints project and copy them to an associated android project
 */
public class GenerateClientLibrariesForMaven extends AnAction {

  private static final Logger LOG = Logger.getInstance(GenerateClientLibrariesForMaven.class);

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getData(LangDataKeys.PROJECT);
    final Module appEngineModule = e.getData(LangDataKeys.MODULE);
    final MavenEndpointGeneratorHelper endpointGeneratorHelper = new MavenEndpointGeneratorHelper(project, appEngineModule);

    endpointGeneratorHelper.regenerateAllClientLibraries(new MavenEndpointGeneratorHelper.LibGenerationCallback() {
      @Override
      public void onGenerationComplete(final boolean generationSuccessful) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {

            if (!generationSuccessful) {
              Messages.showErrorDialog(project, "There was a failure when generating the client libraries for the " +
                                                appEngineModule.getName() +
                                                " module. See the Maven console for details.", "Generate Client Libraries");

              return;
            }

            String androidGcmLibModuleName = GradleGcmGeneratorHelper.getGcmLibModuleNameFromAppEngineModule(appEngineModule.getName());
            Module androidGcmModule = ModuleManager.getInstance(project).findModuleByName(androidGcmLibModuleName);

            if (androidGcmModule == null) {
              LOG.info("No associated Android module (" + androidGcmLibModuleName + ") found. Skipping copying of endpoint source.");
              return;
            }

            final File gcmLibModuleDir =
              (androidGcmModule.getModuleFile() != null ? VfsUtil.virtualToIoFile(androidGcmModule.getModuleFile().getParent()) : null);
            if (gcmLibModuleDir == null) {
              LOG.warn(
                "Cannot find root directory for Android module " + androidGcmLibModuleName + ". Skipping copying of endpoint source.");
              return;
            }

            File googleGeneratedDir = new File(VfsUtil.virtualToIoFile(appEngineModule.getModuleFile().getParent()),
                                               EndpointsConstants.APP_ENGINE_GENERATED_LIB_DIR);
            // refresh before copying endpoints so that the ide can pick up the files... perhaps it is better to do this using java io
            // rather than IDE io
            LocalFileSystem.getInstance().refreshIoFiles(Collections.singletonList(googleGeneratedDir), true, true, new Runnable() {

              @Override
              public void run() {
                ApplicationManager.getApplication().invokeLater(new Runnable() {

                  @Override
                  public void run() {
                    endpointGeneratorHelper.copyEndpointSourceAndDepsToAndroidModule(gcmLibModuleDir);
                    CompilerManager.getInstance(project).make(null);
                  }
                });
              }
            });
            // endpointGeneratorHelper.copyEndpointSourceAndDepsToAndroidModule(gcmLibModuleDir);
            // CompilerManager.getInstance(project).make(null);
            // Trigger a build
          }
        });
      }
    });
  }

  /**
   * Activate this option only if it's a maven based app engine project
   */
  @Override
  public void update(AnActionEvent e) {
    final Module module = e.getData(LangDataKeys.MODULE);
    final Project project = e.getData(DataKeys.PROJECT);

    e.getPresentation().setEnabled(MavenUtils.isMavenProjectWithAppEnginePlugin(project, module));
  }
}
