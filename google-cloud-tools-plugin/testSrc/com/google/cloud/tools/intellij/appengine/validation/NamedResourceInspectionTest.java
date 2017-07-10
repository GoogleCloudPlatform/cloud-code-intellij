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

import com.google.cloud.tools.intellij.appengine.GctConstants;
import com.google.cloud.tools.intellij.appengine.validation.NamedResourceInspection.NamedResourceError;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiParameter;
import com.intellij.testFramework.MockProblemDescriptor;

/**
 * Tests for {@link NamedResourceInspection}.
 */
public class NamedResourceInspectionTest extends EndpointTestBase {

  /**
   * Tests that NamedResourceInspection problems are generated for @Named parameters
   * that have duplicate query names as other @Named parameters within the same method.
   */
  public void testMethodsWithNamedResources() {
    doTest();
  }

  /**
   * Tests that NamedResourceInspection problems are generated for @Named parameters
   * that do not have specified query names.
   */
  public void testMethodsWithUnnamedResources() {
    doTest();
  }

  public void testMethodsWithUnnamedResourcesNoArg() {
    doTest();
  }

  public void testMethodsWithUnnamedResourcesEmptyString() {
    doTest();
  }

  /**
   * Tests that the NamedResourceInspection's quick fix flagged with
   * {@link NamedResourceError.DUPLICATE_PARAMETER} for an @Named annotation updates
   * the query name by adding "_1" as a suffix.
   */
  public void testQuickFix_duplicateParameter() {
    Project myProject = myFixture.getProject();
    String annotationString = "@" + GctConstants.APP_ENGINE_ANNOTATION_NAMED + "(\"someName\")";
    PsiAnnotation annotation =
      JavaPsiFacade.getInstance(myProject).getElementFactory().createAnnotationFromText(annotationString, null);
    NamedResourceInspection.DuplicateNameQuickFix myQuickFix =
      new NamedResourceInspection().new DuplicateNameQuickFix();
    MockProblemDescriptor problemDescriptor = new MockProblemDescriptor(annotation, "", ProblemHighlightType.ERROR);

    myQuickFix.applyFix(myProject, problemDescriptor);
    assertEquals("@" + GctConstants.APP_ENGINE_ANNOTATION_NAMED + "(\"someName_1\")", annotation.getText());
  }

  /**
   * Tests that the NamedResourceInspection's quick fix flagged with
   * {@link NamedResourceError.MISSING_NAME} for an @Named annotation
   * with no parent updates the query name to "myName".
   */
  public void testQuickFix_noQueryNameSpecifiedWithoutParameter() {
    Project myProject = myFixture.getProject();
    String annotationString = "@" + GctConstants.APP_ENGINE_ANNOTATION_NAMED + "()";
    PsiAnnotation annotation =
      JavaPsiFacade.getInstance(myProject).getElementFactory().createAnnotationFromText(annotationString, null);
    NamedResourceInspection.MissingNameQuickFix myQuickFix =
      new NamedResourceInspection().new MissingNameQuickFix();
    MockProblemDescriptor problemDescriptor = new MockProblemDescriptor(annotation, "", ProblemHighlightType.ERROR);

    myQuickFix.applyFix(myProject, problemDescriptor);
    assertEquals("@" + GctConstants.APP_ENGINE_ANNOTATION_NAMED + "(\"myName\")", annotation.getText());
  }

  /**
   * Tests that the NamedResourceInspection's quick fix flagged with
   * {@link NamedResourceError.MISSING_NAME} for an @Named annotation
   * with a {@link PsiParameter} parent updates the query name to to the name of the
   * {@link PsiParameter}.
   */
  public void testQuickFix_noQueryNameSpecifiedWithParameter() {
    Project myProject = myFixture.getProject();
    PsiParameter parameter = JavaPsiFacade.getInstance(myProject).getElementFactory()
      .createParameterFromText("@javax.inject.Named() String foobar", null);
    PsiAnnotation[] annotationsList = parameter.getModifierList().getAnnotations();
    assert (annotationsList.length == 1);
    NamedResourceInspection.MissingNameQuickFix myQuickFix =
      new NamedResourceInspection().new MissingNameQuickFix();
    MockProblemDescriptor problemDescriptor = new MockProblemDescriptor(annotationsList[0], "", ProblemHighlightType.ERROR);

    myQuickFix.applyFix(myProject, problemDescriptor);
    assertEquals("@javax.inject.Named(\"foobar\")", annotationsList[0].getText());
  }

  private void doTest() {
    LocalInspectionTool localInspectionTool = new NamedResourceInspection();
    String testName = getTestName(true);
    final String testDataPath = getTestDataPath();
    myFixture.setTestDataPath(testDataPath);
    myFixture.testInspection("inspections/namedResourceInspection/" + testName, new LocalInspectionToolWrapper(localInspectionTool));
  }
}