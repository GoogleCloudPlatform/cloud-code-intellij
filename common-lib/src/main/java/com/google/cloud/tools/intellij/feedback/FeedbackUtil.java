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

package com.google.cloud.tools.intellij.feedback;

import com.intellij.ExtensionPoints;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.PluginId;

/** Handy plugin related methods for Google Feedback integration. */
public final class FeedbackUtil {

  private FeedbackUtil() {}

  /** Registers an error reporter extension point for the given plugin. */
  public static void enableGoogleFeedbackErrorReporting(String pluginId) {
    GoogleFeedbackErrorReporter errorReporter = new GoogleFeedbackErrorReporter();
    errorReporter.setPluginDescriptor(PluginManager.getPlugin(PluginId.getId(pluginId)));
    Extensions.getRootArea()
        .getExtensionPoint(ExtensionPoints.ERROR_HANDLER_EP)
        .registerExtension(errorReporter);
  }
}
