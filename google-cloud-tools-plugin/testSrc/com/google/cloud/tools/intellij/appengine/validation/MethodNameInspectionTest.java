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
package com.google.cloud.tools.intellij.appengine.validation;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;

/**
 * Test for {@link MethodNameInspection}.
 */
public class MethodNameInspectionTest extends EndpointTestBase {

  /**
   *  Test to verify that a valid API method name does not generate
   *  a MethodNameInspection error.
   */
  public void testValidMethodName() {
    doTest();
  }

  /**
   *  Test to verify that a valid API method name with special characters
   *  generates a MethodNameInspection error.
   */
  public void testMethodNameWithSpecialCharacter() {
    doTest();
  }

  /**
   *  Test to verify that a valid API method name containing a dot does not
   *  generate a MethodNameInspection error.
   */
  public void testMethodNameContainingDot() {
    doTest();
  }

  /**
   * Tests that {@link MethodNameInspection.MyQuickFix} returns the same value when passed
   * in a valid API method name.
   */
  public void testQuickFix_validName() {
    MethodNameInspection.MyQuickFix myQuickFix = new MethodNameInspection().new MyQuickFix();
    assertEquals("foo.1_2_3", myQuickFix.getMethodNameSuggestions("foo.1_2_3"));
    assertEquals("foo.boo", myQuickFix.getMethodNameSuggestions("foo...boo"));
  }

  /**
   * Tests that {@link MethodNameInspection.MyQuickFix} returns the same value without
   * the starting and trailing dots when the API method name is a valid string with starting
   * and trailing dots.
   */
  public void testQuickFix_nameWithStartingTrailingDots() {
    MethodNameInspection.MyQuickFix myQuickFix = new MethodNameInspection().new MyQuickFix();
    assertEquals("foo", myQuickFix.getMethodNameSuggestions("..foo....."));
  }

  /**
   * Tests that {@link MethodNameInspection.MyQuickFix} provides the correct suggestion for an
   * API names with invalid characters.
   */
  public void testQuickFix_nameWithIllegalCharacter() {
    MethodNameInspection.MyQuickFix myQuickFix = new MethodNameInspection().new MyQuickFix();
    assertEquals("foo", myQuickFix.getMethodNameSuggestions("f*o&o."));
  }

  private void doTest() {
    LocalInspectionTool localInspectionTool = new MethodNameInspection();
    String testName = getTestName(true);
    final String testDataPath = getTestDataPath();
    myFixture.setTestDataPath(testDataPath);
    myFixture.testInspection("inspections/methodNameInspection/" + testName, new LocalInspectionToolWrapper(localInspectionTool));
  }
}
