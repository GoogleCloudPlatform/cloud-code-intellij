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

package com.google.cloud.tools.intellij.flags;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.diagnostic.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.jetbrains.annotations.Nullable;

/**
 * Use an instance of this class to read flags provided to your plugin through a {@code
 * config.properties} file placed in your plugin's resource root directory.
 */
public class PropertiesFileFlagReader implements FlagReader {
  private static final Logger LOGGER = Logger.getInstance(PropertiesFileFlagReader.class);
  private static final String DEFAULT_PROPERTIES_FILE_PATH = "config.properties";
  private final Properties properties;

  public PropertiesFileFlagReader() {
    this(DEFAULT_PROPERTIES_FILE_PATH);
  }

  @VisibleForTesting
  protected PropertiesFileFlagReader(String propertiesFilePath) {
    properties = new Properties();
    InputStream in = null;
    try {
      in = getClass().getClassLoader().getResourceAsStream(propertiesFilePath);
      if (in == null) {
        throw new IllegalArgumentException(
            "Failed to find the plugins property configuration file"
                + " which was configured as "
                + propertiesFilePath);
      }
      properties.load(in);
    } catch (IOException ioe) {
      throw new RuntimeException("Error reading the properties file: " + propertiesFilePath, ioe);
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException ioe) {
          LOGGER.error(ioe);
        }
      }
    }
  }

  @Nullable
  @Override
  public String getFlagString(String propertyName) {
    return properties.getProperty(propertyName);
  }
}
