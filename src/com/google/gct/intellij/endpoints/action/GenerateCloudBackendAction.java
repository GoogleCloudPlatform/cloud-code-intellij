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

import com.android.tools.idea.model.ManifestInfo;

import com.google.gct.intellij.endpoints.GctConstants;
import com.google.gct.intellij.endpoints.generator.sample.CloudBackendGenerator;
import com.google.gct.intellij.endpoints.action.ui.GenerateBackendDialog;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;

import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.sdk.AndroidSdkUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.utils.MavenUtil;

import java.io.File;

/**
 * Action to generate an App Engine cloud backend with an associated Android library module
 */
public class GenerateCloudBackendAction extends AnAction {

  private static final String MESSAGE_TITLE = "Generate Cloud Backend";

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Module androidModule = e.getData(LangDataKeys.MODULE);
    final Project project = e.getProject();

    if(project == null || androidModule == null) {
      Messages.showErrorDialog(project, "Please select an android module to create a backend for",
                               MESSAGE_TITLE);
      return;
    }

    if (AndroidFacet.getInstance(androidModule) == null) {
      Messages.showErrorDialog(project, "Selected Module : " + androidModule.getName() + " is not an Android Module. If you selected the " +
                                        "root project, try selecting the Android Module instead.",
                               MESSAGE_TITLE);
      return;
    }

    if(!passesExternalChecks(project)) {
      return;
    }

    // Make sure that the dir does not exist
    final String appEngineModuleName = androidModule.getName() + GctConstants.APP_ENGINE_MODULE_SUFFIX;
    final String appEngineLfsModuleFileDir = project.getBasePath() + File.separatorChar + appEngineModuleName;
    if (LocalFileSystem.getInstance().findFileByPath(appEngineLfsModuleFileDir) != null) {
      Messages.showErrorDialog(project, "Unable to generate an App Engine Backend named '" + appEngineModuleName + "'.\n" +
                                        "The directory '" + appEngineLfsModuleFileDir + "' already exists.",
                               MESSAGE_TITLE);
      return;
    }

    // Look for the root package
    final ManifestInfo manifestInfo = ManifestInfo.get(androidModule);
    if (manifestInfo == null) {
      Messages.showErrorDialog(project, "Unable to generate an App Engine Backend named '" + appEngineModuleName + "'.\n" +
                                        "Cannot find the AndroidManifest.xml for the '" + androidModule.getName() + "' module.",
                               MESSAGE_TITLE);
      return;
    }

    final String rootPackage = manifestInfo.getPackage();
    if (rootPackage == null || rootPackage.isEmpty()) {
      Messages.showErrorDialog(project, "Unable to generate an App Engine Backend named '" + appEngineModuleName + "'.\n" +
                                        "Cannot find a package attribute in the <manifest> tag of AndroidManifest.xml.",
                               MESSAGE_TITLE);
      return;
    }

    final String androidEndpointsLibModuleName = androidModule.getName() + GctConstants.ANDROID_GCM_LIB_MODULE_SUFFIX;
    final File androidLibModuleRoot = new File(project.getBasePath(), androidEndpointsLibModuleName);
    if (androidLibModuleRoot.exists()) {
      Messages.showErrorDialog(project, "Unable to generate an Android Library named named '" + androidEndpointsLibModuleName + "'.\n" +
                                        "The directory '" + androidLibModuleRoot.getPath() + "' already exists.",
                               MESSAGE_TITLE);
      return;
    }

    final GenerateBackendDialog dialog = new GenerateBackendDialog(project);
    dialog.show();
    if(dialog.isOK()) {
      doAction(project, androidModule, dialog);
    }
  }

  void doAction(final Project project, Module androidModule, GenerateBackendDialog dialog) {
    new CloudBackendGenerator(project, androidModule, dialog).generate(new CloudBackendGenerator.Callback() {
      @Override
      public void backendCreated(@NotNull Module androidLibModule, @NotNull Module appEngineModule) {
        // We don't need to do anything here, the process is complete.
      }

      @Override
      public void onFailure(final String errorMessage) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            Messages.showErrorDialog(project, "An error occurred during backend generation : " + errorMessage,
                                     MESSAGE_TITLE);
          }
        });
      }
    });
  }

  protected boolean passesExternalChecks(Project project) {
    File mavenHome = MavenUtil.resolveMavenHomeDirectory(null);
    if(mavenHome == null) {
      Messages.showErrorDialog(project, "Could not find a valid installation of maven. " +
                                        "Perhaps you have not installed Maven or have not setup your Maven paths", MESSAGE_TITLE);
      return false;
    }

    String mavenVersion = MavenUtil.getMavenVersion(mavenHome.getAbsolutePath());
    if(StringUtil.compareVersionNumbers("3", mavenVersion) > 0) {
      Messages.showErrorDialog(project, "Cloud Endpoints requires Maven version 3 or higher", MESSAGE_TITLE);
      return false;
    }

    if(MavenProjectsManager.getInstance(project).getGeneralSettings().isWorkOffline()) {
      Messages.showErrorDialog(project, "Could not connect to MavenCentral because Maven is in 'offline' mode. Switch maven to 'online' " +
                                        "mode and try again", MESSAGE_TITLE);
      return false;
    }

    if(!new File(AndroidSdkUtils.tryToChooseAndroidSdk().getLocation() + GctConstants.ANDROID_SDK_GCM_PATH).exists()) {
      Messages.showErrorDialog(project, "Could not find gcm.jar. Please install the Android " +
                                        "SDK Extra : 'Google Cloud Messaging for Android Library' using the Android SDK Manager.",
                               MESSAGE_TITLE);
      return false;
    }
    return true;
  }
}

