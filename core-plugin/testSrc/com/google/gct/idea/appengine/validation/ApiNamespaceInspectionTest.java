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

package com.google.gct.idea.appengine.validation;

import com.google.gct.idea.appengine.GctConstants;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.testFramework.MockProblemDescriptor;
import junit.framework.Assert;

/**
 * Tests for {@link ApiNamespaceInspection}
 */
public class ApiNamespaceInspectionTest extends EndpointTestBase  {
  public void ignore_testApiNamespaceAttribute_complete() {
    doTest();
  }

  public void ignore_testApiNamespaceAttribute_withoutPackagePath() {
    doTest();
  }

  public void ignore_testApiNamespaceAttribute_withoutOwnerName() {
    doTest();
  }

  public void ignore_testApiNamespaceAttribute_withOnlyPackagePath() {
    doTest();
  }

  public void ignore_testApiNamespaceAttribute_withOnlyOwnerDomain() {
    doTest();
  }

  public void testQuickFix_allAttributesSpecified() throws Exception {
    String annotationString = "@" + GctConstants.APP_ENGINE_ANNOTATION_API_NAMESPACE
      + "(ownerName = \"myName\", ownerDomain = \"myDomain\", packagePath = \"myPath\")";
    String expectedString = "@" + GctConstants.APP_ENGINE_ANNOTATION_API_NAMESPACE
      + "(ownerName = \"myName\", ownerDomain = \"myDomain\", packagePath = \"myPath\")";
    runQuickFixTest(annotationString, expectedString);
  }

  public void testQuickFix_noAttributesSpecified() throws Exception {
    String annotationString = "@" + GctConstants.APP_ENGINE_ANNOTATION_API_NAMESPACE
                              + "(ownerName = \"\", ownerDomain = \"\", packagePath = \"\")";
    String expectedString = "@" + GctConstants.APP_ENGINE_ANNOTATION_API_NAMESPACE
                            + "(ownerName = \"YourCo\", ownerDomain = \"your-company.com\", packagePath = \"\")";
    runQuickFixTest(annotationString, expectedString);
  }

  public void testQuickFix_nameAndDomainSet(){
    String annotationString = "@" + GctConstants.APP_ENGINE_ANNOTATION_API_NAMESPACE
                              + "(ownerName = \"myName\", ownerDomain = \"myDomain\", packagePath = \"\")";
    String expectedString = "@" + GctConstants.APP_ENGINE_ANNOTATION_API_NAMESPACE
                            + "(ownerName = \"myName\", ownerDomain = \"myDomain\", packagePath = \"\")";
    runQuickFixTest(annotationString, expectedString);
  }

  public void testQuickFix_domainAndPathSet(){
    String annotationString = "@" + GctConstants.APP_ENGINE_ANNOTATION_API_NAMESPACE
                              + "(ownerName = \"\", ownerDomain = \"myDomain\", packagePath = \"myPath\")";
    String expectedString = "@" + GctConstants.APP_ENGINE_ANNOTATION_API_NAMESPACE
                            + "(ownerName = \"YourCo\", ownerDomain = \"myDomain\", packagePath = \"myPath\")";
    runQuickFixTest(annotationString, expectedString);
  }

  public void testQuickFix_nameSet(){
    String annotationString = "@" + GctConstants.APP_ENGINE_ANNOTATION_API_NAMESPACE
                              + "(ownerName = \"myName\", ownerDomain = \"\", packagePath = \"\")";
    String expectedString = "@" + GctConstants.APP_ENGINE_ANNOTATION_API_NAMESPACE
                            + "(ownerName = \"myName\", ownerDomain = \"your-company.com\", packagePath = \"\")";
    runQuickFixTest(annotationString, expectedString);
  }

  public void testQuickFix_pathSet(){
    String annotationString = "@" + GctConstants.APP_ENGINE_ANNOTATION_API_NAMESPACE
                              + "(ownerName = \"\", ownerDomain = \"\", packagePath = \"myPath\")";
    String expectedString = "@" + GctConstants.APP_ENGINE_ANNOTATION_API_NAMESPACE
                            + "(ownerName = \"YourCo\", ownerDomain = \"your-company.com\", packagePath = \"myPath\")";
    runQuickFixTest(annotationString, expectedString);
  }

  private void runQuickFixTest(String annotationString, String expectedString) {
   final Project myProject = myFixture.getProject();
    PsiAnnotation annotation = JavaPsiFacade.getInstance(myProject).getElementFactory()
      .createAnnotationFromText(annotationString, null);
    MockProblemDescriptor problemDescriptor = new MockProblemDescriptor(annotation, "", ProblemHighlightType.ERROR, null);

    ApiNamespaceInspection.MyQuickFix myQuickFix = new ApiNamespaceInspection().new  MyQuickFix();
    myQuickFix.applyFix(myProject, problemDescriptor);
    Assert.assertEquals(expectedString, annotation.getText());
  }

  private void doTest() {
    LocalInspectionTool localInspectionTool = new ApiNamespaceInspection();
    String testName = getTestName(true);
    final String testDataPath = getTestDataPath();
    myFixture.setTestDataPath(testDataPath);
    myFixture.testInspection("inspections/apiNamespaceInspection/" + testName, new LocalInspectionToolWrapper(localInspectionTool));
  }
}
