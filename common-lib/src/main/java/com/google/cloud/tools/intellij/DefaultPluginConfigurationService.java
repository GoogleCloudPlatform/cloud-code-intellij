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

import com.google.cloud.tools.intellij.feedback.GoogleFeedbackErrorReporter;
import com.intellij.ExtensionPoints;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.PluginId;
import org.jetbrains.annotations.NotNull;

/**
 * This will be the implementation you use for typical plugin deployments to the IntelliJ platform.
 */
public class DefaultPluginConfigurationService implements PluginConfigurationService {

  @Override
  public <T> void registerExtension(
      @NotNull ExtensionPointName<T> extensionPoint, @NotNull T extension) {
    Extensions.getRootArea().getExtensionPoint(extensionPoint).registerExtension(extension);
  }

  @Override
  public void enabledGoogleFeedbackErrorReporting(@NotNull String pluginId) {
    GoogleFeedbackErrorReporter errorReporter = new GoogleFeedbackErrorReporter();
    IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId(pluginId));
    if (plugin == null) {
      throw new IllegalArgumentException(pluginId + " is not a valid plugin ID.");
    }
    errorReporter.setPluginDescriptor(plugin);
    Extensions.getRootArea()
        .getExtensionPoint(ExtensionPoints.ERROR_HANDLER_EP)
        .registerExtension(errorReporter);
  }
}
