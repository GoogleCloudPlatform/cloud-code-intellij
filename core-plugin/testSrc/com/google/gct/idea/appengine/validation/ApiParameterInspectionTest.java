/*
 * Copyright (C) 2015 The Android Open Source Project
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

import com.google.gct.idea.appengine.GctConstants;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiParameter;
import com.intellij.testFramework.MockProblemDescriptor;
import junit.framework.Assert;

/**
 * Test for {@link ApiParameterInspection}.
 */
public class ApiParameterInspectionTest extends EndpointTestBase {

  public void ignore_testMethodsWithNamedParameters() {
    doTest();
  }

  public void ignore_testMethodsWithUnnamedParameters() {
    doTest();
  }

  /**
   * Tests that no ApiParameterInspection errors are generated for constructors with parameters
   * that are of API parameter type.
   */
  public void ignore_testConstructors() {
    doTest();
  }

  /**
   * Tests that {@link ApiParameterInspection.MyQuickFix} applies the
   * {@link GctConstants.APP_ENGINE_ANNOTATION_NAMED} annotation to a {@link PsiParameter}
   * when the {@link PsiParameter} does not already have an @Named annotation.
   */
  public void testQuickFix_parameterWithNoNamedAnnotation() {
    runQuickFixTest("int someParam", "@Named(\"someParam\")int someParam");
  }

  /**
   * Tests that {@link ApiParameterInspection.MyQuickFix} does not add any annotation
   * to a {@link PsiParameter} that already has an @Named annotation.
   */
  public void testQuickFix_parameterWithNamedAnnotation() {
    Project myProject = myFixture.getProject();
    String parameterText = "@" + GctConstants.APP_ENGINE_ANNOTATION_NAMED + "(\"foo\")int someParam";
    runQuickFixTest(parameterText, parameterText);
  }

  private void runQuickFixTest(String parameterText, String expectedString) {
    Project myProject = myFixture.getProject();
    PsiParameter parameter =
      JavaPsiFacade.getInstance(myProject).getElementFactory().createParameterFromText(parameterText, null);
    ApiParameterInspection.MyQuickFix myQuickFix =
      new ApiParameterInspection().new MyQuickFix();
    MockProblemDescriptor problemDescriptor = new MockProblemDescriptor(parameter, "", ProblemHighlightType.ERROR);
    myQuickFix.applyFix(myFixture.getProject(), problemDescriptor);
    Assert.assertEquals(expectedString, parameter.getText());
  }

  private void doTest() {
    LocalInspectionTool localInspectionTool = new ApiParameterInspection();
    String testName = getTestName(true);
    final String testDataPath = getTestDataPath();
    myFixture.setTestDataPath(testDataPath);
    myFixture.testInspection("inspections/apiParameterInspection/" + testName, new LocalInspectionToolWrapper(localInspectionTool));
  }
}
