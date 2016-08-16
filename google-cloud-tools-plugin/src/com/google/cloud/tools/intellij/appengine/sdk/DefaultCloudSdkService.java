/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.sdk;

import com.google.cloud.tools.appengine.api.whitelist.AppEngineJreWhitelist;
import com.google.cloud.tools.intellij.appengine.jps.model.impl.JpsAppEngineModuleExtensionImpl;

import com.intellij.execution.configurations.ParametersList;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.JarUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.containers.ContainerUtil;

import gnu.trove.THashMap;
import gnu.trove.THashSet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;

/**
 * Default implementation of {@link CloudSdkService} backed by {@link PropertiesComponent} for
 * serialization.
 */
public class DefaultCloudSdkService extends CloudSdkService {

  private static final Logger logger = Logger.getInstance(DefaultCloudSdkService.class);

  private PropertiesComponent propertiesComponent;
  private static final String CLOUD_SDK_PROPERTY_KEY = "GCT_CLOUD_SDK_HOME_PATH";
  private static final String JAVA_TOOLS_BASE_PATH
      = "platform/google_appengine/google/appengine/tools/java/";
  private Map<String, Set<String>> myMethodsBlackList;

  public DefaultCloudSdkService() {
    this.propertiesComponent = PropertiesComponent.getInstance();
  }

  @Nullable
  @Override
  public String getCloudSdkHomePath() {
    return propertiesComponent.getValue(CLOUD_SDK_PROPERTY_KEY);
  }

  @Override
  public void setCloudSdkHomePath(String cloudSdkHomePath) {
    propertiesComponent.setValue(CLOUD_SDK_PROPERTY_KEY, cloudSdkHomePath);
  }

  @NotNull
  @Override
  public File getToolsApiJarFile() {
    return new File(JAVA_TOOLS_BASE_PATH,
        JpsAppEngineModuleExtensionImpl.LIB_APPENGINE_TOOLS_API_JAR);
  }

  @NotNull
  @Override
  public File[] getLibraries() {
    return getJarsFromDirectory(new File(JAVA_TOOLS_BASE_PATH, "lib/shared"));
  }

  @NotNull
  @Override
  public File[] getJspLibraries() {
    return getJarsFromDirectory(new File(JAVA_TOOLS_BASE_PATH, "lib/shared/jsp"));
  }

  @Override
  public boolean isMethodInBlacklist(@NotNull String className, @NotNull String methodName) {
    if (myMethodsBlackList == null) {
      try {
        myMethodsBlackList = loadBlackList();
      } catch (IOException ioe) {
        logger.error(ioe);
        myMethodsBlackList = new THashMap<>();
      }
    }
    final Set<String> methods = myMethodsBlackList.get(className);
    return methods != null && methods.contains(methodName);
  }

  @Override
  public boolean isClassInWhiteList(@NotNull String className) {
    return AppEngineJreWhitelist.contains(className);
  }

  @NotNull
  @Override
  public List<String> getUserLibraryPaths() {
    List<String> result = new ArrayList<>();
    result.add(getLibUserDirectoryPath());
    File opt = new File(getCloudSdkHomePath(), JAVA_TOOLS_BASE_PATH + "lib/opt/user");
    ContainerUtil.addIfNotNull(result, findLatestVersion(new File(opt, "appengine-endpoints")));
    ContainerUtil.addIfNotNull(result, findLatestVersion(new File(opt, "jsr107")));
    return result;
  }

  @NotNull
  @Override
  public String getOrmLibDirectoryPath() {
    return getLibUserDirectoryPath() + "/orm";
  }

  @NotNull
  @Override
  // TODO this path is incorrect. We need to determine an alternate strategy for loading these libs
  // maybe from maven central
  public VirtualFile[] getOrmLibSources() {
    final File libsDir = new File(JAVA_TOOLS_BASE_PATH, "src/orm");
    final File[] files = libsDir.listFiles();
    List<VirtualFile> roots = new ArrayList<>();
    if (files != null) {
      for (File file : files) {
        final String url = VfsUtil.getUrlForLibraryRoot(file);
        final VirtualFile zipRoot = VirtualFileManager.getInstance().findFileByUrl(url);
        if (zipRoot != null && zipRoot.isDirectory()) {
          String fileName = file.getName();
          final String srcDirName = StringUtil.trimEnd(fileName, "-src.zip");
          final VirtualFile sourcesDir = zipRoot.findFileByRelativePath(srcDirName + "/src/java");
          if (sourcesDir != null) {
            roots.add(sourcesDir);
          } else {
            roots.add(zipRoot);
          }
        }
      }
    }
    return VfsUtilCore.toVirtualFileArray(roots);
  }

  @NotNull
  @Override
  public File getApplicationSchemeFile() {
    return new File(getCloudSdkHomePath(), JAVA_TOOLS_BASE_PATH + "docs/appengine-application.xsd");
  }

  @NotNull
  @Override
  public File getWebSchemeFile() {
    return new File(getCloudSdkHomePath(), JAVA_TOOLS_BASE_PATH + "docs/appengine-web.xsd");
  }

  @Override
  public void patchJavaParametersForDevServer(@NotNull ParametersList vmParameters) {
    final String agentPath = JAVA_TOOLS_BASE_PATH + "/lib/agent/appengine-agent.jar";
    if (new File(FileUtil.toSystemDependentName(agentPath)).exists()) {
      vmParameters.add("-javaagent:" + agentPath);
    }
    String patchPath = JAVA_TOOLS_BASE_PATH + "/lib/override/appengine-dev-jdk-overrides.jar";
    if (new File(FileUtil.toSystemDependentName(patchPath)).exists()) {
      vmParameters.add("-Xbootclasspath/p:" + patchPath);
    }
  }

  @Override
  @Nullable
  public String getVersion() {
    return JarUtil.getJarAttribute(getToolsApiJarFile(), "com/google/appengine/tools/info/",
        Attributes.Name.SPECIFICATION_VERSION);
  }

  @Override
  public boolean isValid() {
    return getToolsApiJarFile().exists();
  }

  private static String findLatestVersion(File dir) {
    String[] names = dir.list();
    if (names != null && names.length > 0) {
      String max = Collections.max(Arrays.asList(names));
      return FileUtil.toSystemIndependentName(new File(dir, max).getAbsolutePath());
    }
    return null;
  }

  private Map<String, Set<String>> loadBlackList() throws IOException {
    final InputStream stream = getClass().getResourceAsStream("/data/methodsBlacklist.txt");
    logger.assertTrue(stream != null, "/data/methodsBlacklist.txt not found");
    final THashMap<String, Set<String>> map = new THashMap<>();
    BufferedReader reader
        = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
    try {
      String line;
      while ((line = reader.readLine()) != null) {
        final int i = line.indexOf(':');
        String className = line.substring(0, i);
        String methods = line.substring(i + 1);
        map.put(className, new THashSet<>(StringUtil.split(methods, ",")));
      }
    } finally {
      reader.close();
    }
    return map;
  }

  private String getLibUserDirectoryPath() {
    return getCloudSdkHomePath() + JAVA_TOOLS_BASE_PATH + "/lib/user";
  }

  private static File[] getJarsFromDirectory(File libFolder) {
    List<File> jars = new ArrayList<>();
    final File[] files = libFolder.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.isFile() && file.getName().endsWith(".jar")) {
          jars.add(file);
        }
      }
    }
    return jars.toArray(new File[jars.size()]);
  }
}
