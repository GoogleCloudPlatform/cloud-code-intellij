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

/** Tests for {@link ResourceParameterInspection}. */
public class ResourceParameterInspectionTest extends EndpointTestBase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    myFixture.addClass("package java.util; public interface Collection {}");
    myFixture.addClass("package java.util; public interface List extends Collection {}");
  }

  /**
   * Tests that ResourceParameterInspection error is not generated for a method parameter of
   * parameter type.
   */
  public void testNonResourceParameter() {
    doTest();
  }

  /**
   * Tests that a ResourceParameterInspection error is generated for a resource parameter with
   * the @Named.
   */
  public void testResourceParameterWithNamedAnnotation() {
    doTest();
  }

  /**
   * Tests that a ResourceParameterInspection error is generated for a resource parameter of array
   * type.
   */
  public void testResourceParameterOfArrayType() {
    doTest();
  }

  /**
   * Tests that a ResourceParameterInspection error is generated for a resource parameter of List
   * type.
   */
  public void testResourceParameterOfListType() {
    doTest();
  }

  /**
   * Tests that a ResourceParameterInspection error is not generated for a resource parameter that
   * is not of array or collection type and does not have @Named.
   */
  public void testValidResourceParameterConfiguration() {
    doTest();
  }

  /**
   * Tests that a ResourceParameterInspection error is generated for a method that has multiple
   * resource parameters.
   */
  public void testMultipleResourceParameters() {
    doTest();
  }
  /**
   * Tests that a ResourceParameterInspection error is not generated for constructor with a resource
   * parameter.
   */
  public void testConstructor() {
    doTest();
  }

  private void doTest() {
    LocalInspectionTool localInspectionTool = new ResourceParameterInspection();
    String testName = getTestName(true);
    final String testDataPath = getTestDataPath();
    myFixture.setTestDataPath(testDataPath);
    LocalInspectionToolWrapper toolWrapper = new LocalInspectionToolWrapper(localInspectionTool);
    myFixture.testInspection("inspections/resourceParameterInspection/" + testName, toolWrapper);
  }
}
