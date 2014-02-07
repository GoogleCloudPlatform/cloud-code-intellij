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

package com.google.gct.intellij.endpoints.validation;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;

/**
 * Tests for {@link NamedResourceInspection}.
 */
public class NamedResourceInspectionTest extends EndpointTestBase {

  public void testMethodsWithNamedResources() {
    doTest();
  }

  public void testMethodsWithUnnamedResources() {
    doTest();
  }

  private void doTest() {
    LocalInspectionTool localInspectionTool = new NamedResourceInspection();
    String testName = getTestName(true);
    final String testDataPath = getTestDataPath();
    myFixture.setTestDataPath(testDataPath);
    myFixture.testInspection("inspections/namedResourceInspection/" + testName, new LocalInspectionToolWrapper(localInspectionTool));
  }
}