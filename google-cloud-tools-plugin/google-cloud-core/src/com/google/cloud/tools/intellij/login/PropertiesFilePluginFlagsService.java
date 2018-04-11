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

package com.google.cloud.tools.intellij.login;

import com.google.cloud.tools.intellij.flags.FlagReader;
import com.google.cloud.tools.intellij.flags.PropertiesFileFlagReader;
import com.intellij.openapi.diagnostic.Logger;

/**
 * This is a standard {@link PluginFlagsService} implementation that reads its values from the
 * default properties file.
 */
public class PropertiesFilePluginFlagsService implements PluginFlagsService {

  private static final Logger LOGGER = Logger.getInstance(PropertiesFilePluginFlagsService.class);
  private static final String USAGE_TRACKER_PROPERTY = "usage.tracker.property";
  private FlagReader flagReader;

  /** Initialize the property flag reader. */
  public PropertiesFilePluginFlagsService() {
    flagReader = new PropertiesFileFlagReader();
  }

  @Override
  public String getAnalyticsId() {
    return flagReader.getFlagString(USAGE_TRACKER_PROPERTY);
  }
}
