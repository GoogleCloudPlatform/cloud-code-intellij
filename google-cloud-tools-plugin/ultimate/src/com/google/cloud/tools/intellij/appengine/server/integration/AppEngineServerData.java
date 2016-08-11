/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.intellij.appengine.server.integration;

import com.google.cloud.tools.intellij.appengine.sdk.AppEngineSdk;
import com.google.cloud.tools.intellij.appengine.sdk.AppEngineSdkManager;

import com.intellij.javaee.appServerIntegrations.ApplicationServerPersistentData;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * @author nik
 */
public class AppEngineServerData implements ApplicationServerPersistentData {
  private String mySdkPath;

  public AppEngineServerData(@NotNull String sdkPath) {
    mySdkPath = sdkPath;
  }

  @NotNull 
  public String getSdkPath() {
    return mySdkPath;
  }

  @NotNull
  public AppEngineSdk getSdk() {
    return AppEngineSdkManager.getInstance().findSdk(mySdkPath);
  }

  public void setSdkPath(@NotNull String sdkPath) {
    mySdkPath = sdkPath;
  }

  public void readExternal(Element element) throws InvalidDataException {
    mySdkPath = element.getChildTextTrim("sdk-path");
  }

  public void writeExternal(Element element) throws WriteExternalException {
    element.addContent(new Element("sdk-path").addContent(mySdkPath));
  }
}
