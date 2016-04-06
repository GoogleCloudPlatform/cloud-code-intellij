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

package com.google.cloud.tools.intellij;

/**
 * The singleton instance of this class provides plugin metadata for the Google Account plugin.
 */
public class IdeaAccountPluginInfoService extends BasePluginInfoService implements AccountPluginInfoService {

  protected IdeaAccountPluginInfoService() {
    super("gcloud-intellij-account-plugin", "com.google.gct.login");
  }
}
