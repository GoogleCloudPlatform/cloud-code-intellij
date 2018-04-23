/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.java.startup;

import com.google.cloud.tools.intellij.appengine.java.sdk.CloudSdkVersionNotifier;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

/**
 * A StartupActivity that checks the configured Cloud SDK's version, and warns the user if the Cloud
 * SDK needs to be updated.
 */
public class CloudSdkVersionStartupCheck implements StartupActivity {

  @Override
  public void runActivity(@NotNull Project project) {
    // If there is a configured Cloud SDK at this time, check that it is supported.
    CloudSdkVersionNotifier.getInstance().notifyIfUnsupportedVersion();
  }
}
