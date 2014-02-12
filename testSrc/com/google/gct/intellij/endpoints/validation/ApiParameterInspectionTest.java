package com.google.gct.intellij.endpoints.validation;

import com.google.gct.intellij.endpoints.GctConstants;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.codeInspection.ex.ProblemDescriptorImpl;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import junit.framework.Assert;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    Project myProject = myFixture.getProject();
    PsiParameter parameter =
      JavaPsiFacade.getInstance(myProject).getElementFactory().createParameter("someParam", PsiType.INT);
    ApiParameterInspection.MyQuickFix myQuickFix =
      new ApiParameterInspection().new MyQuickFix();
    ProblemDescriptorImpl problemDescriptorMock = mock(ProblemDescriptorImpl.class);
    when(problemDescriptorMock.getPsiElement()).thenReturn(parameter);

    myQuickFix.applyFix(myProject, problemDescriptorMock);
    Assert.assertEquals("@Named(\"someParam\")int someParam", parameter.getText());
  }

  /**
   * Tests that {@link ApiParameterInspection.MyQuickFix} does not add any annotation
   * to a {@link PsiParameter} that already has an @Named annotation.
   */
  public void testQuickFix_parameterWithNamedAnnotation() {
    Project myProject = myFixture.getProject();
    String parameterText = "@" + GctConstants.APP_ENGINE_ANNOTATION_NAMED + "(\"foo\")int someParam";
    PsiParameter parameter =
      JavaPsiFacade.getInstance(myProject).getElementFactory().createParameterFromText(parameterText, null);
    ApiParameterInspection.MyQuickFix myQuickFix =
      new ApiParameterInspection().new MyQuickFix();
    ProblemDescriptorImpl problemDescriptorMock = mock(ProblemDescriptorImpl.class);
    when(problemDescriptorMock.getPsiElement()).thenReturn(parameter);

    myQuickFix.applyFix(myProject, problemDescriptorMock);
    Assert.assertEquals(parameterText, parameter.getText());
  }

  private void doTest() {
    LocalInspectionTool localInspectionTool = new ApiParameterInspection();
    String testName = getTestName(true);
    final String testDataPath = getTestDataPath();
    myFixture.setTestDataPath(testDataPath);
    myFixture.testInspection("inspections/apiParameterInspection/" + testName, new LocalInspectionToolWrapper(localInspectionTool));
  }
}
