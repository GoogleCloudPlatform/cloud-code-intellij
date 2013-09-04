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

package com.google.gct.intellij.endpoints.action;

import com.google.gct.intellij.endpoints.GctConstants;
import com.google.gct.intellij.endpoints.project.AppEngineMavenProject;
import com.google.gct.intellij.endpoints.util.VfsUtils;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.VfsUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Action to generate client libraries for an AppEngine endpoints project and copy them to an associated android project
 */
public class GenerateClientLibrariesAction extends AnAction {

  private static final String ERROR_MESSAGE_TITLE = "Failed to Generate Client Libraries";
  private static final Logger LOG = Logger.getInstance(GenerateClientLibrariesAction.class);

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getProject();
    final Module appEngineModule = e.getData(LangDataKeys.MODULE);

    if(project == null || appEngineModule == null) {
      Messages.showErrorDialog(project, "Please select an App Engine module", ERROR_MESSAGE_TITLE);
      return;
    }

    AppEngineMavenProject appEngineMavenProject = AppEngineMavenProject.get(appEngineModule);
    if(appEngineMavenProject == null) {
      Messages.showErrorDialog(project, "Please select a valid Maven enabled App Engine project", ERROR_MESSAGE_TITLE);
      return;
    }

    Module androidLibModule =
      ModuleManager.getInstance(project).findModuleByName(project.getName() + GctConstants.ANDROID_GCM_LIB_MODULE_SUFFIX);
    if(androidLibModule == null) {
      Messages.showWarningDialog(project, "Could not find associated Android library module (" +
                                          project.getName() + GctConstants.ANDROID_GCM_LIB_MODULE_SUFFIX +
                                          "), skipping copying of client libraries into Android", "Warning");
    }

    doAction(appEngineMavenProject, androidLibModule);
  }

  void doAction(@NotNull final AppEngineMavenProject appEngineMavenProject, @Nullable final Module androidLibModule) {

    appEngineMavenProject.runGenClientLibraries(new AppEngineMavenProject.MavenBuildCallback() {
      @Override
      public void onBuildCompleted(int resultCode, String text) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            try {
              appEngineMavenProject.expandAllClientLibs();
              ApplicationManager.getApplication().runWriteAction(new ThrowableComputable<Void, FileNotFoundException>() {
                @Override
                public Void compute() throws FileNotFoundException {
                  appEngineMavenProject.refreshExpandedSourceDir(false, null);
                  if(androidLibModule != null) {
                    appEngineMavenProject.copyExpandedSources(VfsUtil.virtualToIoFile(VfsUtils.findModuleRoot(androidLibModule)));
                  }
                  return null;
                }
              });
            }
            catch (IOException e) {
              LOG.error("Failed to expand Client Libraries from downloaded zips", e);
            }
          }
        });
      }
    });
  }

}
