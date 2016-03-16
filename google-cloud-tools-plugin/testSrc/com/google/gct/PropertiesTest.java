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

package com.google.gct;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class PropertiesTest {

  @Test
  public void testSpace() throws IOException {
      File f = new File("resources/messages/CloudToolsBundle.properties");
      InputStream in = null;
      try {
          in = new FileInputStream(f);
          Properties p = new Properties();
          p.load(new InputStreamReader(in, "UTF-8"));
          Assert.assertEquals("Do you want to checkout branch {0} and restore the saved stash?",
                  p.getProperty("clouddebug.restorestash"));
      } finally {
          if (in != null) {
              in.close();
          }
      }
  }
}
