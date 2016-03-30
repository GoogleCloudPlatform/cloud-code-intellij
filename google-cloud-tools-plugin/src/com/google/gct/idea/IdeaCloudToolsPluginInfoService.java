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
 * The singleton instance of this class provides plugin metadata for the Google Cloud Tools plugin.
 */
public class IdeaCloudToolsPluginInfoService extends BasePluginInfoService implements
    CloudToolsPluginInfoService {

  private final static String CLIENT_VERSION_PREFIX = "google.com/intellij/v";

  protected IdeaCloudToolsPluginInfoService() {
    super("gcloud-intellij-cloud-tools-plugin", "com.google.gct.core");
  }

  @Override
  public String getClientVersionForCloudDebugger() {
    return CLIENT_VERSION_PREFIX + getPluginVersion();
  }
}
