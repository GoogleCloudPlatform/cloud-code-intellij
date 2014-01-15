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
 * Tests for {@link ApiNamespaceInspection}
 */
public class ApiNamespaceInspectionTest extends EndpointTestBase  {

  public void testApiNamespaceAttribute_complete() {
    doTest();
  }

  public void testApiNamespaceAttribute_withoutPackagePath() {
    doTest();
  }

  public void testApiNamespaceAttribute_withoutOwnerName() {
    doTest();
  }

  public void testApiNamespaceAttribute_withOnlyPackagePath() {
    doTest();
  }

  public void testApiNamespaceAttribute_withOnlyOwnerDomain() {
    doTest();
  }

  private void doTest() {
    LocalInspectionTool localInspectionTool = new ApiNamespaceInspection();
    String testName = getTestName(true);
    final String testDataPath = getTestDataPath();
    myFixture.setTestDataPath(testDataPath);
    myFixture.testInspection("inspections/apiNamespaceInspectionTest/" + testName, new LocalInspectionToolWrapper(localInspectionTool));
  }
}
