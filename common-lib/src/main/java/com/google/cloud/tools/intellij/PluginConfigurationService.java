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

import com.intellij.openapi.extensions.ExtensionPointName;

/**
 * An IntelliJ Application Service for Google plugins to perform common plugin configurations, such
 * as enabling certain features or turning on/off error reporting.
 */
public interface PluginConfigurationService {

  /** Registers an extension using the IJ API. */
  <T> void registerExtension(ExtensionPointName<T> extensionPoint, T extension);

  /** Turn on Google Feedback Error reporting for the pluginId provided. */
  void enabledGoogleFeedbackErrorReporting(String pluginId);
}
