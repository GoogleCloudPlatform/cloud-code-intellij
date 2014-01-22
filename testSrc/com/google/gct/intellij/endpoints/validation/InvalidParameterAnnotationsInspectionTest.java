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
package com.google.gct.intellij.endpoints.validation;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;

/**
 * Tests for {@link InvalidParameterAnnotationsInspection}
 */
public class InvalidParameterAnnotationsInspectionTest extends EndpointTestBase {
  /**
   * Tests that when a name is not specified in the path attribute of an @ApiMethod,
   * no InvalidParameterAnnotationsInspection error produced.
   */
  public void testPathAttributeWithNoNameDeclared() {
    doTest();
  }

  /**
   * Tests that when a variable name is specified in the path attribute of an @ApiMethod,
   * and the corresponding method parameter has @DefaultValue, an InvalidParameterAnnotationsInspection
   * error is produced.
   */
  public void testPathNameWithDefaultValueAnnotation() {
    doTest();
  }

  /**
   * Tests that when a variable name is specified in the path attribute of an @ApiMethod,
   * and the corresponding method parameter has @Nullable, an InvalidParameterAnnotationsInspection
   * error is produced.
   */
  public void testPathNameWithNullableAnnotation() {
    doTest();
  }

  /**
   * Tests that when a variable name is specified in the path attribute of an @ApiMethod,
   * and the corresponding method parameter does not have @Nullable or @DefaultValue,
   * no InvalidParameterAnnotationsInspection error is produced.
   */
  public void testPathNameWithNoNullableOrDefaultValue() {
    doTest();
  }

  private void doTest() {
    LocalInspectionTool localInspectionTool = new InvalidParameterAnnotationsInspection();
    String testName = getTestName(true);
    myFixture.setTestDataPath(getTestDataPath());
    myFixture.testInspection("inspections/invalidParameterAnnotationsInspection/" + testName, new LocalInspectionToolWrapper(localInspectionTool));
  }
}
