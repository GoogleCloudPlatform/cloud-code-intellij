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
   * Tests that a ConstructorInspection error is generated for a class with only a constructor
   * with arguments.
   */
  public void  testClassWithConstructorWithArgument() {
    doTest();
  }

  /**
   * Tests that a ConstructorInspection error is not generated for a class with
   * with no user provided constructor.
   */
  public void testClassWithNoConstructor() {
    doTest();
  }

  /**
   * Tests that a ConstructorInspection error is generated for a class with only a
   * private constructor.
   */
  public void testClassWithPrivateConstructor() {
    doTest();
  }

  /**
   * Tests that a ConstructorInspection error is not generated for a class with only a constructor
   * without arguments.
   */
  public void  testClassWithPublicNullaryConstructor() {
    doTest();
  }

  /**
   *  Tests that a ConstructorInspection error is not generated for a class with multiple
   *  constructors including a public nullary constructor.
   */
  public void testMultipleConstructorsIncludingPublicNullary() {
    doTest();
  }

  /**
   *  Tests that a ConstructorInspection error is generated for a class with multiple
   *  constructors that does not include a public nullary constructor.
   */
  public void testMultipleConstructorsWithoutPublicNullary() {
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
