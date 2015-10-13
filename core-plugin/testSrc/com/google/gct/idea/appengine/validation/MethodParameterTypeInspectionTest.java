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
package com.google.gct.idea.appengine.validation;


import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;

/**
 * Unit test for {@link MethodParameterTypeInspection}.
 */
public class MethodParameterTypeInspectionTest extends EndpointTestBase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    myFixture.addClass("package java.util; public interface Collection {}");
    myFixture.addClass("package java.util; public interface List extends Collection {}");
  }

  /**
   * Tests that method parameters of arrays of arrays cause MethodParameterTypeInspection errors.
   */
  public void testMultipleParameterTypesTwoDArray() {
    doTest();
  }

  public void testMultipleParameterTypesArrayOfLists() {
    doTest();
  }

  // todo(elharo): seems non-functioning
  public void testMultipleParameterTypesListOfArrays() {
    doTest();
  }

  // todo(elharo): seems non-functioning
  public void testMultipleParameterTypesListOfLists() {
    doTest();
  }

  private void doTest() {
    LocalInspectionTool localInspectionTool = new MethodParameterTypeInspection();
    String testName = getTestName(true);
    myFixture.setTestDataPath(getTestDataPath());
    myFixture.testInspection("inspections/methodParameterTypeInspection/" + testName, new LocalInspectionToolWrapper(localInspectionTool));
  }
}
