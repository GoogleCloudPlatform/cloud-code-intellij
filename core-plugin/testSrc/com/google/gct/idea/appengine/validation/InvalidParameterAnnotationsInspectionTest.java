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

import com.google.gct.idea.appengine.GctConstants;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.testFramework.MockProblemDescriptor;
import junit.framework.Assert;

/**
 * Tests for {@link InvalidParameterAnnotationsInspection}
 */
public class InvalidParameterAnnotationsInspectionTest extends EndpointTestBase {
  /**
   * Tests that when a name is not specified in the path attribute of an @ApiMethod,
   * no InvalidParameterAnnotationsInspection error produced.
   */
  public void ignore_testPathAttributeWithNoNameDeclared() {
    doTest();
  }

  /**
   * Tests that when a variable name is specified in the path attribute of an @ApiMethod,
   * and the corresponding method parameter has @DefaultValue, an InvalidParameterAnnotationsInspection
   * error is produced.
   */
  public void ignore_testPathNameWithDefaultValueAnnotation() {
    doTest();
  }

  /**
   * Tests that when a variable name is specified in the path attribute of an @ApiMethod,
   * and the corresponding method parameter has @Nullable, an InvalidParameterAnnotationsInspection
   * error is produced.
   */
  public void ignore_testPathNameWithNullableAnnotation() {
    doTest();
  }

  /**
   * Tests that when a variable name is specified in the path attribute of an @ApiMethod,
   * and the corresponding method parameter does not have @Nullable or @DefaultValue,
   * no InvalidParameterAnnotationsInspection error is produced.
   */
  public void ignore_testPathNameWithNoNullableOrDefaultValue() {
    doTest();
  }

  /**
   * Tests that the Quick fix for a parameter with no annotation returns the same
   * parameter unchanged.
   */
  public void testQuickFix_noAnnotation() {
    Project myProject = myFixture.getProject();
    PsiParameter parameter = JavaPsiFacade.getInstance(myProject).getElementFactory()
      .createParameter("param", PsiType.BOOLEAN);
    runQuickFixTest(parameter, "boolean param");
  }

  /**
   * Test that the Quick fix for a parameter with @Named returns the same parameter unchanged.
   */
  public void testQuickFix_withNamedAnnotation() {
    Project myProject = myFixture.getProject();
    PsiParameter parameter = JavaPsiFacade.getInstance(myProject).getElementFactory()
      .createParameter("param", PsiType.BOOLEAN);
    parameter.getModifierList().addAnnotation(GctConstants.APP_ENGINE_ANNOTATION_NAMED);
    runQuickFixTest(parameter, "@" + GctConstants.APP_ENGINE_ANNOTATION_NAMED + " boolean param");
  }

  /**
   * Tests that the Quick fix for a parameter with the App Engine @Nullable returns the parameter
   * without @Nullable.
   */
  public void testQuickFix_withGaeNullableAnnotation() {
    Project myProject = myFixture.getProject();
    PsiParameter parameter = JavaPsiFacade.getInstance(myProject).getElementFactory()
      .createParameter("param", PsiType.BOOLEAN);
    parameter.getModifierList().addAnnotation(GctConstants.APP_ENGINE_ANNOTATION_NULLABLE);
    runQuickFixTest(parameter, " boolean param");
  }

  /**
   * Tests that the Quick fix for a parameter with the javax @Nullable returns the parameter
   * without @Nullable.
   */
  public void testQuickFix_withJavaxNullableAnnotation() {
    Project myProject = myFixture.getProject();
    PsiParameter parameter = JavaPsiFacade.getInstance(myProject).getElementFactory()
      .createParameter("param", PsiType.BOOLEAN);
    parameter.getModifierList().addAnnotation("javax.annotation.Nullable");
    runQuickFixTest(parameter, " boolean param");
  }

  /**
   * Tests that the Quick fix for a parameter with @DefaultValue returns the parameter
   * without @DefaultValue.
   */
  public void testQuickFix_withDefaultAnnotation() {
    Project myProject = myFixture.getProject();
    PsiParameter parameter = JavaPsiFacade.getInstance(myProject).getElementFactory()
      .createParameter("param", PsiType.BOOLEAN);
    parameter.getModifierList().addAnnotation(GctConstants.APP_ENGINE_ANNOTATION_DEFAULT_VALUE);
    runQuickFixTest(parameter, " boolean param");
  }

  /**
   * Tests that the Quick fix for a parameter with the javax @Nullable and @DefaultValue returns
   * the parameter without @Nullable and @DefaultValue.
   */
  public void testQuickFix_withJavaxNullableAndDefaultAnnotation() {
    Project myProject = myFixture.getProject();
    PsiParameter parameter = JavaPsiFacade.getInstance(myProject).getElementFactory()
      .createParameter("param", PsiType.BOOLEAN);
    parameter.getModifierList().addAnnotation("javax.annotation.Nullable");
    parameter.getModifierList().addAnnotation(GctConstants.APP_ENGINE_ANNOTATION_DEFAULT_VALUE);
    runQuickFixTest(parameter, " boolean param");
  }

  private void runQuickFixTest(PsiParameter parameter, String expectedString) {
    InvalidParameterAnnotationsInspection.MyQuickFix myQuickFix = new InvalidParameterAnnotationsInspection().new MyQuickFix();
    MockProblemDescriptor problemDescriptor = new MockProblemDescriptor(parameter, "", ProblemHighlightType.ERROR, null);
    myQuickFix.applyFix(myFixture.getProject(), problemDescriptor);
    Assert.assertEquals(expectedString, parameter.getText());
  }

  private void doTest() {
    LocalInspectionTool localInspectionTool = new InvalidParameterAnnotationsInspection();
    String testName = getTestName(true);
    myFixture.setTestDataPath(getTestDataPath());
    myFixture.testInspection("inspections/invalidParameterAnnotationsInspection/" + testName, new LocalInspectionToolWrapper(localInspectionTool));
  }
}
