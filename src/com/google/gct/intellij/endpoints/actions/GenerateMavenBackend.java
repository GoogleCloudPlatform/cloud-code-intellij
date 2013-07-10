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

import com.android.tools.idea.rendering.ManifestInfo;

import com.google.gct.intellij.endpoints.EndpointsConstants;
import com.google.gct.intellij.endpoints.generator.GradleGcmGeneratorHelper;
import com.google.gct.intellij.endpoints.generator.MavenBackendGeneratorHelper;
import com.google.gct.intellij.endpoints.generator.MavenEndpointGeneratorHelper;
import com.google.gct.intellij.endpoints.ui.GenerateBackendDialog;
import com.google.gct.intellij.endpoints.util.MavenUtils;

import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.sdk.AndroidSdkUtils;
import org.jetbrains.idea.maven.execution.MavenRunConfiguration;
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType;
import org.jetbrains.idea.maven.execution.MavenRunnerParameters;
import org.jetbrains.idea.maven.utils.MavenUtil;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

/**
 * An Action to create a full AppEngine project as an endpoints backend for an Android project
 */
public class GenerateMavenBackend extends AnAction {

  private static final Logger LOG = Logger.getInstance(GenerateMavenBackend.class);

  @Override
  public void actionPerformed(AnActionEvent e) {

    // Get module information and generate potential name for new App Engine module
    final Module androidModule = e.getData(LangDataKeys.MODULE);
    final Project project = e.getData(DataKeys.PROJECT);


    if (AndroidFacet.getInstance(androidModule) == null) {
      Messages.showErrorDialog(project, "Unable to generate an App Engine Backed.  " +
                                        "The module '" + androidModule.getName() + "' is not an Android Module. " +
                                        "If you selected the root project, try selecting the Android Module instead.",
                               "Generate App Engine Backend");
      return;
    }

    if(!checkExternalDependencies(project, androidModule)) {
      return;
    }

    final String appEngineModuleName = MavenBackendGeneratorHelper.getAppEngineModuleName(androidModule.getName());
    final String appEngineLfsModuleFileDir = project.getBasePath() +
                                               '/' +
                                               appEngineModuleName;

    // Make sure that the dir does not exist
    if (LocalFileSystem.getInstance().findFileByPath(appEngineLfsModuleFileDir) != null) {
      Messages.showErrorDialog(project, "Unable to generate an App Engine Backend named '" + appEngineModuleName + "'.\n" +
                                        "The directory '" + appEngineLfsModuleFileDir + "' already exists.", "Generate App Engine Backend");
      return;
    }

    // Look for the root package
    ManifestInfo manifestInfo = ManifestInfo.get(androidModule);

    if (manifestInfo == null) {
      Messages.showErrorDialog(project, "Unable to generate an App Engine Backend named '" + appEngineModuleName + "'.\n" +
                                        "Cannot find the AndroidManifest.xml for the '" + androidModule.getName() + "' module.",
                               "Generate App Engine Backend");
      return;
    }

    final String rootPackage = manifestInfo.getPackage();
    if (rootPackage == null || rootPackage.isEmpty()) {
      // Don't need to check for at least two separators, IJ Android Module creation forces at least two segments.
      Messages.showErrorDialog(project, "Unable to generate an App Engine Backend named '" + appEngineModuleName + "'.\n" +
                                        "Cannot find a package attribute in the <manifest> tag of AndroidManifest.xml.",
                               "Generate App Engine Backend");
    }

    final String androidEndpointsLibModuleName = androidModule.getName() + EndpointsConstants.ANDROID_GCM_LIB_MODULE_SUFFIX;

    final File androidLibModuleRoot = new File(project.getBasePath(), androidEndpointsLibModuleName);
    if (androidLibModuleRoot.exists()) {
      Messages.showErrorDialog(project, "Unable to generate an Android Library named named '" + androidEndpointsLibModuleName + "'.\n" +
                                        "The directory '" + androidLibModuleRoot.getPath() + "' already exists.",
                               "Generate App Engine Backend");
      return;
    }

    // TODO: Add logic to the dialog to check for the presence of Maven, and warn if it isn't there.
    final GenerateBackendDialog genBackendDialog = new GenerateBackendDialog(project);
    genBackendDialog.show();

    if (!genBackendDialog.isOK()) {
      return;
    }

    // Generate the App Engine module
    final VirtualFile appEngineModuleRootDir = MavenBackendGeneratorHelper.createAppEngineDirStructure(project, androidModule);
    MavenBackendGeneratorHelper
      .addAppEngineSampleCode(project, appEngineModuleRootDir, rootPackage, genBackendDialog.getAppId(), genBackendDialog.getApiKey());
    MavenBackendGeneratorHelper.addMavenFunctionality(project, appEngineModuleRootDir, rootPackage);

    // Build the project and generate the client libraries
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        // Build the generator module, and generate the client libs
        MavenBackendGeneratorHelper.
          buildModuleAndGenClientlibs(project, appEngineModuleRootDir, new MavenUtils.MavenBuildCallback() {

            // Back on dispatch thread
            @Override
            public void onBuildCompleted(int resultCode) {

              if (resultCode != 0) {
                Messages
                  .showErrorDialog(project, "Unable to generate the Android GCM component for the '" + androidModule.getName() +
                                            "' module." + "The App Engine module failed to build.", "Generate App Engine Backend");
                return;
              }

              Module appEngineModule = ModuleManager.getInstance(project).findModuleByName(appEngineModuleName);
              assert (appEngineModule != null);

              MavenEndpointGeneratorHelper endpointGeneratorHelper = new MavenEndpointGeneratorHelper(project, appEngineModule);

              for (String apiName : MavenBackendGeneratorHelper.SAMPLE_API_NAMES) {
                endpointGeneratorHelper.expandSourceDirForApi(apiName);
              }

              // Now, perform a refresh so that the generated endpoint libs folder (google_generated) shows up
              File generatedLibsDir =
                new File(VfsUtil.virtualToIoFile(appEngineModuleRootDir), EndpointsConstants.APP_ENGINE_GENERATED_LIB_DIR);
              LocalFileSystem.getInstance().refreshIoFiles(Collections.singletonList(generatedLibsDir), true, true, null);


              addMavenRunConfiguration(project, appEngineModule);
              GradleGcmGeneratorHelper
                .generateAndroidGcmLib(project, appEngineModuleRootDir, androidModule, androidLibModuleRoot, rootPackage,
                                       appEngineModuleName, androidEndpointsLibModuleName, genBackendDialog.getProjectNumber());

            }

          });
      }
    });
  }

  /** Only activate on android projects */
  @Override
  public void update(AnActionEvent e) {
    e.getPresentation().setEnabled(true);
    /*
    boolean isEnabled = false;
    Module m = e.getData(LangDataKeys.MODULE);
    if (m != null && AndroidFacet.getInstance(m) != null) {
      // TODO: Don't allow this action on the gcm library that we've already generated. A better check
      //   would be for any library at all. We shouldn't allow the generation of backends against an Android Library.
      if (!m.getName().endsWith(GradleGcmGeneratorHelper.ANDROID_GCM_LIB_MODULE_NAME_SUFFIX)) {
        isEnabled = true;
      }
    }
    e.getPresentation().setEnabled(isEnabled);
    */
  }

  private boolean checkExternalDependencies(Project project, Module androidModule) {
    if(null == MavenUtil.resolveMavenHomeDirectory(null)) {
      Messages.showErrorDialog(project, "Unable to generate the Android GCM component for the '" + androidModule.getName() +
                                        "' module.  Could find a valid installation of maven.  " +
                                        "Perhaps you have not installed Maven2 or have not defined M2_HOME", "Generate App Engine Backend");
    }
    if(!new File(AndroidSdkUtils.tryToChooseAndroidSdk().getLocation() + EndpointsConstants.ANDROID_SDK_GCM_PATH).exists()) {
      Messages.showErrorDialog(project,
                               "Unable to generate an AppEngine backend. Could not find gcm.jar. Please install the Android " +
                               "SDK Extra : 'Google Cloud Messaging for Android Library' using the Android SDK Manager.",
                               "Generate App Engine Backend");
      return false;
    }
    return true;
  }

  private static final String DEV_APP_SERVER_MAVEN_GOAL = "appengine:devserver";
  private void addMavenRunConfiguration(Project project, Module appEngineModule) {
    ConfigurationFactory type = MavenRunConfigurationType.getInstance().getConfigurationFactories()[0];
    RunManagerEx runManagerEx = RunManagerEx.getInstanceEx(project);
    String configName = appEngineModule.getName() + " [" + DEV_APP_SERVER_MAVEN_GOAL + "]";
    // don't create a run configuration if one already exists, LOG it
    for (RunConfiguration config : runManagerEx.getConfigurations(MavenRunConfigurationType.getInstance())) {
      if (config.getName().equals(configName)) {
        LOG.error("Failed to add run configuration for appengine maven project (" +
                  appEngineModule.getName() +
                  "), one already exists.");
        return;
      }
    }
    RunnerAndConfigurationSettings settings = runManagerEx.createConfiguration(configName, type);
    MavenRunConfiguration configuration = (MavenRunConfiguration) settings.getConfiguration();
    MavenRunnerParameters params = configuration.getRunnerParameters();
    params.setGoals(Arrays.asList(DEV_APP_SERVER_MAVEN_GOAL));
    // the standard maven run configuration generated from the maven panel isn't "shared",
    // so lets stay in line with this for now
    runManagerEx.addConfiguration(
      settings,
      false,
      runManagerEx.getBeforeRunTasks(settings.getConfiguration()),
      false
    );
  }
}
