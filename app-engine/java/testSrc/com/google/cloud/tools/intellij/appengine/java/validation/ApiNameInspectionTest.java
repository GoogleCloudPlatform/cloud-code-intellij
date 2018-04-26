/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.java.validation;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;

/** Tests for {@link ApiNameInspection}. */
public class ApiNameInspectionTest extends EndpointTestBase {

  public void testGetGroupDisplayName() {
    LocalInspectionTool localInspectionTool = new ApiNameInspection();
    assertEquals("Google Cloud Platform", localInspectionTool.getGroupDisplayName());
  }

  /**
   * Test to verify that when the API name attribute is an empty string, an ApiNameInspection error
   * is not generated.
   */
  public void testEmptyApiNameAttribute() {
    doTest();
  }

  /**
   * Test to verify that when the API name attribute is not specified, an ApiNameInspection error is
   * not generated.
   */
  public void testNoApiNameAttribute() {
    doTest();
  }

  /**
   * Test to verify that an API name that matches "^[a-z]+[A-Za-z0-9]*$" pattern does not generate
   * an ApiNameInspection error.
   */
  public void testValidApiNameAttribute() {
    doTest();
  }

  /**
   * Test to verify that an API name beginning with a digit causes an ApiNameInspection error to be
   * generated.
   */
  public void testApiNameAttribute_startWithNumber() {
    doTest();
  }

  /**
   * Test to verify that an Api name with a special character causes an ApiNameInspection error to
   * be generated.
   */
  public void testApiNameAttribute_withSpecialCharacter() {
    doTest();
  }

  /**
   * Tests that {@link ApiNameInspection.MyQuickFix} returns the same value when passed in a valid
   * API name.
   */
  public void testQuickFix_withValidApiName() {
    ApiNameInspection.MyQuickFix localQuickFix = new ApiNameInspection().new MyQuickFix();
    assertEquals("foo", localQuickFix.getNameSuggestions("foo"));
  }

  /**
   * Tests that {@link ApiNameInspection.MyQuickFix} provides the correct suggestion for an API
   * names with invalid characters.
   */
  public void testQuickFix_withInvalidCharacters() {
    ApiNameInspection.MyQuickFix localQuickFix = new ApiNameInspection().new MyQuickFix();
    assertEquals("invalidcharacters", localQuickFix.getNameSuggestions("@invalid&characters#"));
    assertEquals("invalidCharacters", localQuickFix.getNameSuggestions("@Invalid&()Characters#"));
    assertEquals("invalidCharacters", localQuickFix.getNameSuggestions("@23Inval&*idChara(cters#"));
  }
  /**
   * Tests that {@link ApiNameInspection.MyQuickFix} provides the correct suggestion for an API
   * names beginning with digits.
   */
  public void testQuickFix_withStartingDigits() {
    ApiNameInspection.MyQuickFix localQuickFix = new ApiNameInspection().new MyQuickFix();
    assertEquals("digit", localQuickFix.getNameSuggestions("1digit"));
    assertEquals("digit", localQuickFix.getNameSuggestions("123digit"));
    assertEquals("api12345", localQuickFix.getNameSuggestions("12345"));
  }

  /**
   * Tests that {@link ApiNameInspection.MyQuickFix} provides the correct suggestion for API names
   * beginning with uppercase letters.
   */
  public void testQuickFix_withUppercaseLetters() {
    ApiNameInspection.MyQuickFix localQuickFix = new ApiNameInspection().new MyQuickFix();
    assertEquals("foo", localQuickFix.getNameSuggestions("Foo"));
    assertEquals("fOO", localQuickFix.getNameSuggestions("FOO"));
  }

  private void doTest() {
    LocalInspectionTool localInspectionTool = new ApiNameInspection();
    String testName = getTestName(true);
    final String testDataPath = getTestDataPath();
    myFixture.setTestDataPath(testDataPath);
    LocalInspectionToolWrapper toolWrapper = new LocalInspectionToolWrapper(localInspectionTool);
    myFixture.testInspection("inspections/apiNameInspection/" + testName, toolWrapper);
  }
}
