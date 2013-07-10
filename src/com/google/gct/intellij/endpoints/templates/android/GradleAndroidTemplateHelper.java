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
package com.google.gct.intellij.endpoints.templates.android;

import com.android.sdklib.BuildToolInfo;
import com.android.tools.idea.templates.Template;
import com.android.tools.idea.templates.TemplateMetadata;
import com.android.tools.idea.wizard.NewModuleWizardState;
import com.android.tools.idea.wizard.NewProjectWizardState;
import com.android.tools.idea.wizard.TemplateWizard;
import com.google.gct.intellij.endpoints.generator.MavenEndpointGeneratorHelper;
import com.google.gct.intellij.endpoints.templates.TemplateHelper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.containers.hash.HashMap;
import org.jetbrains.android.sdk.AndroidSdkUtils;
import org.jetbrains.idea.maven.project.MavenProject;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Functionality for creating the Android module for Endpoints from template files
 */
public class GradleAndroidTemplateHelper {
  // This matches the name of the API in MessageEndpoint.java.template
  private static final String MESSAGE_ENDPOINT_API_NAME = "messageEndpoint";

  private static final String TEMPLATE_DEVICE_INFO_ENDPOINT = "deviceInfoEndpointImport";
  private static final String TEMPLATE_DEVICE_INFO = "deviceInfoImport";
  private static final String TEMPLATE_MESSAGE_ENDPOINT = "messageEndpointImport";
  private static final String TEMPLATE_MESSAGE_DATA = "messageDataImport";
  private static final String TEMPLATE_COLLECTION_RESPONSE_MESSAGE_DATA = "collectionResponseMessageDataImport";
  private static final String TEMPLATE_PROJECT_NUMBER = "gcmProjectNumber";

  private static final Pattern PROGUARD_CONFIG_PATTERN = Pattern.compile("#??proguard\\.config=.*", Pattern.MULTILINE);

  // don't instantiate
  private GradleAndroidTemplateHelper() {
  }


  /**
   * create a map of strings to prime for replacement
   * @param p
   * @param appEngineMavenProject
   * @param androidLibRootDir
   * @param androidRootPackage
   * @param gcmProjectNumber
   * @param loadedTemplate
   * @return
   */
  public static Map<String, Object> buildReplacementMap(Project p, MavenProject appEngineMavenProject, File androidLibRootDir,
                                                        String androidRootPackage, String gcmProjectNumber, Template loadedTemplate) {

    Map<String, Object> nameToObjMap = new java.util.HashMap<String, Object>();

    Map<String, String> clientLibBasedReplacements =
        buildReplacementsBasedOnGeneratedEndpointLibs(appEngineMavenProject, androidRootPackage, gcmProjectNumber);

    if (clientLibBasedReplacements == null) {
      return null;
    }

    nameToObjMap.putAll(clientLibBasedReplacements);

    nameToObjMap.put(NewModuleWizardState.ATTR_PROJECT_LOCATION, p.getBasePath());

    // All of these hardcoded values can be implied from the master Android module
    //nameToObjMap.put(TemplateMetadata.ATTR_IS_NEW_PROJECT, false);
    nameToObjMap.put(TemplateMetadata.ATTR_IS_GRADLE, "true");
    nameToObjMap.put(TemplateMetadata.ATTR_CREATE_ICONS, false);
    nameToObjMap.put(TemplateMetadata.ATTR_IS_LIBRARY_MODULE, true);
    nameToObjMap.put(NewModuleWizardState.ATTR_CREATE_ACTIVITY, false);


    BuildToolInfo buildTool = AndroidSdkUtils.tryToChooseAndroidSdk().getLatestBuildTool();
    if (buildTool != null) {
      // If buildTool is null, the template will use buildApi instead, which might be good enough.
      nameToObjMap.put(TemplateMetadata.ATTR_BUILD_TOOLS_VERSION, buildTool.getRevision().toString());
    }

    nameToObjMap.put(TemplateMetadata.ATTR_PACKAGE_NAME, androidRootPackage);

    // Convert these values to ints
    convertToInt(nameToObjMap, loadedTemplate, TemplateMetadata.ATTR_MIN_API);
    convertToInt(nameToObjMap, loadedTemplate, TemplateMetadata.ATTR_MIN_API_LEVEL);
    convertToInt(nameToObjMap, loadedTemplate, TemplateMetadata.ATTR_BUILD_API);
    convertToInt(nameToObjMap, loadedTemplate, TemplateMetadata.ATTR_TARGET_API);

    File mainFlavorSourceRoot = new File(androidLibRootDir, TemplateWizard.MAIN_FLAVOR_SOURCE_PATH);
    File javaSourceRoot = new File(mainFlavorSourceRoot, TemplateWizard.JAVA_SOURCE_PATH);
    File javaSourcePackageRoot = new File(javaSourceRoot, androidRootPackage.replace('.', '/'));
    File resourceSourceRoot = new File(mainFlavorSourceRoot, TemplateWizard.RESOURCE_SOURCE_PATH);

    nameToObjMap.put(NewProjectWizardState.ATTR_MODULE_NAME, androidLibRootDir.getName());
    //nameToObjMap.put(TemplateMetadata.ATTR_PROJECT_OUT, moduleRoot.getPath());
    nameToObjMap.put(TemplateMetadata.ATTR_MANIFEST_OUT, mainFlavorSourceRoot.getPath());
    nameToObjMap.put(TemplateMetadata.ATTR_SRC_OUT, javaSourcePackageRoot.getPath());
    nameToObjMap.put(TemplateMetadata.ATTR_RES_OUT, resourceSourceRoot.getPath());
    nameToObjMap.put(TemplateMetadata.ATTR_TOP_OUT, p.getBasePath());


    String mavenUrl = System.getProperty("android.mavenRepoUrl");
    if (mavenUrl != null) {
      nameToObjMap.put(TemplateMetadata.ATTR_MAVEN_URL, mavenUrl);
    }


    return nameToObjMap;
  }


  private static Map<String, String> buildReplacementsBasedOnGeneratedEndpointLibs(MavenProject mavenProject,
                                                                                   String rootPackage,
                                                                                   String projectNumber) {

    Map<String, String> paramMap = new HashMap<String, String>();

    String deviceInfoApiName = TemplateHelper.getApiNameFromEntityName("DeviceInfo");
    File deviceinfoEndpointSrcFolder = MavenEndpointGeneratorHelper.expandSourceDirForApi(mavenProject, deviceInfoApiName);

    if (deviceinfoEndpointSrcFolder == null) {
      return null;
    }

    File messageEndpointsSrcFolder = MavenEndpointGeneratorHelper.expandSourceDirForApi(mavenProject, MESSAGE_ENDPOINT_API_NAME);

    if (deviceinfoEndpointSrcFolder == null) {
      return null;
    }

    paramMap.put(TEMPLATE_PROJECT_NUMBER, projectNumber);

    paramMap.put(TEMPLATE_DEVICE_INFO, getImportPathForClass(deviceinfoEndpointSrcFolder, "DeviceInfo.java"));
    paramMap.put(TEMPLATE_DEVICE_INFO_ENDPOINT, getImportPathForClass(deviceinfoEndpointSrcFolder, "Deviceinfoendpoint.java"));

    paramMap.put(TEMPLATE_MESSAGE_DATA, getImportPathForClass(messageEndpointsSrcFolder, "MessageData.java"));
    paramMap.put(TEMPLATE_MESSAGE_ENDPOINT, getImportPathForClass(messageEndpointsSrcFolder, "MessageEndpoint.java"));
    paramMap.put(TEMPLATE_COLLECTION_RESPONSE_MESSAGE_DATA,
                 getImportPathForClass(messageEndpointsSrcFolder, "CollectionResponseMessageData.java"));

    return paramMap;
  }

  private static String getImportPathForClass(File srcRootDir, String classFileName) {
    Pattern p = Pattern.compile(classFileName);

    List<File> matchingClassFiles = FileUtil.findFilesByMask(p, srcRootDir);

    if (matchingClassFiles.size() == 0) {
      return "com.unknown";
    }
    String importPath = FileUtil.getRelativePath(srcRootDir, matchingClassFiles.get(0)).replace(File.separatorChar, '.');

    if (importPath == null) {
      return "com.unknown";
    }

    int indexOfJavaSuffix = importPath.lastIndexOf(".java");
    if (indexOfJavaSuffix < 0) {
      return "com.unknown";
    }

    return importPath.substring(0, indexOfJavaSuffix);
  }

  private static void convertToInt(Map<String, Object> nameToObjMap, Template loadedTemplate, String attrName) {
    String strVal = loadedTemplate.getMetadata().getParameter(attrName).initial;
    if (strVal == null) {
      return;
    }

    try {
      int intVal = Integer.parseInt(strVal);
      nameToObjMap.put(attrName, intVal);
    }
    catch (NumberFormatException nfe) {
      nfe.printStackTrace();
    }
  }
}
