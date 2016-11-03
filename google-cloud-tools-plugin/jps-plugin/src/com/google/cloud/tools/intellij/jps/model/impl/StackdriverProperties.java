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

package com.google.cloud.tools.intellij.jps.model.impl;

/**
 * Serializable Stackdriver configuration.
 */
public class StackdriverProperties {
  private boolean generateSourceContext = true;
  private boolean ignoreErrors = true;
  // Needs to be serialized here so the build plugin can use the Cloud SDK.
  private String sdkPath;

  public StackdriverProperties() {}

  public StackdriverProperties(String sdkPath) {
    this.sdkPath = sdkPath;
  }

  public boolean isGenerateSourceContext() {
    return generateSourceContext;
  }

  public void setGenerateSourceContext(boolean generateSourceContext) {
    this.generateSourceContext = generateSourceContext;
  }

  public void setIgnoreErrors(boolean ignoreErrors) {
    this.ignoreErrors = ignoreErrors;
  }

  public boolean isIgnoreErrors() {
    return ignoreErrors;
  }

  public void setCloudSdkPath(String sdkPath) {
    this.sdkPath = sdkPath;
  }

  public String getCloudSdkPath() {
    return sdkPath;
  }
}
