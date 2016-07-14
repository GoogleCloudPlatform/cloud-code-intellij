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

package com.intellij.appengine.server.integration;

import com.google.cloud.tools.intellij.appengine.sdk.AppEngineSdk;

import com.intellij.javaee.appServerIntegrations.ApplicationServerHelper;
import com.intellij.javaee.appServerIntegrations.ApplicationServerInfo;
import com.intellij.javaee.appServerIntegrations.ApplicationServerPersistentData;
import com.intellij.javaee.appServerIntegrations.ApplicationServerPersistentDataEditor;
import com.intellij.javaee.appServerIntegrations.CantFindApplicationServerJarsException;

/**
 * @author nik
 */
public class AppEngineServerHelper implements ApplicationServerHelper {

  public ApplicationServerInfo getApplicationServerInfo(
      ApplicationServerPersistentData persistentData)
      throws CantFindApplicationServerJarsException {
    final AppEngineSdk sdk = ((AppEngineServerData) persistentData).getSdk();
    String version = sdk.getVersion();
    return new ApplicationServerInfo(sdk.getLibraries(),
        "AppEngine Dev" + (version != null ? " " + version : ""));
  }

  public ApplicationServerPersistentData createPersistentDataEmptyInstance() {
    return new AppEngineServerData("");
  }

  public ApplicationServerPersistentDataEditor createConfigurable() {
    return new AppEngineServerEditor();
  }
}
