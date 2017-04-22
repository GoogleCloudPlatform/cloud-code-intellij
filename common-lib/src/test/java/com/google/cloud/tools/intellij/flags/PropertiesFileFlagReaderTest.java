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

import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Test;

/** Test cases for {@link PropertiesFileFlagReader}. */
public class PropertiesFileFlagReaderTest {

  @Test
  public void testGetFlagString_flatExists() {
    PropertiesFileFlagReader flagReader = getValidPropertiesFlagReader();
    Assert.assertEquals("testing123", flagReader.getFlagString("test.flag"));
  }

  @Test
  public void testGetFlagString_flagDoesNotExist() {
    PropertiesFileFlagReader flagReader = getValidPropertiesFlagReader();
    Assert.assertNull(flagReader.getFlagString("non.existent.flag"));
  }

  @Test
  public void testGetFlagString_emptyFlagIsEmptyString() {
    PropertiesFileFlagReader flagReader = getValidPropertiesFlagReader();
    Assert.assertEquals("", flagReader.getFlagString("test.empty.flag"));
  }

  @Test
  public void testInvalidConfigFilePath() {
    try {
      new PropertiesFileFlagReader("idontexist.properties");
    } catch (IllegalArgumentException e) {
      return;
    }
    fail();
  }

  private PropertiesFileFlagReader getValidPropertiesFlagReader() {
    return new PropertiesFileFlagReader(
        "com/google/cloud/tools/intellij/flags/flags_test.properties");
  }
}
