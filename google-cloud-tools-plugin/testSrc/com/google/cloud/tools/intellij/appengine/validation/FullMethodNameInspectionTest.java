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

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.testFramework.MockProblemDescriptor;

import junit.framework.Assert;

/**
 * Tests for {@link FullMethodNameInspection}.
 */
public class FullMethodNameInspectionTest extends EndpointTestBase {

  /**
   * Tests that the FullMethodNameInspection does not flag methods with
   * unique API method names.
   */
  public void testClassWithUniqueFullMethodNames() {
    doTest();
  }

  /**
   * Tests that the FullMethodNameInspection flag methods with
   * duplicate API method names.
   */
  public void testClassWithDuplicateFullMethodNames() {
    doTest();
  }

  /**
   * Tests that the FullMethodNameInspection's quick fix updates
   * the name attribute of {@link GctConstants.APP_ENGINE_ANNOTATION_API_METHOD}
   * by adding "_1" as a suffix.
   */
  public void testQuickFix_ApiMethodAnnotation() {
    Project myProject = myFixture.getProject();
    String annotationString = "@" + GctConstants.APP_ENGINE_ANNOTATION_API_METHOD + "(name = \"someName\")";
    PsiAnnotation annotation =
      JavaPsiFacade.getInstance(myProject).getElementFactory().createAnnotationFromText(annotationString, null);
    FullMethodNameInspection.MyQuickFix myQuickFix = new FullMethodNameInspection().new MyQuickFix();
    MockProblemDescriptor problemDescriptor = new MockProblemDescriptor(annotation, "", ProblemHighlightType.ERROR);

    myQuickFix.applyFix(myProject, problemDescriptor);
    Assert.assertEquals("@" + GctConstants.APP_ENGINE_ANNOTATION_API_METHOD + "(name = \"someName_1\")", annotation.getText());
  }

  /**
   * Tests that the FullMethodNameInspection's quick fix does not update
   * an annotation that is not {@link GctConstants.APP_ENGINE_ANNOTATION_API_METHOD}
   */
  public void testQuickFix_NonApiMethodAnnotation() {
    Project myProject = myFixture.getProject();
    String annotationString = "@" + GctConstants.APP_ENGINE_ANNOTATION_NAMED + "(name = \"someName\")";
    PsiAnnotation annotation =
      JavaPsiFacade.getInstance(myProject).getElementFactory().createAnnotationFromText(annotationString, null);
    FullMethodNameInspection.MyQuickFix myQuickFix = new FullMethodNameInspection().new MyQuickFix();
    MockProblemDescriptor problemDescriptor = new MockProblemDescriptor(annotation, "", ProblemHighlightType.ERROR);

    myQuickFix.applyFix(myProject, problemDescriptor);
    Assert.assertEquals(annotationString, annotation.getText());
  }

  private void doTest() {
    LocalInspectionTool localInspectionTool = new FullMethodNameInspection();
    String testName = getTestName(true);
    myFixture.setTestDataPath(getTestDataPath());
    myFixture.testInspection("inspections/fullMethodNameInspection/" + testName, new LocalInspectionToolWrapper(localInspectionTool));
  }
}
