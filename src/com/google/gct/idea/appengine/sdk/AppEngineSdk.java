/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.gct.idea.appengine.sdk;

import com.intellij.execution.configurations.ParametersList;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Container for AppEngine SDK, for validation and other utilities
 */
public class AppEngineSdk {

  public static final Logger LOG = Logger.getInstance(AppEngineSdk.class);

  private static final String TOOLS_API_JAR_PATH = "/lib/appengine-tools-api.jar";
  private static final String AGENT_JAR_PATH = "/lib/agent/appengine-agent.jar";
  private static final String OVERRIDES_JAR_PATH = "/lib/override/appengine-dev-jdk-overrides.jar";
  public static final String DEV_APPSERVER_CLASS = "com.google.appengine.tools.development.DevAppServerMain";

  private final String mySdkPath;


  public AppEngineSdk(String sdkPath) {
    mySdkPath = sdkPath;
  }

  /**
   * Check if the sdk has the requirements to run the appengine dev app server
   */
  public boolean canRunDevAppServer() {
    return getToolsApiJarFile() != null;
  }

  /**
   * Find a jar in an SDK based on a path
   * @param jarPath
   * @return File if it exists, null otherwise
   */
  @Nullable
  public File getSdkJar(String jarPath) {
    final String fullJarPath = mySdkPath + jarPath;
    File sdkJar = new File(FileUtil.toSystemDependentName(fullJarPath));
    if (sdkJar.exists()) {
      return sdkJar;
    }
    return null;
  }

  /** Get the App Engine tools api jar */
  @Nullable
  public File getToolsApiJarFile() {
    return getSdkJar(TOOLS_API_JAR_PATH);
  }

  /** When running dev app server and not using Kick Start, use these params to run DevAppServerMain directly */
  public void addServerVmParams(ParametersList vmParams) {
    File agentJar = getSdkJar(AGENT_JAR_PATH);
    if (agentJar != null) {
      vmParams.add("-javaagent:" + agentJar.getAbsolutePath());
    }
    else {
        LOG.warn("App Engine SDK Agent jar not found : " + AGENT_JAR_PATH);
    }

    File overridesJar = getSdkJar(OVERRIDES_JAR_PATH);
    if (overridesJar != null) {
      vmParams.add("-Xbootclasspath/p:" + overridesJar.getAbsolutePath());
    }
    else {
      LOG.warn("App Engine SDK Overrides JAR not found " + OVERRIDES_JAR_PATH);
    }
  }
}