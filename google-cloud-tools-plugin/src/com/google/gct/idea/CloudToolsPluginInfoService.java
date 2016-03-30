/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.google.gct.idea;

/**
 * Cloud Tools specific {@link PluginInfoService}. Exists to ensure unique bindings of this pugin's
 * information service across google-cloud-tools-plugin and google-account-plugin
 */
public interface CloudToolsPluginInfoService extends PluginInfoService {

  /**
   * Returns the fully qualified version of the plugin for specifying in Cloud Debugger API
   * requests.
   *
   * @return "google.com/intellij/v{currentPluginVersion}"
   */
  String getClientVersionForCloudDebugger();
}
