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

package com.google.cloud.tools.intellij.appengine.cloud;

import org.jetbrains.annotations.Nullable;

public enum AppEngineStandardRuntime {

  JAVA_8("java8");

  private String label;

  AppEngineStandardRuntime(String label) {
    this.label = label;
  }

  public boolean isJava8() {
    return this == JAVA_8;
  }

  public String getLabel() {
    return this.label;
  }

  @Nullable
  public static AppEngineStandardRuntime fromLabel(String label) {
    switch(label) {
      case "java8":
        return AppEngineStandardRuntime.JAVA_8;
      default:
        return null;
    }
  }

}
