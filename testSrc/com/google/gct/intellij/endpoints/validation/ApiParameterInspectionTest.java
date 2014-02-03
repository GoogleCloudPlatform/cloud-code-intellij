package com.google.gct.intellij.endpoints.validation;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;

/**
 * Test for {link@ApiParameterInspection}.
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

  private void doTest() {
    LocalInspectionTool localInspectionTool = new ApiParameterInspection();
    String testName = getTestName(true);
    final String testDataPath = getTestDataPath();
    myFixture.setTestDataPath(testDataPath);
    myFixture.testInspection("inspections/apiParameterInspection/" + testName, new LocalInspectionToolWrapper(localInspectionTool));
  }
}
