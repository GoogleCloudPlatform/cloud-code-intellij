/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import com.google.common.base.Optional;
import com.intellij.ide.plugins.IdeaPluginDescriptor;

/** Provides information about the current run of the application. */
public interface ApplicationPluginInfoService {

  /** Finds a plugin by its plugin ID. */
  Optional<IdeaPluginDescriptor> findPlugin(String pluginId);

  /** Returns true if a plugin is installed, and currently active. */
  boolean isPluginActive(String pluginId);
}
