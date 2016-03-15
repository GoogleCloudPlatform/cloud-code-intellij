package com.google.gct.idea.flags;

import com.google.common.annotations.VisibleForTesting;

import com.intellij.openapi.diagnostic.Logger;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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
      in = getClass().getClassLoader()
          .getResourceAsStream(propertiesFilePath);
      if (in == null) {
        throw new IllegalArgumentException("Failed to find the plugins property configuration file"
            + " which was configured as " + propertiesFilePath);
      }
      properties.load(in);
    } catch (IOException e) {
      throw new RuntimeException("Error reading the properties file: " + propertiesFilePath, e);
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
          LOGGER.error(e);
        }
      }
    }
  }

  @Nullable
  public String getFlagString(String propertyName) {
    return properties.getProperty(propertyName);
  }
}
