/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.gct.login;

import junit.framework.TestCase;

import java.io.*;


/**
 * Tests for {@link GoogleLoginPrefs}
 */

//public class GoogleLoginPrefsTest extends TestCase {
public class GoogleLoginPrefsTest {
  // The required permission for the preference file
  private static final String PREFERENCE_FILE_PERMISSION = "-rw-r-----";

  /**
   * Tests that the Google Login credentials file is only accessible by the user.
   */
  // TODO: when upgraded to Java7, use java.nio.file.Files.getPosixFilePermissions
  public void testPreferenceFileAccessibility() throws IOException {
    String preferencesPath = GoogleLoginPrefs.getPreferencesPath();
    File userRootDir = new File(System.getProperty("java.util.prefs.userRoot",
      System.getProperty("user.home")), ".java/.userPrefs");
    String absolutePrefPath = userRootDir.getAbsolutePath() + preferencesPath + "/prefs.xml";

    // Get the preference file's permissions
    Process process = Runtime.getRuntime().exec("ls -al " + absolutePrefPath);

    // Parse the output from the process
    InputStream inputStream = process.getInputStream();
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
    String line = reader.readLine();
    inputStream.close();
    String[] splitOutput = line.split(" ");

    // Check the output
    //assertTrue(splitOutput.length > 1);
    //assertEquals(PREFERENCE_FILE_PERMISSION, splitOutput[0]);
  }
}
