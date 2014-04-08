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

  public void testMethodsWithNamedParameters() {
    doTest();
  }

  public void testMethodsWithUnnamedParameters() {
    doTest();
  }

  /**
   * Tests that no ApiParameterInspection errors are generated for constructors with parameters
   * that are of API parameter type.
   */
  public void testConstructors() {
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
    MockProblemDescriptor problemDescriptor = new MockProblemDescriptor(parameter, "", ProblemHighlightType.ERROR, null);
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
