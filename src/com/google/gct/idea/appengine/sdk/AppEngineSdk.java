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

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Container for AppEngine SDK, for validation and other utilities
 */
public class AppEngineSdk {

  private static final String TOOLS_API_JAR_PATH = "/lib/appengine-tools-api.jar";
  public static final String KICK_STARTER_CLASS = "com.google.appengine.tools.KickStart";
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

  @Nullable
  public File getToolsApiJarFile() {
    final String path = FileUtil.toSystemDependentName(mySdkPath + TOOLS_API_JAR_PATH);
    File toolsJar = new File(path);
    if(toolsJar.exists()) {
      return toolsJar;
    }
    return null;
  }
}