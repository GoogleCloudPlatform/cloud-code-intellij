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
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.testFramework.MockProblemDescriptor;

/**
 * Tests for {@link FullJavaNameInspection}.
 */
public class FullJavaNameInspectionTest extends EndpointTestBase {

  public void testClassWithUniqueMethodNames() {
    doTest();
  }

  public void testClassWithDuplicateMethodNames() {
    doTest();
  }

  public void testQuickFix() {
    PsiMethod someFunction =
      JavaPsiFacade.getInstance(myFixture.getProject()).getElementFactory().createMethod("someFunction", PsiType.VOID);
    MockProblemDescriptor problemDescriptor = new MockProblemDescriptor(someFunction, "", ProblemHighlightType.ERROR);

    FullJavaNameInspection.MyQuickFix myQuickFix = new FullJavaNameInspection().new MyQuickFix();
    myQuickFix.applyFix(myFixture.getProject(), problemDescriptor);
    assertEquals("someFunction_1", someFunction.getName());

    myQuickFix.applyFix(myFixture.getProject(), problemDescriptor);
    assertEquals("someFunction_1_1", someFunction.getName());
  }

  private void doTest() {
    LocalInspectionTool localInspectionTool = new FullJavaNameInspection();
    String testName = getTestName(true);
    myFixture.setTestDataPath(getTestDataPath());
    myFixture.testInspection("inspections/fullJavaNameInspection/" + testName, new LocalInspectionToolWrapper(localInspectionTool));
  }
}
