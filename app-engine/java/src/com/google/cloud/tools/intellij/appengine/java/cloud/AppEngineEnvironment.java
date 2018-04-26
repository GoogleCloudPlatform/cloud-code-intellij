/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.cloud.tools.intellij.appengine.java.cloud;

import com.google.cloud.tools.intellij.appengine.java.AppEngineMessageBundle;

/**
 * Specifies an App Engine environment.
 *
 * <p>The App Engine service only contemplates two environments: Standard and Flexible. We're
 * considering a third one here, flexible compatible, which represents a project whose source layout
 * is Standard (e.g., exploded war), but is finally deployed to the Flexible environment in a Docker
 * container.
 *
 * <p>This distinction is useful, e.g., to select the deploy runner for a compat project (i.e., the
 * standard runner) and to tell a user which environment the app is being deployed to (i.e.,
 * flexible environment).
 */
public enum AppEngineEnvironment {
  APP_ENGINE_STANDARD("appengine.environment.name.standard"),
  APP_ENGINE_FLEX("appengine.environment.name.flexible"),
  APP_ENGINE_FLEX_COMPAT("appengine.environment.name.flexible");

  private final String label;

  AppEngineEnvironment(String label) {
    this.label = label;
  }

  public boolean isStandard() {
    return this == APP_ENGINE_STANDARD;
  }

  public boolean isFlexible() {
    return this == APP_ENGINE_FLEX;
  }

  public boolean isFlexCompat() {
    return this == APP_ENGINE_FLEX_COMPAT;
  }

  public String localizedLabel() {
    return AppEngineMessageBundle.message(label);
  }
}
