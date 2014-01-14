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
 * Tests for {@link ConstructorInspection}
 */
public class ConstructorInspectionTest extends EndpointTestBase {
  /**
   * Tests that a ConstructorInspection error is not generated for a nullary constructor.
   */
  public void testNullaryConstructor() {
    doTest();
  }

  /**
   * Tests that a ConstructorInspection error is generated for a private constructor.
   */
  public void testPrivateConstructor() {
    doTest();
  }

  /**
   * Tests that a ConstructorInspection error is generated for a constructor with arguments.
   */
  public void  testConstructorWithArgument() {
    doTest();
  }

  private void doTest() {
    LocalInspectionTool localInspectionTool = new ConstructorInspection();
    String testName = getTestName(true);
    final String testDataPath = getTestDataPath();
    myFixture.setTestDataPath(testDataPath);
    myFixture.testInspection("inspections/constructorInspectionTest/" + testName, new LocalInspectionToolWrapper(localInspectionTool));
  }
}
