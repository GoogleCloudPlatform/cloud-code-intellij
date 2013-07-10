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

package com.google.gct.intellij.endpoints.generator;

import com.android.SdkConstants;
import com.android.tools.idea.gradle.project.GradleProjectImporter;
import com.android.tools.idea.templates.Template;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.gct.intellij.endpoints.EndpointsConstants;
import com.google.gct.intellij.endpoints.templates.android.GradleAndroidTemplateHelper;
import com.google.gct.intellij.endpoints.util.FacetUtils;

import com.google.gct.intellij.endpoints.util.GradleUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.PathUtil;
import com.intellij.util.SystemProperties;

import org.jetbrains.android.sdk.AndroidSdkUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Functionality to create the Android part (as a library module) of the GCM/Endpoints project additions
 */
public class GradleGcmGeneratorHelper {

  /**
   * Finds include ':module_name_1', ':module_name_2',... statements in settings.gradle files
   */
  private static final Pattern INCLUDE_PATTERN = Pattern.compile("include +(':[^']+', *)*':[^']+'");

  private static final String SDKTEMPLATES = "sdktemplates";
  private static final String GCM_ACTIVITY = "GcmActivity";

  private static final Logger LOG = Logger.getInstance(GradleGcmGeneratorHelper.class);

  /**
   * Get the name of the android endpoints module associated
   * @param appEngineModuleName
   * @return
   */
  public static String getGcmLibModuleNameFromAppEngineModule(String appEngineModuleName) {
    assert (appEngineModuleName.endsWith(EndpointsConstants.APP_ENGINE_MODULE_SUFFIX));
    return appEngineModuleName.replace(EndpointsConstants.APP_ENGINE_MODULE_SUFFIX, EndpointsConstants.ANDROID_GCM_LIB_MODULE_SUFFIX);
  }

  /**
   * Returns the endpoints source directory root for the Android GCM Library. This directory may not exist.
   *
   * @param androidGcmLibModuleRoot The root folder for the androidGcmLib module (i.e. the main content root; there should only be one).
   *                                This folder must exist.
   * @return The endpoints source directory root for the Android GCM Library. This folder may not exist.
   */
  public static File getEndpointSrcDir(File androidGcmLibModuleRoot) {
    File endpointSrcDir = new File(androidGcmLibModuleRoot, EndpointsConstants.ANDROID_ENDPOINT_SRC_PATH);
    return endpointSrcDir;
  }

  /**
   * NOTE: must be invoked on dispatch thread
   * Generates the android library project for GCM and copies java client libraries from the endpoints project
   *
   * @param project
   * @param appEngineModuleRootDir
   * @param androidModule
   * @param androidLibModuleRoot
   * @param rootPackage
   * @param appEngineModuleName
   * @param androidEndpointsLibModuleName
   * @param projectNumber
   */
  public static void generateAndroidGcmLib(final Project project,
                                           final VirtualFile appEngineModuleRootDir,
                                           final Module androidModule,
                                           final File androidLibModuleRoot,
                                           final String rootPackage,
                                           final String appEngineModuleName,
                                           final String androidEndpointsLibModuleName,
                                           final String projectNumber) {

    if (!androidLibModuleRoot.exists()) {
      androidLibModuleRoot.mkdir();
    }

    File jarPath = new File(PathUtil.getJarPathForClass(GradleGcmGeneratorHelper.class));
    if (jarPath.isFile()) {
      jarPath = jarPath.getParentFile();
    }

    File localTemplateDir = new File(jarPath, SDKTEMPLATES);
    File blankLibraryTemplateDir = new File(localTemplateDir, GCM_ACTIVITY);

    final Template t = Template.createFromPath(blankLibraryTemplateDir);

    // Now, expand the client libs, and generate thet set of replacement parameters we need
    MavenProject mavenProject = MavenProjectsManager.getInstance(project).findProject(appEngineModuleRootDir.findChild("pom.xml"));
    // TODO: This expands out the source directories. Should we be doing this on the dispatch thread?
    final Map<String, Object> templateReplacementMap = GradleAndroidTemplateHelper
      .buildReplacementMap(project, mavenProject, androidLibModuleRoot, rootPackage, projectNumber, t);
    if (templateReplacementMap == null) {
      Messages.showErrorDialog(project, "Unable to generate an App Engine Backend named '" +
                                        appEngineModuleName +
                                        "'.\n" +
                                        "Unable to find the source files for the generated client libraries under '" +
                                        appEngineModuleRootDir.getPath() +
                                        "/" +
                                        EndpointsConstants.APP_ENGINE_GENERATED_LIB_DIR +
                                        "' .", "Generate App Engine Backend");
      return;
    }

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        t.render(androidLibModuleRoot, androidLibModuleRoot, templateReplacementMap);
      }
    });

    // grab gcm.jar from Android SDK extras
    File gcmJar = new File(AndroidSdkUtils.tryToChooseAndroidSdk().getLocation() + EndpointsConstants.ANDROID_SDK_GCM_PATH);
    File androidLibModuleLibsFolder = new File(androidLibModuleRoot, SdkConstants.LIBS_FOLDER);
    if(!androidLibModuleLibsFolder.exists()) {
      androidLibModuleLibsFolder.mkdirs();
    }
    File targetGcmJar = new File(androidLibModuleLibsFolder, "gcm.jar");

    try {
      Files.copy(gcmJar, targetGcmJar);
    }
    catch (IOException e1) {
      LOG.error(e1);
      return;
    }

    final Module appEngineModule = ModuleManager.getInstance(project).findModuleByName(appEngineModuleName);

    // TODO: Do this async?
    File googleGeneratedDir =
      new File(VfsUtil.virtualToIoFile(appEngineModuleRootDir), EndpointsConstants.APP_ENGINE_GENERATED_LIB_DIR);
    LocalFileSystem.getInstance().refreshIoFiles(Collections.singletonList(googleGeneratedDir), true, true, new Runnable() {

      @Override
      public void run() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {

          @Override
          public void run() {
            copyEndpointsSourceOverToModule(project, appEngineModule, androidModule, androidLibModuleRoot, androidEndpointsLibModuleName);
          }
        });
      }
    });
  }

  private static void copyEndpointsSourceOverToModule(final Project project,
                                                      Module appEngineModule,
                                                      final Module androidModule,
                                                      File androidGcmLibModuleRoot,
                                                      final String androidEndpointsLibModuleName) {

    MavenEndpointGeneratorHelper mavenEndpointGeneratorHelper = new MavenEndpointGeneratorHelper(project, appEngineModule);
    mavenEndpointGeneratorHelper.copyEndpointSourceAndDepsToAndroidModule(androidGcmLibModuleRoot);

    // Update the android module's build.gradle file, we have to add MavenCentral for the library to build
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        VirtualFile buildGradlevFile = FacetUtils.findFileUnderContentRoots(androidModule, "build.gradle");
        if (buildGradlevFile != null) {
          GroovyFile buildGradleFile = (GroovyFile) PsiManager.getInstance(project).findFile(buildGradlevFile);
          GradleUtils.addRepository(project, buildGradleFile, GradleUtils.REPO_MAVEN_CENTRAL);
          GradleUtils.addDependency(project, buildGradleFile, String.format(GradleUtils.DEPENDENCY_COMPILE, androidEndpointsLibModuleName));
        }
      }
    });

    importGcmLib(project, androidModule);
  }

  private static void importGcmLib(final Project project, final Module androidModule) {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        try {
          // Blow away the project's build directory. This is a hack to work around a problem
          // with Gradle importing where the library is not actually built before the main
          // application module is imported.
          VirtualFile outputDir = FacetUtils.findFileUnderContentRoots(androidModule, "build");
          try {
            if (outputDir != null) {
              outputDir.delete(null);
            }
          }
          catch (IOException ioe) {
            LOG.error(ioe);
          }

          GradleProjectImporter.getInstance().reImportProject(project);
        }
        catch (ConfigurationException e) {
          LOG.error(e);
        }
      }
    });
  }
}
