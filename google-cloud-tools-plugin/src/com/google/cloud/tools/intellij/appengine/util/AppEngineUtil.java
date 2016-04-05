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

package com.google.cloud.tools.intellij.appengine.util;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * App Engine utility methods
 */
public class AppEngineUtil {

  private AppEngineUtil() {
    // Not designed for instantiation
  }

  /**
   * Generates a version string in the gcloud version format:
   *
   * [year][month][day]t[hours][min][sec]
   *
   * For example: 20160331t132711
   *
   * @return generated version
   */
  public static String generateVersion() {
    String versionPattern = "yyyyMMdd't'kms";
    DateTime dateTime = new DateTime();
    DateTimeFormatter fmt = DateTimeFormat.forPattern(versionPattern);

    return fmt.print(dateTime);
  }

}
