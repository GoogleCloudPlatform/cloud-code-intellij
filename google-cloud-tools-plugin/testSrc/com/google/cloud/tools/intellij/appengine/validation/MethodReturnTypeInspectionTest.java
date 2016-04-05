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
 * Tests for {@link MethodReturnTypeInspection},
 */
public class MethodReturnTypeInspectionTest extends EndpointTestBase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    myFixture.addClass("package java.util; public interface Collection {}");
    myFixture.addClass("package java.util; public interface Set extends Collection {}");
  }

  public void testMultipleReturnTypes() {
    doTest();
  }

  public void testSetReturnType() {
    doTest();
  }

  private void doTest() {
    LocalInspectionTool localInspectionTool = new MethodReturnTypeInspection();
    String testName = getTestName(true);
    myFixture.setTestDataPath(getTestDataPath());
    LocalInspectionToolWrapper toolWrapper = new LocalInspectionToolWrapper(localInspectionTool);
    myFixture.testInspection("inspections/methodReturnTypeInspection/" + testName, toolWrapper);
  }
}
