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

package com.google.cloud.tools.intellij.util;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;

/**
 * Helper methods for dealing with installed plugins.
 */
public class Plugins {

  public boolean isPluginInstalled(String pluginId) {
    IdeaPluginDescriptor pluginDescriptor = getPluginById(pluginId);
    return pluginDescriptor != null && pluginDescriptor.isEnabled();
  }

  public IdeaPluginDescriptor getPluginById(String pluginId) {
    return PluginManager.getPlugin(PluginId.findId(pluginId));
  }
}
