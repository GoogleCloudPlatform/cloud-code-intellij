/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.google.gct.intellij.endpoints.util;

/**
 * Utility methods for endpoint validation checks.
 */
public class EndpointUtilities {
  /**
   * Removes the beginning and ending quotes on a string. If the input string
   * does not have quotes at the beginning and at the end, the input string is returned.
   * @param input String to be parsed
   * @return input string with quotes at the beginning and end removed if they both exist.
   */
  public static String removeBeginningAndEndingQuotes(String input) {
    if(input == null || !input.startsWith("\"") || !input.endsWith("\"")) {
      return input;
    }

    return input.substring(1,input.length() - 1);
  }
}
